/*
 * (c) Copyright 2002, 2017 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.actions;

import org.eclipse.ui.IEditorPart;

public interface IPostOpenProcessor {

	void postOpen(IEditorPart editor);
}
