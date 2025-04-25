/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor;

import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.ui.part.PluginDropAdapter;

import zipeditor.model.Node;
import zipeditor.model.zstd.ZstdUtilities;
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
		Node selectedNode = (Node) getCurrentTarget();
		Node parentNode = selectedNode;
		if (parentNode == null && getViewer().getInput() instanceof Node)
			parentNode = (Node) getViewer().getInput();
		if (!parentNode.isFolder())
			parentNode = parentNode.getParent();
		if (getViewer() instanceof TreeViewer && getCurrentLocation() == LOCATION_BEFORE
				&& parentNode != getViewer().getInput())
			parentNode = parentNode.getParent();
		String[] names = (String[]) data;
		AddOperation operation = new AddOperation();
		boolean useZstdCompression = ZstdUtilities.useZstdCompression(parentNode);
		operation.execute(names, parentNode, selectedNode, (StructuredViewer) getViewer(), useZstdCompression);
		return true;
	}
}
