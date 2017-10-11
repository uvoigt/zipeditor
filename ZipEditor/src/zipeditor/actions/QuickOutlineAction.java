/*
 * (c) Copyright 2002, 2017 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.actions;

import zipeditor.ZipEditor;

public class QuickOutlineAction extends EditorAction {

	public static final String ID = "zipeditor.command.quickOutline"; //$NON-NLS-1$

	public QuickOutlineAction(ZipEditor editor) {
		super(ActionMessages.getString("QuickOutlineAction.0"), editor); //$NON-NLS-1$
		setActionDefinitionId(ID);
	}

	public void run() {
		fEditor.showQuickOutline();
	}
}
