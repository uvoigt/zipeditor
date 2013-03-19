/*
 * (c) Copyright 2010 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.actions;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.ui.navigator.CommonNavigator;

import zipeditor.ZipContentProvider;
import zipeditor.ZipEditorPlugin;

public class OpenZipFileAction extends AbstractZipFileAction {

	public void run(IAction action) {
		CommonNavigator navigator = getNavigator();
		if (navigator == null)
			return;
		IFile[] files = getSelectedFiles();
		for (int i = 0; i < files.length; i++) {
			IFile file = files[i];
			try {
				((ZipContentProvider) getExtension().getContentProvider()).openModelFor(file);
			} catch (CoreException e) {
				ZipEditorPlugin.showErrorDialog(getActivePart().getSite().getShell(), "File cannot be opened.", e);
				e.printStackTrace();
			}
			navigator.getCommonViewer().setExpandedState(file, true);
			navigator.getCommonViewer().refresh(file, true);
		}
	}

}
