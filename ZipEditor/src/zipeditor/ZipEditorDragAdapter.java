/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;

import zipeditor.model.ZipNode;
import zipeditor.operations.ExtractOperation;

public class ZipEditorDragAdapter extends DragSourceAdapter {
	private ZipEditor fEditor;
	private String[] fTempPaths;
	private ExtractOperation fExtractOperation;

	public ZipEditorDragAdapter(ZipEditor editor) {
		fEditor = editor;
	}
	
	public void dragSetData(DragSourceEvent event) {
		ZipNode[] nodes = fEditor.getSelectedNodes();
		if (nodes.length == 0)
			return;
		boolean createTempFiles = fTempPaths == null || fTempPaths.length != nodes.length;
		if (createTempFiles) {
			fTempPaths = new String[nodes.length];
			File tmpDir = nodes[0].getModel().getTempDir();
			for (int i = 0; i < nodes.length; i++) {
				fTempPaths[i] = new File(tmpDir, nodes[i].getFullPath()).getAbsolutePath();
			}
			fExtractOperation = new ExtractOperation();
			fExtractOperation.execute(nodes, tmpDir, true, true);
		}
		event.data = fTempPaths;
	}

	public void dragFinished(DragSourceEvent event) {
		if (!event.doit)
			return;
		IProgressService progressService = PlatformUI.getWorkbench().getProgressService();
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
			}
		};
		try {
			progressService.runInUI(progressService, op, new ExtractOperation.ExtractRule());
		} catch (InterruptedException e) {
		} catch (Exception e) {
			ZipEditorPlugin.log(e);
		}
		fTempPaths = null;
	}
}
