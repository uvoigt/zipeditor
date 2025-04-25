/*
 * (c) Copyright 2002, 2016 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor;

import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.IIndexableLazyContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;

public class LazyZipContentProvider extends ZipContentProvider implements IIndexableLazyContentProvider {

	private TableViewer fTableViewer;
	private Object[] fRootChildren;

	public LazyZipContentProvider(int mode) {
		super(mode, true);
	}

	public int findElement(Object element) {
		if (fTableViewer == null) {
			return -1;
		}
		IElementComparer comparer = fTableViewer.getComparer();
		for (int i = 0; i < fRootChildren.length; i++) {
			if (comparer.equals(fRootChildren[i], element))
				return i;
		}
		return -1;
	}

	public void updateElement(int index) {
		if (index < fRootChildren.length)
			fTableViewer.replace(fRootChildren[index], index);
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		super.inputChanged(viewer, oldInput, newInput);

		if (viewer instanceof TableViewer && newInput != null) {
			fTableViewer = (TableViewer) viewer;
			refreshCachedElements(newInput);
		} else {
			fTableViewer = null;
		}
	}

	protected void disposeModels() {
		super.disposeModels();
		fRootChildren = null;
	}

	public void refreshCachedElements(Object input) {
		if (fTableViewer != null) {
			fRootChildren = getChildren(input);
			fTableViewer.setItemCount(fRootChildren.length);
			ViewerFilter[] filters = fTableViewer.getFilters();
			for (int i = 0; i < filters.length; i++) {
				fRootChildren = filters[i].filter(fTableViewer, input, fRootChildren);
			}
			ViewerComparator sorter = fTableViewer.getComparator();
			if (sorter != null)
				sorter.sort(fTableViewer, fRootChildren);
		}
	}
}
