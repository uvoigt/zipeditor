package zipeditor.actions;

import zipeditor.PreferenceConstants;
import zipeditor.ZipEditor;

public class ToggleStoreFoldersAction extends EditorAction {

	protected ToggleStoreFoldersAction(ZipEditor editor) {
		super(ActionMessages.getString("PreferencesAction.6"), editor); //$NON-NLS-1$
		setChecked(editor.getRootNode().getModel().isStoreFolders());
	}

	public void run() {
		fEditor.getRootNode().getModel().setStoreFolders(isChecked());
		fEditor.getPreferenceStore().setValue(PreferenceConstants.STORE_FOLDERS_IN_ARCHIVES, isChecked());
	}
}
