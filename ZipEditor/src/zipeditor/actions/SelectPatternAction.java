/*
 * (c) Copyright 2002, 2013 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.window.Window;

import zipeditor.model.Node;

public class SelectPatternAction extends ViewerAction {

	public static final String ID = "zipeditor.command.selectPattern"; //$NON-NLS-1$

	private String fPreviousValue;

	public SelectPatternAction(StructuredViewer viewer) {
		super(ActionMessages.getString("SelectPatternAction.0"), viewer); //$NON-NLS-1$
		setId(ID);
		setActionDefinitionId(ID);
	}

	public void run() {
		InputDialog dlg = new InputDialog(getViewer().getControl().getShell(), ActionMessages.getString("SelectPatternAction.1"), ActionMessages.getString("SelectPatternAction.2"), //$NON-NLS-1$ //$NON-NLS-2$
				fPreviousValue, null);
		if (dlg.open() != Window.OK)
			return;
		String value = dlg.getValue();
		if (value != null) {
			Object root = getViewer().getInput();
			if (root instanceof Node) {
				List selection = new ArrayList();
				StringMatcher matcher = new StringMatcher(value, true, false);
				collectMatches((Node) root, matcher, selection);
				getViewer().setSelection(new StructuredSelection(selection));
			}
			fPreviousValue = value;
		}
	}

	private void collectMatches(Node node, StringMatcher matcher, List selection) {
		Node[] children = node.getChildren();
		for (int i = 0; i < children.length; i++) {
			Node child = children[i];
			if (matcher.match(child.getName()))
				selection.add(child);
			collectMatches(child, matcher, selection);			
		}
	}
}
