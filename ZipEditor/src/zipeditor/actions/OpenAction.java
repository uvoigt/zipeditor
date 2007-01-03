/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.actions;

import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import zipeditor.Utils;
import zipeditor.ZipEditor;

public class OpenAction extends EditorAction {
	
	public OpenAction(ZipEditor editor) {
		super(ActionMessages.getString("OpenAction.0"), editor); //$NON-NLS-1$
		setToolTipText(ActionMessages.getString("OpenAction.1")); //$NON-NLS-1$
		setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
				getImageDescriptor(ISharedImages.IMG_OBJ_FILE));
	}

	public void run() {
		Utils.openFilesFromNodes(getSelectedNodes());
	}

}
