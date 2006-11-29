/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.actions;

import zipeditor.PreferenceConstants;
import zipeditor.ZipEditor;
import zipeditor.ZipEditorPlugin;

public class ToggleViewModeAction extends EditorAction {
	public final static int MODE_FOLDER = 1;
	public final static int MODE_TREE = 2;

	private int fMode;

	public ToggleViewModeAction(ZipEditor editor) {
		super(ActionMessages.getString("ToggleViewModeAction.0"), editor); //$NON-NLS-1$
		setToolTipText(ActionMessages.getString("ToggleViewModeAction.1")); //$NON-NLS-1$
		setImageDescriptor(ZipEditorPlugin.getImageDescriptor("icons/togglemode.gif")); //$NON-NLS-1$
		
		fMode = editor.getPreferenceStore().getInt(PreferenceConstants.VIEW_MODE);
		setChecked(fMode == MODE_TREE);
	}
	
	public void run() {
		fMode = fMode == MODE_FOLDER ? MODE_TREE : MODE_FOLDER;
		fEditor.getPreferenceStore().setValue(PreferenceConstants.VIEW_MODE, fMode);
		fEditor.updateView(fMode, true);
	}

	public int getMode() {
		return fMode;
	}
}
