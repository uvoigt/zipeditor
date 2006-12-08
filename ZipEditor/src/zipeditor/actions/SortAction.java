/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.actions;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.StructuredViewer;

import zipeditor.PreferenceConstants;
import zipeditor.ZipEditorPlugin;
import zipeditor.ZipSorter;

public class SortAction extends ViewerAction {
	private String fPreferenceKey;
	
	public final static String SORTING_CHANGED = "sorting_changed"; //$NON-NLS-1$

	public SortAction(StructuredViewer viewer, String preferencePrefix) {
		super(ActionMessages.getString("SortAction.0"), viewer); //$NON-NLS-1$
		setToolTipText(ActionMessages.getString("SortAction.1")); //$NON-NLS-1$
		setImageDescriptor(ZipEditorPlugin.getImageDescriptor("icons/alphab_sort_co.gif")); //$NON-NLS-1$
		fPreferenceKey = preferencePrefix + PreferenceConstants.SORT_ENABLED;
		setChecked(ZipEditorPlugin.getDefault().getPreferenceStore().getBoolean(fPreferenceKey));
	}

	public void run() {
		IPreferenceStore store = ZipEditorPlugin.getDefault().getPreferenceStore();
		store.setValue(fPreferenceKey, !store.getBoolean(fPreferenceKey));
		((ZipSorter) getViewer().getSorter()).update();

		refreshViewer();
		
		store.firePropertyChangeEvent(fPreferenceKey + SORTING_CHANGED, null, null);
	}

}
