/*
 * (c) Copyright 2010 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.Saveable;
import org.eclipse.ui.navigator.CommonNavigator;

import zipeditor.ZipEditorPlugin;

public class SaveZipFileAction extends AbstractZipFileAction {

	public void run(IAction action) {
		final CommonNavigator navigator = getNavigator();
		if (navigator == null)
			return;
		Saveable[] saveables = navigator.getSaveables();
		for (int i = 0; i < saveables.length; i++) {
			final Saveable saveable = saveables[i];
			IFile[] files = getSelectedFiles();
			for (int j = 0; j < files.length; j++) {
				IFile file = files[j];
				if (file.getName().equals(saveable.getName())) {
					try {
						runWithProgress(navigator.getSite(), saveable);
					} catch (InvocationTargetException e) {
						ZipEditorPlugin.showErrorDialog(navigator.getSite().getShell(), "Error when saving", e.getTargetException());
					} catch (InterruptedException e) {
						break;
					}
					navigator.getCommonViewer().refresh(file, true);
				}
			}
		}
	}

	private void runWithProgress(final IWorkbenchPartSite site, final Saveable saveable)
			throws InvocationTargetException, InterruptedException {
		site.getWorkbenchWindow().run(false, true, new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException,
					InterruptedException {
				try {
					saveable.doSave(monitor);
				} catch (CoreException e) {
					ZipEditorPlugin.showErrorDialog(site.getShell(), "File " + saveable.getName() + " could not be saved", e);
				}
			}
		});
	}
}
