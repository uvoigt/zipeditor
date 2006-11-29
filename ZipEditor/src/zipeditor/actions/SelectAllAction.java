/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.actions;

import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Tree;

import zipeditor.ZipEditor;

public class SelectAllAction extends EditorAction {
	public SelectAllAction(ZipEditor editor) {
		super(ActionMessages.getString("SelectAllAction.0"), editor); //$NON-NLS-1$
	}

	public void run() {
		StructuredViewer viewer = fEditor.getViewer();
		Control control = viewer.getControl();
		if (control instanceof Tree)
			((Tree) control).selectAll();
		else if (control instanceof Table)
			((Table) control).selectAll();
		viewer.setSelection(viewer.getSelection());
	}
}
