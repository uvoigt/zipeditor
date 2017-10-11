/*
 * (c) Copyright 2002, 2016 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.search;

import java.io.File;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;

import zipeditor.Utils;
import zipeditor.ZipEditor;
import zipeditor.ZipEditorPlugin;
import zipeditor.model.Node;

public class OpenArchiveAction extends Action {

	private IWorkbenchPage fPage;
	private File fFile;
	private Node fNodeSelection;

	public OpenArchiveAction(IWorkbenchPage page, File file, Node nodeSelection) {
		fPage = page;
		fFile = file;
		fNodeSelection = nodeSelection;
		setText(SearchMessages.getString("OpenArchiveAction.0")); //$NON-NLS-1$
		setImageDescriptor(ZipEditorPlugin.getImageDescriptor("icons/zipicon.gif")); //$NON-NLS-1$
	}

	public void run() {
		IFileStore fileStore = Utils.getFileStore(fFile);
		try {
			IEditorPart editor = fPage.openEditor(Utils.createEditorInput(fileStore), "zipeditor.ZipEditor"); //$NON-NLS-1$
			if (editor instanceof ZipEditor && fNodeSelection != null) {
				((ZipEditor) editor).getViewer().setSelection(new StructuredSelection(fNodeSelection), true);
			}
		} catch (PartInitException e) {
			ZipEditorPlugin.log(e);
		}
	}
}
