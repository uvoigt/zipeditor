/*
 * (c) Copyright 2002, 2016 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.search;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.search.ui.ISearchPageScoreComputer;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.IURIEditorInput;

import zipeditor.ZipEditorPlugin;
import zipeditor.model.Node;
import zipeditor.model.ZipContentDescriber;

public class ZipSearchScoreComputer implements ISearchPageScoreComputer {

	private Set fFileTypes = readFileTypes(ZipContentDescriber.getAllContentTypeIds());

	public int computeScore(String pageId, Object input) {
		if (ZipSearchPage.ID.equals(pageId)) {
			if (input instanceof IFile) {
				return computeForFile((IFile) input);
			} else if (input instanceof IFileEditorInput) {
				return computeForFile(((IFileEditorInput) input).getFile());
			} else if (input instanceof IURIEditorInput) {
				return computeForFileName(((IURIEditorInput) input).getName());
			} else if (input instanceof IPathEditorInput) {
				return computeForFileName(((IPathEditorInput) input).getName());
			} else if (input instanceof IStorageEditorInput) {
				try {
					return computeForFileName(((IStorageEditorInput) input).getStorage().getName());
				} catch (CoreException e) {
					ZipEditorPlugin.log(e);
				}
			} else if (input instanceof Node) {
				return 1;
			}
		}
		return ISearchPageScoreComputer.LOWEST;
	}

	private int computeForFileName(String name) {
		String extension = Path.fromOSString(name).getFileExtension();
		return fFileTypes.contains(extension) ? 1 : 0;
	}

	private Set readFileTypes(String[] contentTypes) {
		Set types = new HashSet();
		for (int i = 0; i < contentTypes.length; i++) {
			String[] specs = Platform.getContentTypeManager().getContentType(contentTypes[i]).getFileSpecs(IContentType.FILE_EXTENSION_SPEC);
			for (int j = 0; j < specs.length; j++) {
				types.add(specs[j]);
			}
		}
		return types;
	}

	private int computeForFile(IFile file) {
		try {
			IContentDescription contentDescription = file.getContentDescription();
			if (contentDescription != null) {
				IContentType contentType = contentDescription.getContentType();
				if (contentType != null && contentType.isKindOf(ZipContentDescriber.getArchiveContentType()))
					return 1;
			}
		} catch (CoreException e) {
			// ignore
		}
		return ISearchPageScoreComputer.LOWEST;
	}
}
