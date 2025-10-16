/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import zipeditor.model.Node;
import zipeditor.model.ZipModel;

public class ZipContentProvider implements ITreeContentProvider {
	private final int fMode;
	private ZipModel fModel;
	private boolean fDisposeModel;

	public ZipContentProvider(int mode) {
		this(mode, false);
	}

	public ZipContentProvider(int mode, boolean disposeModel) {
		fMode = mode;
		fDisposeModel = disposeModel;
	}

	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof Node)
			return getNodeChildren((Node) parentElement);
		return new Object[0];
	}

	protected Object[] getNodeChildren(Node node) {
		if (node == null) {
			return new Object[0];
		}
		if (fDisposeModel && fModel != node.getModel())
			fModel = node.getModel();
		if ((fMode & PreferenceConstants.VIEW_MODE_TREE) > 0)
			return node.getChildren();
		else {
			List result = new ArrayList();
			addChildren(result, node, 0);
			return result.toArray();
		}
	}

	private void addChildren(List list, Node node, int depth) {
		Node[] children = node.getChildren();
		for (int i = 0; i < children.length; i++) {
			Node child = children[i];
			addChildren(list, child, depth + 1);
			boolean foldersVisible = (fMode & PreferenceConstants.VIEW_MODE_FOLDERS_VISIBLE) > 0;
			if (foldersVisible || !child.isFolder()) {
				boolean allInOneLayer = (fMode & PreferenceConstants.VIEW_MODE_FOLDERS_ONE_LAYER) > 0;
				if (depth == 0 || allInOneLayer)
					list.add(child);
			}
		}
	}

	public Object getParent(Object element) {
		return element instanceof Node ? ((Node) element).getParent() : null;
	}

	public boolean hasChildren(Object element) {
		return getChildren(element).length > 0;
	}

	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	public void dispose() {
		disposeModels();
	}

	protected void disposeModels() {
		if (fDisposeModel && fModel != null)
			fModel.dispose();
		fModel = null;
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (newInput == null)
			disposeModels();
	}

	public void disposeModel(boolean enable) {
		fDisposeModel = enable;
	}
}
