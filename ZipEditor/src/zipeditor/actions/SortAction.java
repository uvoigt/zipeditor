/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.actions;

import org.eclipse.jface.preference.IPreferenceStore;

import zipeditor.PreferenceConstants;
import zipeditor.ZipEditor;
import zipeditor.ZipEditorPlugin;
import zipeditor.ZipSorter;

public class SortAction extends EditorAction {
	public SortAction(ZipEditor editor) {
		super(ActionMessages.getString("SortAction.0"), editor); //$NON-NLS-1$
		setToolTipText(ActionMessages.getString("SortAction.1")); //$NON-NLS-1$
		setImageDescriptor(ZipEditorPlugin.getImageDescriptor("icons/alphab_sort_co.gif")); //$NON-NLS-1$
		boolean enabled = editor.getPreferenceStore().getBoolean(PreferenceConstants.SORT_ENABLED);
		setChecked(enabled);
	}

	public void run() {
		IPreferenceStore store = fEditor.getPreferenceStore();
		store.setValue(PreferenceConstants.SORT_ENABLED,
				!store.getBoolean(PreferenceConstants.SORT_ENABLED));
		((ZipSorter) fEditor.getViewer().getSorter()).update();
		fEditor.updateView(fEditor.getMode(), true);
	}

}
