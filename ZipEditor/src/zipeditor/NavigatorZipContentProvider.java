/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.content.IContentDescription;

import zipeditor.model.ZipContentDescriber;
import zipeditor.model.ZipModel;

public class NavigatorZipContentProvider extends ZipContentProvider {
	private final Map fModels = new HashMap();

	public NavigatorZipContentProvider() {
		super(PreferenceConstants.VIEW_MODE_TREE);
	}

	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof IFile)
			return getFileChildren((IFile) parentElement);
		return super.getChildren(parentElement);
	}

	public boolean hasChildren(Object element) {
		if (element instanceof IFile)
			return isForUs((IFile) element);
		return super.hasChildren(element);
	}

	private Object[] getFileChildren(IFile file) {
		if (!isForUs(file))
			return new Object[0];
		try {
			ZipModel model = getModel(file);
			while (model.isInitializing()) {
				try {
					Thread.sleep(100);
				} catch (Exception ignore) {
				}
			}
			return getNodeChildren(model.getRoot());
		} catch (CoreException e) {
			ZipEditorPlugin.log(e);
			return new Object[0];
		}
	}

	private boolean isForUs(IFile file) {
		try {
			IContentDescription contentDescription = file.getContentDescription();
			if (contentDescription == null)
				return false;
			if (contentDescription.getContentType() == null)
				return false;
			String contentTypeId = contentDescription.getContentType().getId();
			return ZipContentDescriber.isForUs(contentTypeId);
		} catch (CoreException e) {
			ZipEditorPlugin.log(e);
			return false;
		}
	}

	private ZipModel getModel(IFile file) throws CoreException {
		ZipModel model = (ZipModel) fModels.get(file);
		if (model != null) {
			if (file.getLocation().toFile().lastModified() > model.getInitTime()) {
				fModels.remove(file);
				model.dispose();
				model = null;
			}
		}
		if (model == null)
			fModels.put(file, model = new ZipModel(file.getLocation().toFile(),
					file.getContents(), /*file.isReadOnly()*/true));
		return model;
	}

	protected void disposeModels() {
		if (fModels != null) {
			for (Iterator it = fModels.values().iterator(); it.hasNext();) {
				ZipModel model = (ZipModel) it.next();
				model.dispose();
			}
			fModels.clear();
		}
		super.disposeModels();
	}
}
