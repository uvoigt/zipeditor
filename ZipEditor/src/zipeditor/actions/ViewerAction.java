/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredViewer;

import zipeditor.Utils;
import zipeditor.model.Node;

public abstract class ViewerAction extends Action {
	private StructuredViewer fViewer;

	public ViewerAction(String text, StructuredViewer viewer) {
		super(text);
		Assert.isNotNull(viewer);
		fViewer = viewer;
	}
	
	public StructuredViewer getViewer() {
		return fViewer;
	}
	
	protected Node[] getSelectedNodes() {
		return Utils.getSelectedNodes(getSelection());
	}
	
	protected Node getViewerInputAsNode() {
		return (Node) fViewer.getInput();
	}

	protected void refreshViewer() {
		if (fViewer.getControl() == null || fViewer.getControl().isDisposed())
			return;
		fViewer.getControl().setRedraw(false);
		fViewer.refresh();
		fViewer.getControl().setRedraw(true);
	}

	protected ISelection getSelection() {
		return fViewer.getSelection();
	}
}