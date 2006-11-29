/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.actions;

import org.eclipse.jface.viewers.TreeViewer;

import zipeditor.ZipEditor;
import zipeditor.ZipEditorPlugin;

public class CollapseAllAction extends EditorAction {
	public CollapseAllAction(ZipEditor editor) {
		super(ActionMessages.getString("CollapseAllAction.0"), editor); //$NON-NLS-1$
		setToolTipText(ActionMessages.getString("CollapseAllAction.1")); //$NON-NLS-1$
		setImageDescriptor(ZipEditorPlugin.getImageDescriptor("icons/collapseall.gif")); //$NON-NLS-1$
		fEditor = editor;
	}

	public void run() {
		if (fEditor.getMode() == ToggleViewModeAction.MODE_TREE)
			((TreeViewer) fEditor.getViewer()).collapseAll();
	}

}
