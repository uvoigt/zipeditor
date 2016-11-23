/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.model;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.content.IContentDescriber;
import org.eclipse.core.runtime.content.IContentDescription;

import zipeditor.ZipEditorPlugin;

public class ZipContentDescriber implements IContentDescriber {
	public final static String ZIP_FILE = ZipEditorPlugin.PLUGIN_ID + ".zipfile"; //$NON-NLS-1$
	public final static String GZ_FILE = ZipEditorPlugin.PLUGIN_ID + ".gzipfile"; //$NON-NLS-1$
	public final static String TAR_FILE = ZipEditorPlugin.PLUGIN_ID + ".tarfile"; //$NON-NLS-1$
	public final static String TGZ_FILE = ZipEditorPlugin.PLUGIN_ID + ".targzfile"; //$NON-NLS-1$
	public final static String BZ2_FILE = ZipEditorPlugin.PLUGIN_ID + ".bz2file"; //$NON-NLS-1$
	public final static String TBZ_FILE = ZipEditorPlugin.PLUGIN_ID + ".tarbz2file"; //$NON-NLS-1$
	
	private final static String EMPTY = "empty"; //$NON-NLS-1$

	public static final String[] ALL = {
		ZIP_FILE, GZ_FILE, TAR_FILE, TGZ_FILE, BZ2_FILE, TBZ_FILE
	};
	
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
