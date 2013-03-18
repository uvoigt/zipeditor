/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.actions;

import org.eclipse.jface.viewers.StructuredViewer;

import zipeditor.ZipEditorPlugin;
import zipeditor.model.Node;
import zipeditor.operations.AddOperation;

public class AddAction extends DialogAction {
	public AddAction(StructuredViewer viewer) {
		super(ActionMessages.getString("AddAction.0"), viewer, true); //$NON-NLS-1$
		setToolTipText(ActionMessages.getString("AddAction.1")); //$NON-NLS-1$
		setImageDescriptor(ZipEditorPlugin.getImageDescriptor("icons/add.gif")); //$NON-NLS-1$
	}

	public void run() {
		String[] paths = openDialog(ActionMessages.getString("AddAction.2"), null, true, true); //$NON-NLS-1$);
		if (paths == null || paths.length == 0)
			return;
		Node[] selectedNodes = getSelectedNodes();
		Node targetNode = selectedNodes.length > 0 ? selectedNodes[0] : getViewerInputAsNode();
		AddOperation operation = new AddOperation();
		operation.execute(paths, targetNode, null, getViewer());
	}

}
