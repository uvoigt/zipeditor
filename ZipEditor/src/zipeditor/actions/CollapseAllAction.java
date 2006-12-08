/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.actions;

import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreeViewer;

import zipeditor.ZipEditorPlugin;

public class CollapseAllAction extends ViewerAction {
	public CollapseAllAction(StructuredViewer viewer) {
		super(ActionMessages.getString("CollapseAllAction.0"), viewer); //$NON-NLS-1$
		setToolTipText(ActionMessages.getString("CollapseAllAction.1")); //$NON-NLS-1$
		setImageDescriptor(ZipEditorPlugin.getImageDescriptor("icons/collapseall.gif")); //$NON-NLS-1$
	}

	public void run() {
		if (getViewer() instanceof TreeViewer)
			((TreeViewer) getViewer()).collapseAll();
	}

}
