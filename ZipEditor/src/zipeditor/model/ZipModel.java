/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.model;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import zipeditor.Messages;
import zipeditor.Utils;
import zipeditor.ZipEditorPlugin;
import zipeditor.model.IModelListener.ModelChangeEvent;

public class ZipModel {
	public final static int ZIP = 1;
	public final static int TAR = 2;
	public final static int GZ = 3;
	public final static int TARGZ = 4;

	public final static int INIT_STARTED = 0x01;
	public final static int INIT_FINISHED = 0x02;
	public final static int INITIALIZING = 0x04;
	public final static int DIRTY = 0x08;

	public static int typeFromName(String string) {
		if (string != null) {
			String lowerCase = string.toLowerCase();
			if (lowerCase.endsWith(".gz")) //$NON-NLS-1$
				return GZ;
			if (lowerCase.endsWith(".tgz") || lowerCase.endsWith(".tar.gz")) //$NON-NLS-1$ //$NON-NLS-2$
				return TARGZ;
			if (lowerCase.endsWith(".tar")) //$NON-NLS-1$
				return TAR;
		}
		return ZIP;
	}

	public static int detectType(InputStream contents) {
		if (!contents.markSupported())
			contents = new BufferedInputStream(contents);
		try {
			contents.mark(TarBuffer.DEFAULT_BLKSIZE);
			ZipInputStream zip = new ZipInputStream(contents);
			if (zip.getNextEntry() != null) {
				contents.reset();
				return ZIP;
			}
			contents.reset();
			try {
				GZIPInputStream gzip = new GZIPInputStream(contents);
				TarInputStream tar = new TarInputStream(gzip);
				TarEntry tarEntry = tar.getNextEntry();
				if (tarEntry != null && tar.entrySize >= 0) {
					contents.reset();
					return TARGZ;
				} else {
					contents.reset();
					return GZ;
				}
			} catch (IOException e) {
				if ("Not in GZIP format".equals(e.getMessage())) { //$NON-NLS-1$
					contents.reset();
					return TAR;
				}
				contents.reset();
				return -1;
			}
		} catch (Exception ignore) {
			return -1;
		}
	}

	private Node root;
	private File zipPath;
	private int state;
	private int type;
	private File tempDir;
	private List tempFilesToDelete;
	private ListenerList listenerList = new ListenerList();
	private Job initJob;
	
	public ZipModel(File path, final InputStream inputStream) {
		zipPath = path;
		state |= INITIALIZING;
		if (path.length() >= 10000000) {
			initJob = new Job(Messages.getString("ZipModel.0")) { //$NON-NLS-1$
				protected IStatus run(IProgressMonitor monitor) {
					monitor.beginTask(Messages.getString("ZipModel.1"), IProgressMonitor.UNKNOWN); //$NON-NLS-1$
					initialize(inputStream, monitor);
					monitor.done();
					initJob = null;
					return Status.OK_STATUS;
				}
			};
			initJob.schedule();
		} else {
			initialize(inputStream, new NullProgressMonitor());
		}
	}
	
