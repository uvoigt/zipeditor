/*
 * (c) Copyright 2002, 2016 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.search;

import java.io.File;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;

import zipeditor.LocalFileEditorInput;
import zipeditor.Utils;
import zipeditor.ZipEditorPlugin;

public class OpenArchiveAction extends Action {

	private IWorkbenchPage fPage;
	private File fFile;

	public OpenArchiveAction(IWorkbenchPage page, File file) {
		fPage = page;
		fFile = file;
		setText(SearchMessages.getString("OpenArchiveAction.0")); //$NON-NLS-1$
		setImageDescriptor(ZipEditorPlugin.getImageDescriptor("icons/zipicon.gif")); //$NON-NLS-1$
	}

	public void run() {
		IFileStore fileStore = Utils.getFileStore(fFile);
		try {
			fPage.openEditor(new LocalFileEditorInput(fileStore), "zipeditor.ZipEditor"); //$NON-NLS-1$
		} catch (PartInitException e) {
			ZipEditorPlugin.log(e);
		}
	}
}
