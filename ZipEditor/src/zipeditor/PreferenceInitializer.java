/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor;

import java.lang.reflect.Array;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;

import zipeditor.model.NodeProperty;

public class PreferenceInitializer extends AbstractPreferenceInitializer {

	public PreferenceInitializer() {
	}

	public void initializeDefaultPreferences() {
		IPreferenceStore store = ZipEditorPlugin.getDefault().getPreferenceStore();

		store.setDefault(PreferenceConstants.PREFIX_OUTLINE + PreferenceConstants.VIEW_MODE, PreferenceConstants.VIEW_MODE_TREE);
		store.setDefault(PreferenceConstants.PREFIX_OUTLINE + PreferenceConstants.SORT_ENABLED, true);
		store.setDefault(PreferenceConstants.PREFIX_NAVIGATOR + PreferenceConstants.VIEW_MODE, PreferenceConstants.VIEW_MODE_TREE);
		store.setDefault(PreferenceConstants.PREFIX_NAVIGATOR + PreferenceConstants.SORT_ENABLED, true);
		store.setDefault(PreferenceConstants.PREFIX_EDITOR + PreferenceConstants.VIEW_MODE, PreferenceConstants.VIEW_MODE_FOLDERS_ONE_LAYER);
		store.setDefault(PreferenceConstants.PREFIX_EDITOR + PreferenceConstants.SORT_ENABLED, true);
		store.setDefault(PreferenceConstants.SORT_BY, NodeProperty.NAME);
		store.setDefault(PreferenceConstants.SORT_DIRECTION, SWT.UP);
		store.setDefault(PreferenceConstants.VISIBLE_COLUMNS, join(new Object[] {
				new Integer(NodeProperty.NAME),
				new Integer(NodeProperty.TYPE),
				new Integer(NodeProperty.DATE),
				new Integer(NodeProperty.SIZE),
				new Integer(NodeProperty.PATH),
		}, PreferenceConstants.COLUMNS_SEPARATOR));
	}

	public final static String join(Object array, String separator) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0, n = Array.getLength(array); i < n; i++) {
			if (sb.length() > 0)
				sb.append(separator);
			Object object = Array.get(array, i);
			if (object != null)
				sb.append(object);
		}
		return sb.toString();
	}

	public final static Object split(String string, String separator, Class elementType) {
		StringTokenizer st = new StringTokenizer(string, separator);
		int size = st.countTokens();
		Object result = Array.newInstance(elementType, size);
		for (int i = 0; i < size; i++) {
			try {
				Array.set(result, i, valueFromString(st.nextToken(), elementType));
			} catch (Exception e) {
				ZipEditorPlugin.log(e);
			}
		}
		return result;
	}

	private static Object valueFromString(String string, Class type) throws Exception {
		if (type == int.class)
			type = Integer.class;
		else if (type == short.class)
			type = Short.class;
		else if (type == long.class)
			type = Long.class;
		else if (type == double.class)
			type = Double.class;
		else if (type == float.class)
			type = Float.class;
		else if (type == char.class)
			type = Character.class;
		else if (type == boolean.class)
			type = Boolean.class;
		return type.getConstructor(new Class[] { String.class }).newInstance(new Object[] { string });
	}
}
