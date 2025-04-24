/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.operations;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.ui.progress.UIJob;

import zipeditor.Messages;
import zipeditor.Utils;
import zipeditor.ZipEditorPlugin;
import zipeditor.model.Node;

public class AddOperation {
	private class AddFilesJob extends Job {
		private File[] fFilesNames;
		private Node fParentNode;
		private Node fBeforeSibling;
		private UIJob fRefreshJob;
		private boolean fUseZstdCompression;

		public AddFilesJob(File[] filesNames, Node parentNode, Node beforeSibling, UIJob refreshJob, boolean useZstdCompression) {
			super(Messages.getString("AddOperation.1")); //$NON-NLS-1$
			fFilesNames = filesNames;
			fParentNode = parentNode;
			fBeforeSibling = beforeSibling;
			fRefreshJob = refreshJob;
			fUseZstdCompression = useZstdCompression;
		}

		public IStatus run(IProgressMonitor monitor) {
			monitor.beginTask(Messages.getString("AddOperation.4"), 100); //$NON-NLS-1$
			monitor.worked(1);
			int totalWork = Utils.computeTotalNumber(fFilesNames, monitor);
			monitor.setTaskName(Messages.getString("AddOperation.2")); //$NON-NLS-1$
			SubMonitor subMonitor = SubMonitor.convert(monitor, 99);
			subMonitor.beginTask(Messages.getString("AddOperation.2"), totalWork); //$NON-NLS-1$
			try {
				boolean oneAdded = false;
				for (int i = 0; i < fFilesNames.length; i++) {
					if (subMonitor.isCanceled())
						break;
					try {
						fParentNode.add(fFilesNames[i], fBeforeSibling, subMonitor, fUseZstdCompression);
						oneAdded = true;
					} catch (Exception e) {
						return ZipEditorPlugin.createErrorStatus(Messages.getString("AddOperation.0"), e); //$NON-NLS-1$
					}
					subMonitor.worked(1);
				}
				if (oneAdded) {
					fRefreshJob.schedule();
				}
			} finally {
				subMonitor.done();
				monitor.done();
			}
			return Status.OK_STATUS;
		}
	};
	
	private class RefreshJob extends UIJob {
		private StructuredViewer fViewer;

		public RefreshJob(StructuredViewer viewer) {
			super(Messages.getString("AddOperation.3")); //$NON-NLS-1$
			fViewer = viewer;
		}

		public IStatus runInUIThread(IProgressMonitor monitor) {
			if (!fViewer.getControl().isDisposed()) {
				fViewer.refresh();
			}
			return Status.OK_STATUS;
		}
	};

	public void execute(String[] fileNames, Node parentNode, Node beforeSibling, StructuredViewer viewer) {
		// The default case is disabled zstd compressoin. This is done to not disturb all other cases. 
		execute(getFilesFromNames(fileNames), parentNode, beforeSibling, viewer, false);
	}

	public void execute(File[] fileNames, Node parentNode, Node beforeSibling, StructuredViewer viewer, boolean useZstdCompression) {
		while (parentNode != null && !parentNode.isFolder())
			parentNode = parentNode.getParent();
		AddFilesJob addFilesJob = new AddFilesJob(fileNames, parentNode, beforeSibling, new RefreshJob(viewer), useZstdCompression);
		addFilesJob.schedule();
	}

	private File[] getFilesFromNames(String[] filesNames) {
		File[] files = new File[filesNames.length];
		for (int i = 0; i < files.length; i++) {
			files[i] = new File(filesNames[i]);
		}
		return files;
	}
}
