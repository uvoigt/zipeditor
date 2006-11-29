/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.operations;

import java.io.File;
import java.io.FileOutputStream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import zipeditor.Messages;
import zipeditor.Utils;
import zipeditor.ZipEditorPlugin;
import zipeditor.model.ZipModel;
import zipeditor.model.ZipNode;

public class ExtractOperation {
	private class ExtractJob extends Job {
		private ZipNode[] fNodes;
		private File fTargetDir;
		private boolean fOverwrite;

		public ExtractJob(ZipNode[] nodes, File targetDir) {
			super(Messages.getString("ExtractOperation.0")); //$NON-NLS-1$
			fNodes = nodes;
			fTargetDir = targetDir;
		}
		
		protected IStatus run(IProgressMonitor monitor) {
			monitor.beginTask(Messages.getString("ExtractOperation.1"), Utils.computeTotalNumber(fNodes, monitor)); //$NON-NLS-1$
			extract(fNodes, fTargetDir, fOverwrite, monitor);
			if (fRefreshJob != null)
				fRefreshJob.schedule();
			return Status.OK_STATUS;
		}
	};
	
	public static class ExtractRule implements ISchedulingRule {
		public boolean contains(ISchedulingRule rule) {
			return rule instanceof ExtractRule;
		}

		public boolean isConflicting(ISchedulingRule rule) {
			return rule instanceof ExtractRule;
		}
	};
	
	private Job fRefreshJob;
	
	public File execute(ZipNode[] nodes, boolean inBackground, boolean overwrite) {
		return inBackground ? extractInBackground(nodes, null) :
			extract(nodes, null, overwrite, new NullProgressMonitor());
	}

	public File execute(ZipNode[] nodes, File toDir, boolean inBackground, boolean overwrite) {
		return inBackground ? extractInBackground(nodes, toDir) :
			extract(nodes, toDir, overwrite, new NullProgressMonitor());
	}
	
	public void setRefreshJob(Job refreshJob) {
		fRefreshJob = refreshJob;
	}
	
	private File extractInBackground(ZipNode[] nodes, File toDir) {
		if (nodes == null || nodes.length == 0)
			return toDir;
		File targetDir = determineFolderTarget(toDir != null ? toDir : nodes[0].getModel().getTempDir());
		ExtractJob job = new ExtractJob(nodes, targetDir);
		job.setRule(new ExtractRule());
		job.schedule();
		return targetDir;
	}

	public File extract(ZipNode node, File toDir, boolean overwrite, IProgressMonitor monitor) {
		return internalExtract(node, toDir, true, overwrite, monitor);
	}
	
	private File internalExtract(ZipNode node, File toDir, boolean isRoot, boolean overwrite, IProgressMonitor monitor) {
		if (monitor.isCanceled())
			return null;
		toDir = determineFolderTarget(toDir);
		File file = new File(toDir, isRoot ? node.getFullPath() : node.getName());
		if (node.isFolder()) {
			if (!file.exists())
				file.mkdirs();
			ZipNode[] children = node.getChildren();
			for (int i = 0; i < children.length; i++) {
				internalExtract(children[i], file, false, overwrite, monitor);
			}
		} else {
			boolean canCreate = !file.exists();
			if (!canCreate && !overwrite && !isTempFolder(toDir, node.getModel()))
				canCreate = showWarning(file);
			if (canCreate) {
				File parent = file.getParentFile();
				if (!parent.exists())
					parent.mkdirs();
				monitor.subTask(file.getName());
				try {
					Utils.readAndWrite(node.getContent(), new FileOutputStream(file), true);
				} catch (Exception e) {
					ZipEditorPlugin.log(e);
				}
			}
			monitor.worked(1);
		}
		return file;
	}
	
	private boolean showWarning(final File file) {
		if (Utils.isUIThread()) {
			return doShowWarning(file);
		} else {
			final boolean[] result = { false };
			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					result[0] = doShowWarning(file);
				}
			});
			return result[0];
		}
	}
	
	private boolean doShowWarning(File file) {
		return MessageDialog.openConfirm(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
				Messages.getString("ExtractOperation.2"), //$NON-NLS-1$
				Messages.getFormattedString("ExtractOperation.3", file.getAbsolutePath())); //$NON-NLS-1$
	}
	
	private boolean isTempFolder(File toDir, ZipModel model) {
		File tmp = model.getTempDir();
		return toDir.getAbsolutePath().startsWith(tmp.getAbsolutePath());
	}

	public File extract(ZipNode[] nodes, File toDir, boolean overwrite, IProgressMonitor monitor) {
		if (nodes == null || nodes.length == 0)
			return toDir;
		File targetDir = determineFolderTarget(toDir != null ? toDir : nodes[0].getModel().getTempDir());
		for (int i = 0; i < nodes.length; i++) {
			extract(nodes[i], targetDir, overwrite, monitor);
		}
		return toDir;
	}

	private File determineFolderTarget(File file) {
		while (file != null && !file.isDirectory())
			file = file.getParentFile();
		return file;
	}
}
