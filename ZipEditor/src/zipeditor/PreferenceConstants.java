/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor;

import zipeditor.model.ZipContentDescriber.ContentTypeId;

public class PreferenceConstants {

	public final static String VIEW_MODE = "VIEW_MODE"; //$NON-NLS-1$

	public final static String TAR_SUFFIX = "_TAR"; //$NON-NLS-1$

	public final static String ZIP_SUFFIX = "_ZIP"; //$NON-NLS-1$

	public final static String SORT_BY = "SORT_BY"; //$NON-NLS-1$

	public final static String SORT_COLUMN_WIDTH = "SORT_COLUMN_WIDTH"; //$NON-NLS-1$

	public final static String SORT_DIRECTION = "SORT_DIRECTION"; //$NON-NLS-1$

	public final static String VISIBLE_COLUMNS = "VISIBLE_COLUMNS"; //$NON-NLS-1$

	public final static String COLUMNS_SEPARATOR = ","; //$NON-NLS-1$

	public final static String SORT_ENABLED = "SORT_ENABLED"; //$NON-NLS-1$

	public final static String ACTIVATE_ZSTD_LIB = "ACTIVE_ZSTD"; //$NON-NLS-1$
	
	public static final String SELECTED_ZSTD_LIB = "selectedZstdLib"; //$NON-NLS-1$

	public final static String EXTERNAL_EDITORS = "EXTERNAL_EDITORS"; //$NON-NLS-1$
	
	public final static String PREFIX_EDITOR = "editor"; //$NON-NLS-1$

	public final static String PREFIX_OUTLINE = "outline"; //$NON-NLS-1$

	public final static String PREFIX_NAVIGATOR = "commonNavigator"; //$NON-NLS-1$

	public final static int VIEW_MODE_TREE = 0x02;
	
	public final static int VIEW_MODE_FOLDERS_VISIBLE = 0x04;

	public final static int VIEW_MODE_FOLDERS_ONE_LAYER = 0x08;

	public final static String RECENTLY_USED_EDITORS = "recentlyUsedEditors"; //$NON-NLS-1$
	
	public final static String RECENTLY_USED_SEPARATOR = ","; //$NON-NLS-1$

	public final static String STORE_FOLDERS_IN_ARCHIVES = "storeFoldersInArchives"; //$NON-NLS-1$

	public static final String USE_ZSTD_AS_DEFAULT = "useZstdDefault"; //$NON-NLS-1$

	public static final String COMPRESSION_LEVEL = "compressionLevel"; //$NON-NLS-1$

	public static String getPreferenceSuffix(ContentTypeId type) {
		switch (type.getOrdinal()) {
		case ContentTypeId.ZIP:
			return ZIP_SUFFIX;
		case ContentTypeId.TAR:
		case ContentTypeId.TBZ:
		case ContentTypeId.TGZ:
			return TAR_SUFFIX;
		// for compatibility with older releases
		default:
			return ""; //$NON-NLS-1$
		}
	}

	private PreferenceConstants() {
	}
}
