/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.operations;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import zipeditor.Messages;
import zipeditor.Utils;
import zipeditor.ZipEditorPlugin;
import zipeditor.model.Node;
import zipeditor.model.NodeVisitor;
import zipeditor.model.RootNode;
import zipeditor.model.ZipModel;

public class ExtractOperation {
	private class ExtractJob extends Job {
		private Node[] fNodes;
		private File fTargetDir;
		private boolean fOverwrite;

		public ExtractJob(Node[] nodes, File targetDir) {
			super(Messages.getString("ExtractOperation.0")); //$NON-NLS-1$
			fNodes = nodes;
			fTargetDir = targetDir;
		}
		
		protected IStatus run(IProgressMonitor monitor) {
			monitor.beginTask(Messages.getString("ExtractOperation.1"), Utils.computeTotalNumber(fNodes, monitor)); //$NON-NLS-1$
			extract(fNodes, fTargetDir, fOverwrite, false, monitor);
			if (fRefreshJob != null)
				fRefreshJob.schedule();
			return Status.OK_STATUS;
		}
		
		public boolean belongsTo(Object family) {
			return family == ExtractFamily;
		}
	};
	
	public static final Object ExtractFamily = new Object();

	public static class ExtractRule implements ISchedulingRule {
		public boolean contains(ISchedulingRule rule) {
			return rule instanceof ExtractRule;
		}

		public boolean isConflicting(ISchedulingRule rule) {
			return rule instanceof ExtractRule;
		}
	};

	private class ExtractVisitor extends NodeVisitor {
		// can be null which means all nodes are to be extracted
		private Set fToBeExtracted;
		private boolean fOverwrite;
		private boolean fFullNodePath;
		private IProgressMonitor fMonitor;

		ExtractVisitor(Set toBeExtracted, boolean overwrite, boolean fullNodePath, IProgressMonitor monitor) {
			fToBeExtracted = toBeExtracted;
			fOverwrite = overwrite;
			fFullNodePath = fullNodePath;
			fMonitor = monitor;
			propagateResult = true;
		}

		public Object visit(Node node, Object argument) {
			File file = null;
			if (fToBeExtracted == null || fToBeExtracted.contains(node)) {
				file = internalExtract(node, (File) argument, fOverwrite, fFullNodePath, fMonitor);
				if (fToBeExtracted != null) {
					if (fToBeExtracted.size() == 1 && !node.isFolder())
						canceled = true;
					fToBeExtracted.remove(node);
				}
			}
			if (fMonitor.isCanceled())
				canceled = true;

			return file != null ? file : argument;
		}
	}

	private Job fRefreshJob;
	private int fUserStatus = ASK;
	
	private final static int ASK = 0x01;
	private final static int OVERRIDE = 0x02;

	private final static Set extracting = new HashSet();

	public File execute(Node[] nodes, File toDir, boolean inBackground, boolean overwrite) {
		return inBackground ? extractInBackground(nodes, toDir) :
			extract(nodes, toDir, overwrite, false, new NullProgressMonitor());
	}
	
	public void setRefreshJob(Job refreshJob) {
		fRefreshJob = refreshJob;
	}
	
	private File extractInBackground(Node[] nodes, File toDir) {
		if (nodes == null || nodes.length == 0)
			return toDir;
		File targetDir = determineFolderTarget(toDir != null ? toDir : nodes[0].getModel().getTempDir());
		ExtractJob job = new ExtractJob(nodes, targetDir);
		job.setRule(new ExtractRule());
		job.schedule();
		return targetDir;
	}

	public File extract(Node node, File toDir, boolean overwrite, IProgressMonitor monitor) {
		return internalExtract(node, toDir, overwrite, true, monitor);
	}

