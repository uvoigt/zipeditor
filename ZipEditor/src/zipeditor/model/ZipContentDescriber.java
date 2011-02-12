/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.model;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import net.sf.sevenzipjbinding.ArchiveFormat;

import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.content.IContentDescriber;
import org.eclipse.core.runtime.content.IContentDescription;

import zipeditor.ZipEditorPlugin;

public class ZipContentDescriber implements IContentDescriber {
	private final static Map contentTypes;
	
	static {
		contentTypes = new HashMap();
		ArchiveFormat[] formats = ArchiveFormat.values();
		for (int i = 0; i < formats.length; i++) {
			String type = makeContentTypeId(formats[i]);
			contentTypes.put(type, type);
		}
	}

	private static String makeContentTypeId(ArchiveFormat format) {
		return ZipEditorPlugin.PLUGIN_ID + "." + format.getMethodName().toLowerCase() + "file"; //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	private final static String EMPTY = "empty"; //$NON-NLS-1$
	
	public int describe(InputStream contents, IContentDescription description)
			throws IOException {

		String type = (String) contentTypes.get(makeContentTypeId(ZipModel.detectType(contents)));
		if (type == null)
			return INVALID;
		if (description == null) // TODO  || type == EMPTY)
			return VALID;

		String contentTypeId = description.getContentType() != null ? description.getContentType().getId() : null;
		if (type.equals(contentTypeId))
			return VALID;
//		if (type == TGZ_FILE && GZ_FILE.equals(contentTypeId))
//			return VALID;

		return INVALID;
	}

	public QualifiedName[] getSupportedOptions() {
		return IContentDescription.ALL;
	}
	
	public static boolean isForUs(String contentTypeId) {
		return contentTypes.containsKey(contentTypeId);
	}
}
