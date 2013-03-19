/*
 * (c) Copyright 2010 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor;

import java.io.File;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.ui.navigator.CommonDragAdapterAssistant;

import zipeditor.model.Node;
import zipeditor.operations.ExtractOperation;

public class ZipDragAssistant extends CommonDragAdapterAssistant {
	private static final Transfer[] SUPPORTED_TYPES = { FileTransfer
			.getInstance(), LocalSelectionTransfer.getTransfer() };

	public Transfer[] getSupportedTransferTypes() {
		return SUPPORTED_TYPES;
	}

	public boolean setDragData(DragSourceEvent event,
			IStructuredSelection selection) {
		// TODO zentralisieren (zipeditordragadapter)
		final Node[] nodes = Utils.getSelectedNodes(selection);
		if (nodes.length == 0)
			return false;
		String[] tempPaths = new String[nodes.length];
		final File tmpDir = nodes[0].getModel().getTempDir();
		for (int i = 0; i < nodes.length; i++) {
			Node node = nodes[i];
			File file = new File(tmpDir, node.getFullPath());
			tempPaths[i] = file.getAbsolutePath();
			if (node.isFolder())
				file.mkdirs();
		}
		Thread extractor = new Thread(new Runnable() {
			public void run() {
				ExtractOperation extractOperation = new ExtractOperation();
				extractOperation.extract(nodes, tmpDir, true, true, new NullProgressMonitor());
			}
		}, "Extractor"); //$NON-NLS-1$
		extractor.start();

		try {
			extractor.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		event.data = tempPaths;
		return true;
	}
}
