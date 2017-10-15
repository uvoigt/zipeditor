/*
 * (c) Copyright 2002, 2016 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.actions;

import java.io.File;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;

import zipeditor.Utils;
import zipeditor.ZipEditorPlugin;

public class OpenInZipEditorAction implements IObjectActionDelegate {
	private IWorkbenchPart fActivePart;
	private File[] fFiles;

	public void run(IAction action) {
		for (int i = 0; i < fFiles.length; i++) {
			openFileInZipEditor(fFiles[i]);
		}
	}

	private void openFileInZipEditor(File file) {
		IFileStore fileStore = Utils.getFileStore(file);
		try {
			fActivePart.getSite().getPage().openEditor(Utils.createEditorInput(fileStore), "zipeditor.ZipEditor"); //$NON-NLS-1$
		} catch (PartInitException e) {
			ZipEditorPlugin.log(e);
		}
	}

	public void selectionChanged(IAction action, ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			fFiles = Utils.getJavaPackageFragmentRoots(((IStructuredSelection) selection).toArray());
			action.setEnabled(fFiles.length > 0);
		}
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		fActivePart = targetPart;
	}
}
