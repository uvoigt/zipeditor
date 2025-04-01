/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.part.FileEditorInput;

import zipeditor.actions.IPostOpenProcessor;
import zipeditor.model.Node;
import zipeditor.operations.ExtractOperation;
import zipeditor.operations.OpenFileOperation;

public class Utils {
	private static class ExtractAndOpenJob extends Job {
		private Node[] fNodes;

		public ExtractAndOpenJob(Node[] nodes) {
			super(Messages.getString("Utils.0")); //$NON-NLS-1$
			fNodes = nodes;
		}
		
		protected IStatus run(IProgressMonitor monitor) {
			monitor.beginTask(Messages.getString("Utils.1"), fNodes.length); //$NON-NLS-1$
			try {
				internalOpenFilesFromNodes(fNodes, monitor);
			} finally {
				monitor.done();
			}
			return Status.OK_STATUS;
		}
	};

	public static int computeTotalNumber(Node[] nodes, IProgressMonitor monitor) {
		int result = 0;
		for (int i = 0; i < nodes.length; i++) {
			if (monitor.isCanceled())
				break;
			if (nodes[i].isFolder())
				result += computeTotalNumber(nodes[i].getChildren(), monitor);
			else
				result++;
		}
		return result;
	}

	public static int computeTotalNumber(File[] files, IProgressMonitor monitor) {
		if (files == null)
			return 0;
		int result = 0;
		for (int i = 0; i < files.length; i++) {
			if (monitor.isCanceled())
				break;
			if (files[i].isDirectory())
				result += computeTotalNumber(files[i].listFiles(), monitor);
			else
				result++;
		}
		return result;
	}

	public static boolean isUIThread() {
		return Display.getCurrent() != null;
	}
	
	public static IFileStore getFileStore(File file) {
		IFileStore fileStore = EFS.getLocalFileSystem().getStore(new Path(file.getParentFile().getAbsolutePath()));
		return fileStore.getChild(file.getName());
	}
	
	public static void openFilesFromNodes(Node[] nodes) {
		if (nodes == null || nodes.length == 0)
			return;
		ExtractAndOpenJob job = new ExtractAndOpenJob(nodes);
		job.schedule();
	}

	private static void internalOpenFilesFromNodes(Node[] nodes, IProgressMonitor monitor) {
		File tmpDir = nodes[0].getModel().getTempDir();
		ExtractOperation extractOperation = new ExtractOperation();
		OpenFileOperation openFileOperation = new OpenFileOperation();
		for (int i = 0; i < nodes.length; i++) {
			Node node = nodes[i];
			monitor.subTask(node.getName());
			File file = extractOperation.extract(node, tmpDir, true, monitor);
			openFileOperation.execute(file, node);
			ZipEditorPlugin.getDefault().addFileMonitor(file, node);
			monitor.worked(1);
		}
	}

	public static void handlePostOpen(IEditorPart editor, Node node) {
		Object property = node.getProperty("postOpen"); //$NON-NLS-1$
		if (editor != null && property instanceof IPostOpenProcessor)
			((IPostOpenProcessor) property).postOpen(editor);
	}
	
	public static IEditorInput createEditorInput(IFileStore fileStore) {
		IFile workspaceFile = getWorkspaceFile(fileStore);
		if (workspaceFile != null)
			return new FileEditorInput(workspaceFile);
		try {
			return new FileStoreEditorInput(fileStore);
		} catch (Throwable ignore) { // not available
			return new LocalFileEditorInput(fileStore);
		}
	}
	
	public static String getEditorId(IFileStore file) {
		IWorkbench workbench = PlatformUI.getWorkbench();
		IEditorRegistry editorRegistry = workbench.getEditorRegistry();
		IEditorDescriptor descriptor = editorRegistry.getDefaultEditor(file.getName(), getContentType(file));

		// check the OS for in-place editor (OLE on Win32)
		if (descriptor == null && editorRegistry.isSystemInPlaceEditorAvailable(file.getName()))
			descriptor= editorRegistry.findEditor(IEditorRegistry.SYSTEM_INPLACE_EDITOR_ID);
		
		// check the OS for external editor
		if (descriptor == null && editorRegistry.isSystemExternalEditorAvailable(file.getName()))
			descriptor = editorRegistry.findEditor(IEditorRegistry.SYSTEM_EXTERNAL_EDITOR_ID);
		
		if (descriptor != null)
			return descriptor.getId();
		
		return EditorsUI.DEFAULT_TEXT_EDITOR_ID;
	}

