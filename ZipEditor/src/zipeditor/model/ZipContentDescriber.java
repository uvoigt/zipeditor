/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.model;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.content.IContentDescriber;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeSettings;

import zipeditor.ZipEditorPlugin;

public class ZipContentDescriber implements IContentDescriber {
	private final static Set ALL_TYPES = new HashSet();

	public final static String ZIP_FILE = add("zipfile"); //$NON-NLS-1$
	public final static String GZ_FILE = add("gzipfile"); //$NON-NLS-1$
	public final static String TAR_FILE = add("tarfile"); //$NON-NLS-1$
	public final static String TGZ_FILE = add("targzfile"); //$NON-NLS-1$
	public final static String BZ2_FILE = add("bz2file"); //$NON-NLS-1$
	public final static String TBZ_FILE = add("tarbz2file"); //$NON-NLS-1$

	private final static String EMPTY = "empty"; //$NON-NLS-1$

	private static IContentType fArchiveContentType;

	public static String[] getAllContentTypeIds() {
		return (String[]) ALL_TYPES.toArray(new String[ALL_TYPES.size()]);
	}

	private static String add(String s) {
		String contentTypeId = ZipEditorPlugin.PLUGIN_ID + '.' + s;
		ALL_TYPES.add(contentTypeId);
		return contentTypeId;
	}

	public static boolean isForUs(String contentTypeId) {
		return ALL_TYPES.contains(contentTypeId);
	}

	public static IContentType getArchiveContentType() {
		if (fArchiveContentType == null)
			fArchiveContentType = Platform.getContentTypeManager().getContentType("ZipEditor.archive"); //$NON-NLS-1$
		return fArchiveContentType;
	}

	public static List getFileExtensionsAssociatedWithArchives() {
		return getFileSpecs(IContentTypeSettings.FILE_EXTENSION_SPEC);
	}

	public static List getFileNamesAssociatedWithArchives() {
		return getFileSpecs(IContentTypeSettings.FILE_NAME_SPEC);
	}

	private static List getFileSpecs(int type) {
		String[] contentTypeIds = getAllContentTypeIds();
		List result = new ArrayList();
		for (int i = 0; i < contentTypeIds.length; i++) {
			String[] specs = Platform.getContentTypeManager().getContentType(contentTypeIds[i]).getFileSpecs(type);
			for (int j = 0; j < specs.length; j++) {
				result.add(specs[j].toLowerCase());
			}
		}
		return result;
	}

	public static boolean matchesFileSpec(String name, List fileNames, List fileExtension) {
		for (int i = 0; i < fileNames.size(); i++) {
			if (name.equals(fileNames.get(i)))
				return true;
		}
		for (int i = 0; i < fileExtension.size(); i++) {
			if (name.endsWith("." + fileExtension.get(i))) //$NON-NLS-1$
				return true;
		}
		return false;
	}

	public int describe(InputStream contents, IContentDescription description)
			throws IOException {

		String type = detectType(contents);
		if (type == null)
			return INVALID;
		if (description == null || type == EMPTY)
			return VALID;

		String contentTypeId = description.getContentType() != null ? description.getContentType().getId() : null;
		if (type.equals(contentTypeId))
			return VALID;
		if (type == TGZ_FILE && GZ_FILE.equals(contentTypeId))
			return VALID;
		if (type == TBZ_FILE && BZ2_FILE.equals(contentTypeId))
			return VALID;

		return INVALID;
	}

	private String detectType(InputStream contents) {
		switch (ZipModel.detectType(contents)) {
		default:
			return null;
		case ZipModel.ZIP:
			return ZIP_FILE;
		case ZipModel.TAR:
			return TAR_FILE;
		case ZipModel.GZ:
			return GZ_FILE;
		case ZipModel.TARGZ:
			return TGZ_FILE;
		case ZipModel.BZ2:
			return BZ2_FILE;
		case ZipModel.TARBZ2:
			return TBZ_FILE;
		case ZipModel.EMPTY:
			return EMPTY; 
		}
	}

	public QualifiedName[] getSupportedOptions() {
		return IContentDescription.ALL;
	}

}
