package zipeditor.model;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import zipeditor.Messages;
import zipeditor.Utils;
import zipeditor.ZipEditorPlugin;

public class ZipModelSpace {
	private final Map fModels = new HashMap();
	private IResourceChangeListener fResourceListener;

	public ZipModelSpace() {
		fResourceListener = new IResourceChangeListener() {
			public void resourceChanged(IResourceChangeEvent event) {
				// TODO klappt so nicht: https://eclipse.org/articles/Article-Resource-deltas/resource-deltas.html
				if (event.getType() == IResourceChangeEvent.PRE_DELETE) {
					File file = event.getResource().getLocation().toFile();
					ZipModel model = (ZipModel) fModels.get(file);
					if (model != null) {
						model.dispose();
						fModels.remove(file);
					}
				}
			}
		};
		ResourcesPlugin.getWorkspace().addResourceChangeListener(fResourceListener);
	}

	public ZipModel getModel(File file) {
		return (ZipModel) fModels.get(file);
	}

	public void addModel(File file, ZipModel model) {
		if (fModels.containsKey(file))
			throw new IllegalStateException();
		fModels.put(file, model);
	}

	public void disposeModels() {
		for (Iterator it = new ArrayList(fModels.values()).iterator(); it.hasNext(); ) {
			ZipModel m = (ZipModel) it.next();
			m.dispose();
			fModels.remove(m.getZipPath());
		}
	}

	public void disposeModel(ZipModel model) {
		// prevent concurrent modification
		for (Iterator it = new ArrayList(fModels.values()).iterator(); it.hasNext(); ) {
			ZipModel m = (ZipModel) it.next();
			if (m == model) {
				// first remove, why?
				m.dispose();
				fModels.remove(m.getZipPath());
			}
		}
	}

	public void saveModel(final ZipModel model, IPath toLocation, IProgressMonitor monitor) {
		Node root = model.getRoot();
		monitor.beginTask(Messages.getString("ZipEditor.3"), 100); //$NON-NLS-1$
		monitor.worked(1);
		int totalWork = Utils.computeTotalNumber(root.getChildren(), monitor);
		SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 99);
		monitor.setTaskName(Messages.getString("ZipEditor.2")); //$NON-NLS-1$
		monitor.subTask(toLocation.lastSegment());
		subMonitor.beginTask(Messages.getString("ZipEditor.2") + toLocation, totalWork); //$NON-NLS-1$
		InputStream in = null;
		try {
			in = root.getModel().save(ZipModel.typeFromName(toLocation.lastSegment()), subMonitor);
			IFile file = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(toLocation);
			if (file != null) {
				internalSaveWorkspaceFile(file, in, monitor);
			} else {
				internalSaveLocalFile(toLocation.toFile(), in);
			}
			model.setDirty(false);
		} catch (final Exception e) {
			if (Utils.isUIThread()) {
				doShowErrorDialog(model, e);
			} else {
	        	getShell().getDisplay().syncExec(new Runnable() {
	        		public void run() {
	    				doShowErrorDialog(model, e);
	        		}
	        	});
			}
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException ignore) {
				}
			}
			subMonitor.done();
			monitor.done();
		}
	}

	private Shell getShell() {
		return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
	}

	private void doShowErrorDialog(ZipModel model, Throwable e) {
		ZipEditorPlugin.showErrorDialog(getShell(),
				Messages.getFormattedString("ZipEditor.12", model.getZipPath().getName()), //$NON-NLS-1$
				e);
	}

	private void internalSaveWorkspaceFile(IFile file, InputStream in, IProgressMonitor monitor) throws Exception {
		if (file.exists())
			file.setContents(in, true, true, monitor);
		else
			file.create(in, true, monitor);
	}

	private void internalSaveLocalFile(File file, InputStream in) throws Exception {
		Utils.readAndWrite(in, new FileOutputStream(file), true);
	}

	public void close() {
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(fResourceListener);
	}
}
