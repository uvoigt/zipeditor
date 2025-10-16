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
import zipeditor.model.ZipContentDescriber.ContentTypeId;
import zipeditor.preferences.PreferenceUtils;

public class PreferenceInitializer extends AbstractPreferenceInitializer {

	private static final String SEP_REPLACEMENT = Character.toString((char) 0x263a);

	public void initializeDefaultPreferences() {
		IPreferenceStore store = ZipEditorPlugin.getDefault().getPreferenceStore();

		String defaultVisibleColumns = join(new Object[] {
				new Integer(NodeProperty.NAME),
				new Integer(NodeProperty.TYPE),
				new Integer(NodeProperty.DATE),
				new Integer(NodeProperty.SIZE),
				new Integer(NodeProperty.PATH),
		}, PreferenceConstants.COLUMNS_SEPARATOR);
		String defaultSortBy = Integer.toString(NodeProperty.NAME);

		String tarSuffix = PreferenceConstants.getPreferenceSuffix(ContentTypeId.TAR_FILE);
		String zipSuffix = PreferenceConstants.getPreferenceSuffix(ContentTypeId.ZIP_FILE);

		// migrate from older releases
		String commonVisibleColumns = store.getString(PreferenceConstants.VISIBLE_COLUMNS);
		String zipVisibleColumns = store.getString(PreferenceConstants.VISIBLE_COLUMNS + zipSuffix);
		if (!defaultVisibleColumns.equals(commonVisibleColumns) && zipVisibleColumns.length() == 0) {
			store.setValue(PreferenceConstants.VISIBLE_COLUMNS + zipSuffix, commonVisibleColumns);
			store.setToDefault(PreferenceConstants.VISIBLE_COLUMNS);
		}
		String commonSortBy = store.getString(PreferenceConstants.SORT_BY);
		String zipSortBy = store.getString(PreferenceConstants.SORT_BY + zipSuffix);
		if (!defaultSortBy.equals(commonSortBy) && zipSortBy.length() == 0) {
			store.setValue(PreferenceConstants.SORT_BY + zipSuffix, commonSortBy);
			store.setToDefault(PreferenceConstants.SORT_BY);
		}
		for (int i = 1; i < 99; i++) {
			String commonColumnWidth = store.getString(PreferenceConstants.SORT_COLUMN_WIDTH + i);
			if (commonColumnWidth.length() > 0) {
				String zipColumnWidth = store.getString(PreferenceConstants.SORT_COLUMN_WIDTH + zipSuffix + i);
				if (zipColumnWidth.length() == 0) {
					store.setValue(PreferenceConstants.SORT_COLUMN_WIDTH + zipSuffix + i, commonColumnWidth);
				}
			}
		}
		//

		store.setDefault(PreferenceConstants.PREFIX_OUTLINE + PreferenceConstants.VIEW_MODE, PreferenceConstants.VIEW_MODE_TREE);
		store.setDefault(PreferenceConstants.PREFIX_OUTLINE + PreferenceConstants.SORT_ENABLED, true);
		store.setDefault(PreferenceConstants.PREFIX_NAVIGATOR + PreferenceConstants.VIEW_MODE, PreferenceConstants.VIEW_MODE_TREE);
		store.setDefault(PreferenceConstants.PREFIX_NAVIGATOR + PreferenceConstants.SORT_ENABLED, true);
		store.setDefault(PreferenceConstants.PREFIX_EDITOR + PreferenceConstants.VIEW_MODE, PreferenceConstants.VIEW_MODE_FOLDERS_ONE_LAYER);
		store.setDefault(PreferenceConstants.PREFIX_EDITOR + PreferenceConstants.SORT_ENABLED, true);
		store.setDefault(PreferenceConstants.PREFIX_EDITOR + PreferenceConstants.ACTIVATE_ZSTD_LIB, false);
		store.setDefault(PreferenceConstants.PREFIX_EDITOR + PreferenceConstants.COMPRESSION_LEVEL, 10);
		store.setDefault(PreferenceConstants.PREFIX_EDITOR + PreferenceConstants.USE_ZSTD_AS_DEFAULT, false);
		store.setDefault(PreferenceConstants.SORT_BY, defaultSortBy);
		store.setDefault(PreferenceConstants.SORT_DIRECTION, SWT.UP);
		store.setDefault(PreferenceConstants.VISIBLE_COLUMNS, defaultVisibleColumns);
		store.setDefault(PreferenceConstants.SORT_BY + tarSuffix, defaultSortBy);
		store.setDefault(PreferenceConstants.SORT_DIRECTION + tarSuffix, SWT.UP);
		store.setDefault(PreferenceConstants.VISIBLE_COLUMNS + tarSuffix, defaultVisibleColumns);
		store.setDefault(PreferenceConstants.SORT_BY + zipSuffix, defaultSortBy);
		store.setDefault(PreferenceConstants.SORT_DIRECTION + zipSuffix, SWT.UP);
		store.setDefault(PreferenceConstants.VISIBLE_COLUMNS + zipSuffix, defaultVisibleColumns);
	}

	public final static String join(Object array, String separator) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0, n = Array.getLength(array); i < n; i++) {
			if (sb.length() > 0)
				sb.append(separator);
			Object object = Array.get(array, i);
			if (object != null)
				sb.append(replaceSep(object.toString(), separator));
		}
		return sb.toString();
	}

	public final static Object split(String string, String separator, Class elementType) {
		StringTokenizer st = new StringTokenizer(string, separator);
		int size = st.countTokens();
		Object result = Array.newInstance(elementType, size);
		for (int i = 0; i < size; i++) {
			try {
				Array.set(result, i, valueFromString(unreplaceSep(st.nextToken(), separator), elementType));
			} catch (Exception e) {
				ZipEditorPlugin.log(e);
			}
		}
		return result;
	}

	private static String replaceSep(String s, String separator) {
		return s.replace(separator, SEP_REPLACEMENT);
	}

	private static String unreplaceSep(String s, String separator) {
		return s.replace(SEP_REPLACEMENT, separator);
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
