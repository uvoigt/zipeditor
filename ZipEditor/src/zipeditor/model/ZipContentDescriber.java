/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.model;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
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
	public static class ContentTypeId {

		public final static int ZIP = 1;
		public final static int GZ = 2;
		public final static int TAR = 3;
		public final static int TGZ = 4;
		public final static int BZ2 = 5;
		public final static int TBZ = 6;
		public final static int RPM = 7;

		public final static ContentTypeId ZIP_FILE = add("zipfile", ContentTypeId.ZIP); //$NON-NLS-1$
		public final static ContentTypeId GZ_FILE = add("gzipfile", ContentTypeId.GZ); //$NON-NLS-1$
		public final static ContentTypeId TAR_FILE = add("tarfile", ContentTypeId.TAR); //$NON-NLS-1$
		public final static ContentTypeId TGZ_FILE = add("targzfile", ContentTypeId.TGZ); //$NON-NLS-1$
		public final static ContentTypeId BZ2_FILE = add("bz2file", ContentTypeId.BZ2); //$NON-NLS-1$
		public final static ContentTypeId TBZ_FILE = add("tarbz2file", ContentTypeId.TBZ); //$NON-NLS-1$
		public final static ContentTypeId RPM_FILE = add("rpmfile", ContentTypeId.RPM); //$NON-NLS-1$
		// Avoid handling as file, due this is a dummy for invalid file contents.
		public final static ContentTypeId INVALID = new ContentTypeId("invalid", -1); // $NON-NLS-1$

		private static void init() {}

		private String id;
		private int ordinal;
		private ContentTypeId(String id, int ordinal) {
			this.id = id;
			this.ordinal = ordinal;
		}

		public String getId() {
			return id;
		}

		public int getOrdinal() {
			return ordinal;
		}
	}

	private final static Set ALL_TYPES = new HashSet();
	private final static Set STRINGS = new HashSet();

	private static IContentType fArchiveContentType;
	
	static {
		// force loading the class
		ContentTypeId.init();
	}

	public static String[] getAllContentTypeIds() {
		return (String[]) STRINGS.toArray(new String[STRINGS.size()]);
	}

	private static ContentTypeId add(String s, int n) {
		ContentTypeId id = new ContentTypeId(ZipEditorPlugin.PLUGIN_ID + '.' + s, n);
		ALL_TYPES.add(id);
		STRINGS.add(id.id);
		return id;
	}

	public static boolean isForUs(String contentTypeId) {
		return STRINGS.contains(contentTypeId);
	}

	public static IContentType getArchiveContentType() {
		if (fArchiveContentType == null)
			fArchiveContentType = Platform.getContentTypeManager().getContentType("ZipEditor.archive"); //$NON-NLS-1$
		return fArchiveContentType;
	}

	public static ContentTypeId getContentTypeForFileExtension(String fileExtension) {
		if (fileExtension != null) {
			for (Iterator it = ALL_TYPES.iterator(); it.hasNext(); ) {
				ContentTypeId typeId = (ContentTypeId) it.next();
				String[] specs = Platform.getContentTypeManager().getContentType(typeId.id).
						getFileSpecs(IContentTypeSettings.FILE_EXTENSION_SPEC);
				for (int j = 0; j < specs.length; j++) {
					if (fileExtension.equalsIgnoreCase(specs[j]))
						return typeId;
				}
			}
		}
		return null;
	}

	public static List getFileExtensionsAssociatedWithArchives() {
		return getFileSpecs(IContentTypeSettings.FILE_EXTENSION_SPEC);
	}

	public static List getFileNamesAssociatedWithArchives() {
		return getFileSpecs(IContentTypeSettings.FILE_NAME_SPEC);
	}

	private static List getFileSpecs(int type) {
		List result = new ArrayList();
		for (Iterator it = ALL_TYPES.iterator(); it.hasNext(); ) {
			ContentTypeId typeId = (ContentTypeId) it.next();
			String[] specs = Platform.getContentTypeManager().getContentType(typeId.id).getFileSpecs(type);
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

		ContentTypeId type = ZipModel.detectType(contents);
		if (type == null || description == null)
			return VALID;

		String contentTypeId = description.getContentType() != null ? description.getContentType().getId() : null;
		if (type.id.equals(contentTypeId))
			return VALID;
		if (type == ContentTypeId.TGZ_FILE && ContentTypeId.GZ_FILE.id.equals(contentTypeId))
			return VALID;
		if (type == ContentTypeId.TBZ_FILE && ContentTypeId.BZ2_FILE.id.equals(contentTypeId))
			return VALID;

		return INVALID;
	}

	public QualifiedName[] getSupportedOptions() {
		return IContentDescription.ALL;
	}

}