	public static IContentType getContentType(IFileStore fileStore) {
		if (fileStore == null)
			return null;

		InputStream stream= null;
		try {
			stream = fileStore.openInputStream(EFS.NONE, null);
			return Platform.getContentTypeManager().findContentTypeFor(stream, fileStore.getName());
		} catch (IOException e) {
			ZipEditorPlugin.log(e);
			return null;
		} catch (CoreException e) {
			// Do not log FileNotFoundException (no access)
			if (!(e.getStatus().getException() instanceof FileNotFoundException))
				ZipEditorPlugin.log(e);
			
			return null;
		} finally {
			try {
				if (stream != null)
					stream.close();
			} catch (IOException e) {
				ZipEditorPlugin.log(e);
			}
		}
	}

	private static IFile getWorkspaceFile(IFileStore fileStore) {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		final IFile[] files = filterNonExistentFiles(workspace.getRoot().
				findFilesForLocation(new Path(fileStore.toURI().getPath())));
		if (files == null || files.length == 0)
			return null;
		if (files.length == 1)
			return files[0];
		if (isUIThread()) {
			return selectWorkspaceFile(files);
		} else {
			final IFile[] result = { null };
			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					result[0] = selectWorkspaceFile(files);
				}
			});
			return result[0];
		}
	}
	
	private static IFile selectWorkspaceFile(IFile[] files) {
		ElementListSelectionDialog dialog = new ElementListSelectionDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), new LabelProvider() {
			public String getText(Object element) {
				if (element instanceof IFile) {
					IPath path =  ((IFile) element).getFullPath();
					return path != null ? path.toString() : ""; //$NON-NLS-1$
				}
				return super.getText(element);
			}
		});
		dialog.setElements(files);
		dialog.setTitle(Messages.getString("OpenFileOperation.0")); //$NON-NLS-1$
		dialog.setMessage(Messages.getString("OpenFileOperation.1")); //$NON-NLS-1$
		if (dialog.open() == Window.OK)
			return (IFile) dialog.getFirstResult();
		return null;
	}

	private static IFile[] filterNonExistentFiles(IFile[] files){
		if (files == null)
			return null;

		int length= files.length;
		ArrayList existentFiles = new ArrayList(length);
		for (int i = 0; i < length; i++) {
			if (files[i].exists())
				existentFiles.add(files[i]);
		}
		return (IFile[]) existentFiles.toArray(new IFile[existentFiles.size()]);
	}

	public static void readAndWrite(InputStream in, OutputStream out, boolean closeOut) throws IOException {
		try {
			if (in != null) {
				byte[] buf = new byte[8000];
				for (int count = 0; (count = in.read(buf)) != -1; ) {
					out.write(buf, 0, count);
				}
			}
		} finally {
			if (in != null)
				in.close();
			if (closeOut)
				out.close();
		}
	}
	
	public static boolean allNodesAreFileNodes(IStructuredSelection selection) {
		for (Iterator it = selection.iterator(); it.hasNext();) {
			Object object = it.next();
			if (!(object instanceof Node))
				return false;
			Node node = (Node) object;
			if (node.isFolder())
				return false;
		}
		return true;
	}
	
	public static Node[] getSelectedNodes(ISelection selection) {
		if (!(selection instanceof IStructuredSelection))
			return new Node[0];
		Object[] objects = ((IStructuredSelection) selection).toArray();
		Node[] nodes = new Node[objects.length];
		System.arraycopy(objects, 0, nodes, 0, objects.length);
		return nodes;
	}

	public static File[] getJavaPackageFragmentRoots(Object[] objects) {
		// no actual way to extract the jar information legally
		if (objects == null)
			return new File[0];
		List files = new ArrayList();
		for (int i = 0; i < objects.length; i++) {
			Object object = objects[i];
			File file = getJavaPackageFragmentRoot(object);
			if (file != null)
				files.add(file);
		}
		return (File[]) files.toArray(new File[files.size()]);
	}

	public static File getJavaPackageFragmentRoot(Object object) {
		IPath path = null;
		try {
			path = (IPath) object.getClass().getMethod("internalPath", null).invoke(object, null); //$NON-NLS-1$
		} catch (Throwable t) {
			// ignore this
		}
		if (path != null) {
			IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(path);
			if (resource != null && resource instanceof IFile)
				return resource.getLocation().toFile();
			File file = path.toFile();
			if (file.exists() && file.isFile())
				return file;
		}
		return null;
	}

	private static boolean hasAircompressor;
	private static boolean checked;
	public static boolean isAircompressorAvailable() {
		if (checked) 
			return hasAircompressor;

		checked = true;
		try {
			Class.forName("io.airlift.compress.zstd.ZstdInputStream"); //$NON-NLS-1$
			hasAircompressor = true;
		} catch (ClassNotFoundException e) {
			hasAircompressor = true;
		}
		return hasAircompressor;
	}

	private Utils() {
	}
}
