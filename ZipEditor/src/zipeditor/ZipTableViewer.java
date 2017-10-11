/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor;

import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.widgets.Composite;

public class ZipTableViewer extends TableViewer {

	public ZipTableViewer(Composite parent, int style) {
		super(parent, style);
	}

	public void fireSelectionChanged() {
		super.fireSelectionChanged(new SelectionChangedEvent(this, getSelection()));
	}

	protected void internalRefresh(Object element) {
		IContentProvider contentProvider = getContentProvider();
		if (contentProvider instanceof LazyZipContentProvider)
			((LazyZipContentProvider) contentProvider).refreshCachedElements(getInput());
		super.internalRefresh(element);
	}
}
