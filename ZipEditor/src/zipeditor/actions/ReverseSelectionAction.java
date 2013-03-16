/*
 * (c) Copyright 2002, 2013 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.actions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;

import zipeditor.model.Node;

public class ReverseSelectionAction extends ViewerAction {

	public static final String ID = "zipeditor.command.reverseSelection"; //$NON-NLS-1$

	public ReverseSelectionAction(StructuredViewer viewer) {
		super(ActionMessages.getString("ReverseSelectionAction_0"), viewer); //$NON-NLS-1$
		setId(ID);
		setActionDefinitionId(ID);
	}

	public void run() {
		Object root = getViewer().getInput();
		if (root instanceof Node) {
			Set selection = new HashSet(((StructuredSelection) getViewer().getSelection()).toList());
			List newSelection = new ArrayList();
			reverseSelection((Node) root, selection, newSelection);
			getViewer().setSelection(new StructuredSelection(newSelection));
		}
	}

	private void reverseSelection(Node node, Set selection, List newSelection) {
		Node[] children = node.getChildren();
		for (int i = 0; i < children.length; i++) {
			Node child = children[i];
			if (!selection.contains(child))
				newSelection.add(child);
			reverseSelection(child, selection, newSelection);			
		}
	}
}
