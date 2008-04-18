/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.preference.IPreferenceStore;

public class TogglePreferenceAction extends Action {
	private String fPreferenceKey;
	private IPreferenceStore fStore;

	public TogglePreferenceAction(String text, String preferenceKey, IPreferenceStore store) {
		super(text);
		fPreferenceKey = preferenceKey;
		setChecked(store.getBoolean(fPreferenceKey));
		fStore = store;
	}

	public void run() {
		togglePreference();
	}

	protected void togglePreference() {
		fStore.setValue(fPreferenceKey, !fStore.getBoolean(fPreferenceKey));
	}
}
