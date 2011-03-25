/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor;

import java.io.File;

import org.eclipse.core.runtime.NullProgressMonitor;
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
		Thread extractor = null;
		if (createTempFiles) {
			fTempPaths = new String[nodes.length];
			final File tmpDir = nodes[0].getModel().getTempDir();
			for (int i = 0; i < nodes.length; i++) {
				Node node = nodes[i];
				File file = new File(tmpDir, node.getFullPath());
				fTempPaths[i] = file.getAbsolutePath();
				if (node.isFolder())
					file.mkdirs();
			}
			extractor = new Thread(new Runnable() {
				public void run() {
					ExtractOperation extractOperation = new ExtractOperation();
					extractOperation.extract(nodes, tmpDir, true, true, new NullProgressMonitor());
				}
			}, "Extractor"); //$NON-NLS-1$
			extractor.start();
		}
		if (extractor != null) {
			try {
				extractor.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		event.data = fTempPaths;
	}

	public void dragFinished(DragSourceEvent event) {
		fTempPaths = null;
	}
}