	private File internalExtract(Node node, File toDir, boolean overwrite, boolean fullNodePath, IProgressMonitor monitor) {

		toDir = determineFolderTarget(toDir);
		File file = null;
		if (node.isFolder()) {
			file = fullNodePath ? toDir : new File(toDir, node.getName());
			if (!file.exists())
				file.mkdirs();
		} else {
			file = new File(toDir, fullNodePath ? node.getFullPath() : node.getName());
			while (extracting.contains(file)) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					break;
				}
			}
			boolean writeFile = !file.exists();
			if (!writeFile && !overwrite && (fUserStatus & ASK) > 0 && !isTempFolder(toDir, node.getModel())) {
				switch (showWarning(file)) {
				case 0:
					writeFile = true;
					break;
				case 1:
					writeFile = true;
					fUserStatus &= -1 ^ ASK;
					fUserStatus |= OVERRIDE;
					break;
				case 3:
					fUserStatus &= -1 ^ (ASK | OVERRIDE);
					break;
				case 4:
					monitor.setCanceled(true);
					return file;
				}
			}
			if (writeFile || (fUserStatus & OVERRIDE) > 0) {
				File parent = file.getParentFile();
				if (!parent.exists())
					parent.mkdirs();
				monitor.subTask(file.getName());
				try {
					extracting.add(file);
					long time = System.currentTimeMillis();
					try (FileOutputStream out = new FileOutputStream(file)) {
						IOUtils.copyLarge(node.getContent(), out);
					}
					if (ZipEditorPlugin.DEBUG)
						System.out.println("Extracted " + node + " in " + (System.currentTimeMillis() - time) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				} catch (Exception e) {
					node.getModel().logError(e);
				} finally {
					extracting.remove(file);
				}
			}
			monitor.worked(1);
		}
		return file;
	}

	private void computeNodesToBeExtracted(Node node, Set result) {
		if (node.isFolder()) {
			Node[] children = node.getChildren();
			for (int i = 0; i < children.length; i++) {
				computeNodesToBeExtracted(children[i], result);
			}
		}
		result.add(node);
	}
	
	private int showWarning(final File file) {
		if (Utils.isUIThread()) {
			return doShowWarning(file);
		} else {
			final int[] result = { -1 };
			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					result[0] = doShowWarning(file);
				}
			});
			return result[0];
		}
	}
	
	private int doShowWarning(File file) {
		MessageDialog dialog = new MessageDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
				Messages.getString("ExtractOperation.2"), //$NON-NLS-1$
				null, Messages.getFormattedString("ExtractOperation.3", file.getAbsolutePath()), //$NON-NLS-1$
				MessageDialog.QUESTION,
				new String[] { IDialogConstants.YES_LABEL, IDialogConstants.YES_TO_ALL_LABEL,
				IDialogConstants.NO_LABEL, IDialogConstants.NO_TO_ALL_LABEL, IDialogConstants.CANCEL_LABEL },
				0);
		return dialog.open();
	}
	
	private boolean isTempFolder(File toDir, ZipModel model) {
		File tmp = model.getTempDir();
		return toDir.getAbsolutePath().startsWith(tmp.getAbsolutePath());
	}

	public File extract(Node[] nodes, File toDir, boolean overwrite, boolean fullNodePath, IProgressMonitor monitor) {
		if (nodes == null || nodes.length == 0)
			return toDir;
		File targetDir = determineFolderTarget(toDir != null ? toDir : nodes[0].getModel().getTempDir());
		Set toBeExtracted = null;
		ZipModel model = null;
		for (int i = 0; i < nodes.length; i++) {
			Node node = nodes[i];
			if (model == null) {
				model = node.getModel();
				if (node instanceof RootNode && nodes.length == 1)
					break;
				toBeExtracted = new HashSet();
			}
			computeNodesToBeExtracted(node, toBeExtracted);
		}
		try {
			ExtractVisitor visitor = new ExtractVisitor(toBeExtracted, overwrite, fullNodePath, monitor);
			model.getRoot().accept(visitor, targetDir);
		} catch (Exception e) {
			ZipEditorPlugin.log(e);
		}
		return toDir;
	}

	private File determineFolderTarget(File file) {
		while (file != null && !file.isDirectory())
			file = file.getParentFile();
		return file;
	}
}