	private void initialize(InputStream inputStream, IProgressMonitor monitor) {
		long time = System.currentTimeMillis();
		InputStream zipStream = inputStream;
		try {
			zipStream = detectStream(inputStream);
		} catch (Exception ignore) {
		}
		ZipEntry zipEntry = null;
		TarEntry tarEntry = null;
		root = new ZipNode(this, "", true); //$NON-NLS-1$
		state |= INIT_STARTED;
		boolean isGzipStream = zipStream instanceof GZIPInputStream;
		while (true) {
			if (monitor.isCanceled()) {
				state |= DIRTY;
				break;
			}
			try {
				if (zipStream instanceof ZipInputStream)
					zipEntry = ((ZipInputStream) zipStream).getNextEntry();
				else if (zipStream instanceof TarInputStream)
					tarEntry = ((TarInputStream) zipStream).getNextEntry();
			} catch (Exception e) {
				ZipEditorPlugin.log(e);
				break;
			}
			if ((!isGzipStream && zipEntry == null && tarEntry == null)
					|| (isGzipStream && root.children != null)) {
				state &= -1 ^ DIRTY;
				break;
			}
			String entryName = zipEntry != null ? zipEntry.getName()
					: tarEntry != null ? tarEntry.getName() : zipPath.getName()
							.endsWith(".gz") ? zipPath.getName().substring(0, //$NON-NLS-1$
							zipPath.getName().length() - 3) : zipPath.getName();
			String[] names = splitName(entryName);
			Node node = null;
			int n = names.length - 1;
			for (int i = 0; i < n; i++) {
				String pathSeg = names[i];
				Node parent = node != null ? node : root;
				node = parent.getChildByName(pathSeg, false);
				if (node == null)
					parent.add(node = new ZipNode(this, pathSeg, true));
			}
			boolean isFolder = entryName.endsWith("/") || entryName.endsWith("\\") || //$NON-NLS-1$ //$NON-NLS-2$
					(zipEntry != null && zipEntry.isDirectory() || tarEntry != null && tarEntry.isDirectory());
			if (node == null)
				node = root;
			if (node.getChildByName(names[n], false) == null) {
				Node newChild = zipEntry != null ? new ZipNode(this, zipEntry, names[n], isFolder) :
						tarEntry != null ? (Node) new TarNode(this, tarEntry, names[n], isFolder)
								: new GzipNode(this, names[n], isFolder);
				node.add(newChild);
				if (zipStream instanceof ZipInputStream) {
					try {
						((ZipInputStream) zipStream).closeEntry();
					} catch (Exception e) {
						ZipEditorPlugin.log(e);
					}
				}
				newChild.setSize(zipEntry != null ? zipEntry.getSize() : tarEntry != null ? tarEntry.getSize() : 0);
			}
			state &= -1 ^ INIT_STARTED;
		}
		if (zipStream != null) {
			try {
				zipStream.close();
			} catch (IOException e) {
				ZipEditorPlugin.log(e);
			}
		}
		state &= -1 ^ INITIALIZING;
		state |= INIT_FINISHED;
		notifyListeners();
		state &= -1 ^ INIT_FINISHED;
		if (ZipEditorPlugin.DEBUG)
			System.out.println(zipPath + " initialized in " + (System.currentTimeMillis() - time) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	private InputStream detectStream(InputStream contents) throws IOException {
		BufferedInputStream in = new BufferedInputStream(contents);
		switch (type = detectType(in)) {
		default:
			return in;
		case ZIP:
			return new ZipInputStream(in);
		case TAR:
			return new TarInputStream(in);
		case GZ:
			return new GZIPInputStream(in);
		case TARGZ:
			return new TarInputStream(new GZIPInputStream(in));
		}
	}

	public InputStream save(int type, IProgressMonitor monitor) throws IOException {
		File tmpFile = new File(root.getModel().getTempDir(), Integer.toString((int) System.currentTimeMillis()));
		OutputStream out = new FileOutputStream(tmpFile);
		switch (type) {
		case GZ:
			out = new GZIPOutputStream(out);
			break;
		case TAR:
			out = new TarOutputStream(out);
			break;
		case TARGZ:
			out = new TarOutputStream(new GZIPOutputStream(out));
			break;
		case ZIP:
			out = new ZipOutputStream(out);
			break;
		}
		if (out instanceof TarOutputStream)
            ((TarOutputStream) out).setLongFileMode(TarOutputStream.LONGFILE_GNU);
		try {
			saveNodes(out, root, type, monitor);
			out.close();
		} catch (Exception e) {
			ZipEditorPlugin.log(e);
		}
		return new FileInputStream(tmpFile);
	}

	private void saveNodes(OutputStream out, Node node, int type, IProgressMonitor monitor) throws IOException {
		if (out == null)
			return;
		Node[] children = node.getChildren();
		for (int i = 0; i < children.length; i++) {
			if (monitor.isCanceled())
				break;
			Node child = children[i];
			if (child.isFolder()) {
				saveNodes(out, child, type, monitor);
				continue;
			}
			ZipEntry zipEntry = type == ZIP ? new ZipEntry(child.getPath() + child.getName()) : null;
			TarEntry tarEntry = type == TAR || type == TARGZ ? new TarEntry(child.getPath() + child.getName()): null;
			if (zipEntry != null) {
				zipEntry.setTime(child.getTime());
				if (child instanceof ZipNode)
					zipEntry.setComment(((ZipNode) child).getComment());
			} else if (tarEntry != null) {
				tarEntry.setModTime(child.getTime());
				tarEntry.setSize(child.getSize());
				if (child instanceof TarNode) {
					TarNode tarNode = (TarNode) child;
					tarEntry.setGroupId(tarNode.getGroupId());
					tarEntry.setGroupName(tarNode.getGroupName());
					tarEntry.setUserId(tarNode.getUserId());
					tarEntry.setUserName(tarNode.getUserName());
					tarEntry.setGroupId(tarNode.getGroupId());
					tarEntry.setMode(tarNode.getMode());
				} else {
					tarEntry.setMode(TarEntry.DEFAULT_FILE_MODE);
				}
			}
			
			if (out instanceof ZipOutputStream)
				((ZipOutputStream) out).putNextEntry(zipEntry);
			else if (out instanceof TarOutputStream)
				((TarOutputStream) out).putNextEntry(tarEntry);
			Utils.readAndWrite(child.getContent(), out, false);
			if (tarEntry != null)
				((TarOutputStream) out).closeEntry();
			monitor.worked(1);
		}
	}

	private String[] splitName(String name) {
		List list = new ArrayList();
		while (name != null && name.length() > 0) {
			int index = name.indexOf('/');
			if (index == -1)
				index = name.indexOf('\\');
			if (index != -1) {
				if (index > 0)
					list.add(name.substring(0, index));
				name = name.substring(index + 1);
			} else {
				list.add(name);
				name = new String();
			}
		}
		return (String[]) list.toArray(new String[list.size()]);
	}
	
	public void addModelListener(IModelListener listener) {
		listenerList.add(listener);
	}
	
	public void removeModelListener(IModelListener listener) {
		listenerList.remove(listener);
	}
	
	protected void notifyListeners() {
//System.out.println("ZipModel.notifyListeners(): state=" + state);
		Object[] listeners = listenerList.getListeners();
		for (int i = 0; i < listeners.length; i++) {
			((IModelListener) listeners[i]).modelChanged(new ModelChangeEvent(this));
		}
	}

	public void dispose() {
		if (initJob != null)
			initJob.cancel();
		if (tempFilesToDelete != null) {
			for (int i = 0, n = tempFilesToDelete.size(); i < n; i++) {
				deleteFile(((File) tempFilesToDelete.get(i)));
			}
			tempFilesToDelete.clear();
			tempFilesToDelete = null;
		}
		tempDir = null;
	}

	public void deleteFileOnDispose(File file) {
		if (file == null)
			return;
		if (tempFilesToDelete == null)
			tempFilesToDelete = new ArrayList();
		tempFilesToDelete.add(file);
	}
	
	private void deleteFile(File file) {
		if (file.isDirectory()) {
			File[] files = file.listFiles();
			if (files != null) {
				for (int i = 0; i < files.length; i++) {
					deleteFile(files[i]);
				}
			}
		}
		if (!file.delete())
			System.out.println("Couldn't delete " + file); //$NON-NLS-1$
	}

	public File getTempDir() {
		if (tempDir == null) {
			File sysTmpDir = new File(System.getProperty("java.io.tmpdir")); //$NON-NLS-1$
			tempDir = new File(sysTmpDir, "zip" + (int) System.currentTimeMillis()); //$NON-NLS-1$
			tempDir.mkdirs();
			deleteFileOnDispose(tempDir);
		}
		return tempDir;
	}

	public Node getRoot() {
		return root;
	}

	public boolean isInitializing() {
		return (state & INITIALIZING) > 0;
	}

	public boolean isInitStarted() {
		return (state & INIT_STARTED) > 0;
	}

	public boolean isInitFinished() {
		return (state & INIT_FINISHED) > 0;
	}

	public boolean isDirty() {
		return (state & DIRTY) > 0;
	}
	
	public void setDirty(boolean dirty) {
		if (dirty) {
			if (!isInitializing())
				state |= DIRTY;
		} else {
			state &= -1 ^ DIRTY;
		}
	}

	public ZipFile getZipFile() throws IOException {
		return new ZipFile(zipPath);
	}
	
	public TarInputStream getTarFile() throws IOException {
		switch (type) {
		default:
		case TAR:
			return new TarInputStream(new FileInputStream(zipPath));
		case TARGZ:
			return new TarInputStream(new GZIPInputStream(new FileInputStream(zipPath)));
		}
	}

	public File getZipPath() {
		return zipPath;
	}

}
