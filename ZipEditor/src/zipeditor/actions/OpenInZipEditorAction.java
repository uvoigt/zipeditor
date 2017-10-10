/*
 * (c) Copyright 2002, 2016 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.actions;

import java.io.File;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.IPath;
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
	private File fFile;

	public void run(IAction action) {
		IFileStore fileStore = Utils.getFileStore(fFile);
		try {
			fActivePart.getSite().getPage().openEditor(Utils.createEditorInput(fileStore), "zipeditor.ZipEditor"); //$NON-NLS-1$
		} catch (PartInitException e) {
			ZipEditorPlugin.log(e);
		}
	}

	public void selectionChanged(IAction action, ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			Object firstElement = ((IStructuredSelection) selection).getFirstElement();
			IPath path = Utils.getJavaPackageFragmentRoot(firstElement);
			if (path != null)
				fFile = path.toFile();
		}
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		fActivePart = targetPart;
	}
}
