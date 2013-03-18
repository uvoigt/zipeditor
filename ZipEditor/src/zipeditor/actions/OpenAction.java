/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.actions;

import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import zipeditor.Utils;
import zipeditor.model.Node;

public class OpenAction extends ViewerAction {
	
	public OpenAction(StructuredViewer viewer) {
		super(ActionMessages.getString("OpenAction.0"), viewer); //$NON-NLS-1$
		setToolTipText(ActionMessages.getString("OpenAction.1")); //$NON-NLS-1$
		setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
				getImageDescriptor(ISharedImages.IMG_OBJ_FILE));
	}

	public void run() {
		Node[] nodes = getSelectedNodes();
		if (nodes.length == 1 && nodes[0].isFolder() && getViewer() instanceof TreeViewer) {
			TreeViewer viewer = (TreeViewer) getViewer();
			viewer.setExpandedState(nodes[0], !viewer.getExpandedState(nodes[0]));
		} else {
			Utils.openFilesFromNodes(nodes);
		}
	}
}
