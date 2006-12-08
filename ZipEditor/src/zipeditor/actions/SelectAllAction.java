/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.actions;

import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;

public class SelectAllAction extends ViewerAction {
	public SelectAllAction(StructuredViewer viewer) {
		super(ActionMessages.getString("SelectAllAction.0"), viewer); //$NON-NLS-1$
	}

	public void run() {
		StructuredViewer viewer = getViewer();
		if (viewer instanceof TreeViewer)
			((TreeViewer) viewer).getTree().selectAll();
		else if (viewer instanceof TableViewer)
			((TableViewer) viewer).getTable().selectAll();
		viewer.setSelection(viewer.getSelection());
	}
}
