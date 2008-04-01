/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor;

import java.io.File;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;

import zipeditor.model.Node;
import zipeditor.operations.ExtractOperation;

public class ZipEditorDragAdapter extends DragSourceAdapter {
	private ISelectionProvider fSelectionProvider;
	private String[] fTempPaths;

	public ZipEditorDragAdapter(ISelectionProvider selectionProvider) {
		fSelectionProvider = selectionProvider;
	}
	
	public void dragSetData(DragSourceEvent event) {
		final Node[] nodes = Utils.getSelectedNodes(fSelectionProvider.getSelection());
		if (nodes.length == 0)
			return;
		boolean createTempFiles = fTempPaths == null || fTempPaths.length != nodes.length;
		if (createTempFiles) {
			fTempPaths = new String[nodes.length];
			final File tmpDir = nodes[0].getModel().getTempDir();
			for (int i = 0; i < nodes.length; i++) {
				fTempPaths[i] = new File(tmpDir, nodes[i].getName()).getAbsolutePath();
			}
			ExtractOperation extractOperation = new ExtractOperation();
			extractOperation.execute(nodes, tmpDir, true, true);
		}
		try {
			Platform.getJobManager().join(ExtractOperation.ExtractFamily, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		event.data = fTempPaths;
	}

	public void dragFinished(DragSourceEvent event) {
//		if (!event.doit)
//			return;
		fTempPaths = null;
	}
}
