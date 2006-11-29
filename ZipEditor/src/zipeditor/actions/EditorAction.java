/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.actions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;

import zipeditor.ZipEditor;
import zipeditor.model.ZipNode;

public abstract class EditorAction extends Action {
	protected ZipEditor fEditor;
	private IStructuredSelection fSelection;

	protected EditorAction(String text, ZipEditor editor) {
		super(text);
		fEditor = editor;
	}

	protected ZipNode[] getSelectedNodes() {
		if (fEditor != null)
			return fEditor.getSelectedNodes();
		List list = getSelection().toList();
		ZipNode[] nodes = new ZipNode[list.size()];
		for (int i = 0; i < nodes.length; i++) {
			Object element = list.get(i);
			if (!(element instanceof ZipNode))
					return new ZipNode[0];
			nodes[i] = (ZipNode) element;
		}
		return nodes;
	}
	
	public IStructuredSelection getSelection() {
		return fSelection != null ? fSelection : StructuredSelection.EMPTY;
	}
	
	public void setSelection(IStructuredSelection selection) {
		fSelection = selection;
	}
}
