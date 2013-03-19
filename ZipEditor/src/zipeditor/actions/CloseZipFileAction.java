/*
 * (c) Copyright 2010 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.actions;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.IAction;
import org.eclipse.ui.navigator.CommonNavigator;

import zipeditor.ZipContentProvider;

public class CloseZipFileAction extends AbstractZipFileAction {

	public void run(IAction action) {
		CommonNavigator navigator = getNavigator();
		if (navigator == null)
			return;
		IFile[] files = getSelectedFiles();
		for (int i = 0; i < files.length; i++) {
			IFile file = files[i];
			((ZipContentProvider) getExtension().getContentProvider()).closeModelFor(file);
			navigator.getCommonViewer().setExpandedState(file, false);
			navigator.getCommonViewer().refresh(file, true);
		}
	}
}
