/*
 * (c) Copyright 2002, 2016 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.search;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.source.AnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.ui.editors.text.StorageDocumentProvider;

public class ResultEditorInputProvider extends StorageDocumentProvider {
	protected IAnnotationModel createAnnotationModel(Object element) throws CoreException {
		return new AnnotationModel();
	}

	public String getEncoding(Object element) {
		if (element instanceof ResultEditorInput) {
			return ((ResultEditorInput) element).getEncoding();
		} else {
			return super.getEncoding(element);
		}
	}
}
