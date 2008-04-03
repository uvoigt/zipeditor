/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.actions;

import zipeditor.PreferenceConstants;
import zipeditor.ZipEditor;
import zipeditor.ZipEditorPlugin;

public class ToggleViewModeAction extends EditorAction {
	private String fPreferenceKey;
	private int fMode;

	public ToggleViewModeAction(ZipEditor editor, String preferencePrefix) {
		super(ActionMessages.getString("ToggleViewModeAction.0"), editor); //$NON-NLS-1$
		setToolTipText(ActionMessages.getString("ToggleViewModeAction.1")); //$NON-NLS-1$
		setImageDescriptor(ZipEditorPlugin.getImageDescriptor("icons/togglemode.gif")); //$NON-NLS-1$
		fPreferenceKey = preferencePrefix + PreferenceConstants.VIEW_MODE;
		
		fMode = editor.getPreferenceStore().getInt(fPreferenceKey);
		setChecked(fMode == PreferenceConstants.VIEW_MODE_TREE);
	}
	
	public void run() {
		fMode = fMode == PreferenceConstants.VIEW_MODE_FOLDER ? PreferenceConstants.VIEW_MODE_TREE : PreferenceConstants.VIEW_MODE_FOLDER;
		fEditor.getPreferenceStore().setValue(fPreferenceKey, fMode);
		fEditor.updateView(fMode, true);
	}
}
