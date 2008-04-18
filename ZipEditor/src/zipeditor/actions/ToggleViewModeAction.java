/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.actions;

import zipeditor.PreferenceConstants;
import zipeditor.ZipEditor;

public class ToggleViewModeAction extends EditorAction {
	private String fPreferenceKey;
	private int fModeConstant;
	private int fMode;

	public ToggleViewModeAction(ZipEditor editor, String text, String preferencePrefix, int modeConstant) {
		super(text, editor);

		fPreferenceKey = preferencePrefix + PreferenceConstants.VIEW_MODE;
		fModeConstant = modeConstant;
		
		fMode = editor.getPreferenceStore().getInt(fPreferenceKey);
		checkDependencies();
		setChecked((fMode & fModeConstant) > 0);
	}

	public void run() {
		fMode = (fMode & (fModeConstant ^ -1)) | (isChecked() ? fModeConstant : 0);
		checkDependencies();
		fEditor.getPreferenceStore().setValue(fPreferenceKey, fMode);
		fEditor.updateView(fMode, true);
	}

	private void checkDependencies() {
		boolean foldersVisible = (fMode & PreferenceConstants.VIEW_MODE_FOLDERS_VISIBLE) > 0;
		boolean allInOneLayer = (fMode & PreferenceConstants.VIEW_MODE_FOLDERS_ONE_LAYER) > 0;
		if (!foldersVisible && !allInOneLayer)
			fMode |= PreferenceConstants.VIEW_MODE_FOLDERS_ONE_LAYER;
		
	}
}
