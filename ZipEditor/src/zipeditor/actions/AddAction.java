/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.actions;

import java.io.File;

import org.eclipse.jface.viewers.StructuredViewer;

import zipeditor.ZipEditorPlugin;
import zipeditor.model.Node;
import zipeditor.model.ZipContentDescriber.ContentTypeId;
import zipeditor.model.zstd.ZstdUtilities;
import zipeditor.operations.AddOperation;
import zipeditor.preferences.PreferenceUtils;

public class AddAction extends DialogAction {
	public AddAction(StructuredViewer viewer) {
		super(ActionMessages.getString("AddAction.0"), viewer, true); //$NON-NLS-1$
		setToolTipText(ActionMessages.getString("AddAction.1")); //$NON-NLS-1$
		setImageDescriptor(ZipEditorPlugin.getImageDescriptor("icons/add.gif")); //$NON-NLS-1$
	}

	public void run() {
		Node[] selectedNodes = getSelectedNodes();
		Node targetNode = selectedNodes.length > 0 ? selectedNodes[0] : getViewerInputAsNode();
		File[] paths = openDialog(ActionMessages.getString("AddAction.2"), null, true, true, PreferenceUtils.isZstdAvailableAndActive() && targetNode.getModel().getType() == ContentTypeId.ZIP_FILE); //$NON-NLS-1$);
		if (paths == null || paths.length == 0)
			return;
		AddOperation operation = new AddOperation();
		operation.execute(paths, targetNode, null, getViewer(), ZstdUtilities.useZstdCompression(targetNode.getModel().getRoot()));
	}

}
