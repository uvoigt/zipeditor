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

	public ToggleViewModeAction(ZipEditor editor, String preferencePrefix) {
		super(ActionMessages.getString("ToggleViewModeAction.0"), editor); //$NON-NLS-1$
		setToolTipText(ActionMessages.getString("ToggleViewModeAction.1")); //$NON-NLS-1$
		setImageDescriptor(ZipEditorPlugin.getImageDescriptor("icons/togglemode.gif")); //$NON-NLS-1$
		fPreferenceKey = preferencePrefix + PreferenceConstants.VIEW_MODE;
		
		int mode = editor.getPreferenceStore().getInt(fPreferenceKey);
		setChecked(mode == PreferenceConstants.VIEW_MODE_TREE);
	}
	
	public void run() {
		int mode = fEditor.getPreferenceStore().getInt(fPreferenceKey);
		mode = mode == PreferenceConstants.VIEW_MODE_FOLDER ? PreferenceConstants.VIEW_MODE_TREE : PreferenceConstants.VIEW_MODE_FOLDER;
		fEditor.getPreferenceStore().setValue(fPreferenceKey, mode);
		fEditor.updateView(mode, true);
	}
}
