/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.actions;

import java.io.File;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import zipeditor.ZipEditor;
import zipeditor.ZipEditorPlugin;
import zipeditor.model.ZipNode;
import zipeditor.operations.ExtractOperation;

public class ExtractAction extends DialogAction {
	private class RefreshJob extends Job {
		public RefreshJob() {
			super(ActionMessages.getString("ExtractAction.3")); //$NON-NLS-1$
		}

		protected IStatus run(IProgressMonitor monitor) {
			finished();
			return Status.OK_STATUS;
		}

	};

	private String fSelectedFolder;

	public ExtractAction(ZipEditor editor) {
		super(ActionMessages.getString("ExtractAction.0"), editor); //$NON-NLS-1$
		setToolTipText(ActionMessages.getString("ExtractAction.1")); //$NON-NLS-1$
		setImageDescriptor(ZipEditorPlugin.getImageDescriptor("icons/extract.gif")); //$NON-NLS-1$
	}

	public void run() {
		ZipNode[] nodes = getSelectedNodes();
		if (nodes.length == 0 && fEditor != null)
			nodes = new ZipNode[] { fEditor.getRootNode() };
		String[] folder = openDialog(ActionMessages.getString("ExtractAction.2"), fSelectedFolder, false, false); //$NON-NLS-1$
		if (folder != null && folder.length > 0) {
			ExtractOperation operation = new ExtractOperation();
			operation.setRefreshJob(new RefreshJob());
			fSelectedFolder = operation.execute(nodes, new File(folder[0]), true, false).getAbsolutePath();
		}
	}

	protected void finished() {
		if (fSelectedFolder == null)
			return;
		try {
			IContainer[] resources = ResourcesPlugin.getWorkspace().getRoot().findContainersForLocation(
					new Path(fSelectedFolder));
			if (resources != null && resources.length > 0)
				resources[0].refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
		} catch (CoreException e) {
			ZipEditorPlugin.log(e);
		}
	}
}
