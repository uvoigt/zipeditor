/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.operations;

import java.io.File;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import zipeditor.Messages;
import zipeditor.Utils;
import zipeditor.ZipEditorPlugin;

public class OpenFileOperation {

	public IEditorPart execute(File file) {
		IFileStore fileStore = Utils.getFileStore(file);
		if (!fileStore.fetchInfo().isDirectory() && fileStore.fetchInfo().exists()) {
			final IEditorInput input = Utils.createEditorInput(fileStore);
			final String editorId = Utils.getEditorId(fileStore);
			if (Utils.isUIThread()) {
				return openEditor(input, editorId);
			} else {
				final IEditorPart[] result = { null };
				Display.getDefault().syncExec(new Runnable() {
					public void run() {
						result[0] = openEditor(input, editorId);
					}
				});
				return result[0];
			}
		}
		return null;
	}

	private IEditorPart openEditor(IEditorInput input, String editorId) {
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		try {
			return page.openEditor(input, editorId);
		} catch (PartInitException e) {
			ZipEditorPlugin.log(e);
			MessageDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), Messages.getString("ZipEditor.8"), e.getMessage()); //$NON-NLS-1$
			return null;
		}
	}

}
