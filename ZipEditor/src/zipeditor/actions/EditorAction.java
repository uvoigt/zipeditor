/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.actions;

import org.eclipse.jface.action.Action;

import zipeditor.ZipEditor;

public abstract class EditorAction extends Action {
	protected ZipEditor fEditor;

	protected EditorAction(String text, ZipEditor editor) {
		super(text);
		fEditor = editor;
	}
}
