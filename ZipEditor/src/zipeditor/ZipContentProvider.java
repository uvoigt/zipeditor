/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import zipeditor.model.Node;
import zipeditor.model.ZipContentDescriber;
import zipeditor.model.ZipModel;

public class ZipContentProvider implements ITreeContentProvider {
	private int fMode = PreferenceConstants.VIEW_MODE_TREE;
	private Map fModels = new HashMap();
	private boolean fDisposeModel = true;

	public ZipContentProvider() {
	}

	public ZipContentProvider(int mode) {
		fMode = mode;
	}

	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof Node)
			return getNodeChildren((Node) parentElement);
		if (parentElement instanceof IFile)
			return getFileChildren((IFile) parentElement);
		return new Object[0];
	}

	private Object[] getNodeChildren(Node node) {
		fModels.put(null, node.getModel());
		if (fMode == PreferenceConstants.VIEW_MODE_TREE)
			return node.getChildren();
		else {
			List result = new ArrayList();
			addChildren(result, node);
			return result.toArray();
		}
	}

	private void addChildren(List list, Node node) {
		Node[] children = node.getChildren();
		for (int i = 0; i < children.length; i++) {
			Node child = children[i];
			addChildren(list, child);
			if (!child.isFolder())
				list.add(child);
		}
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
	
	private ZipModel getModel(IFile file) throws CoreException {
		ZipModel model = (ZipModel) fModels.get(file);
		if (model == null)
			fModels.put(file, model = new ZipModel(file.getLocation().toFile(), file.getContents()));
		return model;
	}

	public Object getParent(Object element) {
		return element instanceof Node ? ((Node) element).getParent() : null;
	}

	public boolean hasChildren(Object element) {
		if (element instanceof IFile)
			return isForUs((IFile) element);
		return getChildren(element).length > 0;
	}

	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	public void dispose() {
		disposeModels();
	}

	private void disposeModels() {
		if (fModels != null) {
			for (Iterator it = fModels.values().iterator(); it.hasNext();) {
				ZipModel model = (ZipModel) it.next();
				if (fDisposeModel)
					model.dispose();
			}
			fModels.clear();
		}
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (newInput == null)
			disposeModels();
	}

	private boolean isForUs(IFile file) {
		try {
			IContentDescription contentDescription = file.getContentDescription();
			if (contentDescription == null)
				return false;
			if (contentDescription.getContentType() == null)
				return false;
			String contentTypeId = contentDescription.getContentType().getId();
			return ZipContentDescriber.GZ_FILE.equals(contentTypeId)
					|| ZipContentDescriber.TAR_FILE.equals(contentTypeId)
					|| ZipContentDescriber.TGZ_FILE.equals(contentTypeId)
					|| ZipContentDescriber.ZIP_FILE.equals(contentTypeId);
		} catch (CoreException e) {
			ZipEditorPlugin.log(e);
			return false;
		}
	}

	public void disposeModel(boolean enable) {
		fDisposeModel = enable;
	}
}
