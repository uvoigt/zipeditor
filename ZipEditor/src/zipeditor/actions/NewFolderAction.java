/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.actions;

import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.window.Window;

import zipeditor.model.Node;

public class NewFolderAction extends ViewerAction {
	public NewFolderAction(StructuredViewer viewer) {
		super(ActionMessages.getString("NewFolderAction.0"), viewer); //$NON-NLS-1$
	}

	public void run() {
		Node parent = null;
		Node[] nodes = getSelectedNodes();
		if (nodes.length == 1)
			parent = nodes[0].isFolder() ? nodes[0] : nodes[0].getParent();
		else
			parent = getViewerInputAsNode();
		
		InputDialog dialog = new InputDialog(getViewer().getControl().getShell(),
				ActionMessages.getString("NewFolderAction.1"), ActionMessages.getString("NewFolderAction.2"), null, null); //$NON-NLS-1$ //$NON-NLS-2$
		if (dialog.open() != Window.OK)
			return;
		String newName = dialog.getValue();
		Node newNode = parent.getModel().createFolderNode(parent, newName);
		getViewer().setSelection(new StructuredSelection(newNode), true);
	}
}
