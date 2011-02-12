/*
 * (c) Copyright 2002, 2010 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.actions;

import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import zipeditor.ZipEditorPlugin;
import zipeditor.model.Node;

public class CopyAction extends ViewerAction {
	private boolean fFullyQualified;
	private String fLineSeparator = System.getProperty("line.separator"); //$NON-NLS-1$
	private Clipboard fClipboard;

	public CopyAction(StructuredViewer viewer, boolean fullyQualified, Clipboard clipboard) {
		super(ActionMessages.getString(fullyQualified ? "CopyAction.2" : "CopyAction.0"), viewer); //$NON-NLS-1$ //$NON-NLS-2$
		setToolTipText(ActionMessages.getString(fullyQualified ? "CopyAction.3" : "CopyAction.1")); //$NON-NLS-1$ //$NON-NLS-2$
		setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(
				ISharedImages.IMG_TOOL_COPY));
		fFullyQualified = fullyQualified;
		fClipboard = clipboard;
	}

	public void run() {
		Node[] nodes = getSelectedNodes();
		StringBuilder text = new StringBuilder();
		for (int i = 0; i < nodes.length; i++) {
			if (i > 0)
				text.append(fLineSeparator);
			text.append(fFullyQualified ? nodes[i].getPath() + nodes[i].getName(): nodes[i].getName());
		}
		if (text.length() > 0) {
			try {
				fClipboard.setContents(
					new Object[] { text.toString() },
					new Transfer[] { TextTransfer.getInstance() });
			} catch (SWTError e) {
				ZipEditorPlugin.showErrorDialog(
						getViewer().getControl().getShell(),
						ActionMessages.getString("CopyAction.2"), e); //$NON-NLS-1$
			}
		}
	}
}
