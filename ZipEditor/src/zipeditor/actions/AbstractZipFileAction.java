/*
 * (c) Copyright 2010 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.actions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.navigator.INavigatorContentExtension;

import zipeditor.ZipContentProvider;

public abstract class AbstractZipFileAction extends Action implements IObjectActionDelegate {
	private IFile[] selectedFiles;
	private IWorkbenchPart activePart;

	protected CommonNavigator getNavigator() {
		return activePart instanceof CommonNavigator ? (CommonNavigator) activePart : null;
	}

	protected INavigatorContentExtension getExtension() {
		CommonNavigator navigator = getNavigator();
		if (navigator == null)
			return null;
		return getNavigator().getNavigatorContentService().getContentExtensionById("zipeditor.navigatorContent"); //$NON-NLS-1$
	}

	public void selectionChanged(IAction action, ISelection selection) {
		List list = new ArrayList();
		if (selection instanceof StructuredSelection) {
			for (Iterator it = ((StructuredSelection) selection).iterator(); it.hasNext();) {
				Object object = it.next();
				if (object instanceof IFile)
					list.add(object);
			}
		}
		selectedFiles = (IFile[]) list.toArray(new IFile[list.size()]);
		INavigatorContentExtension extension = getExtension();
		if (extension == null)
			return;
		ITreeContentProvider contentProvider = extension.getContentProvider();
		if (contentProvider instanceof ZipContentProvider) {
			for (int i = 0; i < selectedFiles.length; i++) {
				if (extension != null  && (((ZipContentProvider) contentProvider).isForUs(selectedFiles[i]))) {
					getNavigator().getCommonViewer().refresh(selectedFiles[i], true);
				}
			}
		}
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		activePart = targetPart;
	}

	public IWorkbenchPart getActivePart() {
		return activePart;
	}

	public IFile[] getSelectedFiles() {
		return selectedFiles;
	}
}
