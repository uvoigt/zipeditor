/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor;

import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.ui.part.PluginDropAdapter;

import zipeditor.model.Node;
import zipeditor.operations.AddOperation;

public class ZipEditorDropAdapter extends PluginDropAdapter {

	public ZipEditorDropAdapter(StructuredViewer viewer) {
		super(viewer);
	}
	
	public void dragEnter(DropTargetEvent event) {
		if (FileTransfer.getInstance().isSupportedType(event.currentDataType)
				&& event.detail == DND.DROP_DEFAULT) {
			event.detail = DND.DROP_COPY;
		}
		super.dragEnter(event);
	}

	public boolean validateDrop(Object target, int operation, TransferData transferType) {
        if (FileTransfer.getInstance().isSupportedType(transferType))
       		return true;
        return super.validateDrop(target, operation, transferType);
	}

	public boolean performDrop(Object data) {
		if (!(data instanceof String[]))
			return false;
		Node node = (Node) getCurrentTarget();
		if (node == null && getViewer().getInput() instanceof Node)
			node = (Node) getViewer().getInput();
		if (!node.isFolder())
			node = node.getParent();
		String[] names = (String[]) data;
		AddOperation operation = new AddOperation();
		operation.execute(names, node, (StructuredViewer) getViewer());
		return true;
	}

}
