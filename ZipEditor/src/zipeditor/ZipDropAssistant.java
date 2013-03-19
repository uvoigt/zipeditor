/*
 * (c) Copyright 2010 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor;

import java.io.File;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.CommonDropAdapter;
import org.eclipse.ui.navigator.CommonDropAdapterAssistant;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.navigator.CommonViewer;

import zipeditor.model.Node;
import zipeditor.operations.AddOperation;
import zipeditor.operations.ExtractOperation;

public class ZipDropAssistant extends CommonDropAdapterAssistant {

	public boolean isSupportedType(TransferData transferType) {
		if (super.isSupportedType(transferType))
			return true;
		return FileTransfer.getInstance().isSupportedType(transferType);
	}
	

	public IStatus validateDrop(Object target, int operation,
			TransferData transferType) {

		switch (operation) {
		default:
			return Status.CANCEL_STATUS;
		case DND.DROP_MOVE:
		case DND.DROP_COPY:
			return Status.OK_STATUS;
		}
	}

	public IStatus handleDrop(CommonDropAdapter adapter, DropTargetEvent event,
			Object target) {
		String[] names = null;
		if (event.data instanceof StructuredSelection) {
			StructuredSelection selection = (StructuredSelection) event.data;
			Object[] objects = selection.toArray();
			names = new String[objects.length];
			for (int i = 0; i < names.length; i++) {
				Object object = objects[i];
				if (object instanceof IResource) {
					names[i] = ((IResource) object).getLocation().toFile().getAbsolutePath();
				} else if (object instanceof Node) {
					final Node node = ((Node) object);
					final File tmpDir = node.getModel().getTempDir();
					File file = new File(tmpDir, node.getFullPath());
					names[i] = file.getAbsolutePath();
					if (node.isFolder())
						file.mkdirs();
					Thread extractor = new Thread(new Runnable() {
						public void run() {
							ExtractOperation extractOperation = new ExtractOperation();
							extractOperation.extract(new Node[] { node }, tmpDir, true, true, new NullProgressMonitor());
						}
					}, "Extractor"); //$NON-NLS-1$
					extractor.start();

					try {
						extractor.join();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		} else if (event.data instanceof String[]) {
			names = (String[]) event.data;
		} else {
			return Status.CANCEL_STATUS;
		}
		Node selectedNode = null;
		if (target instanceof IResource) {
			selectedNode = ZipEditorPlugin.getSpace().getModel(((IResource) target).getLocation().toFile()).getRoot();
		} else {
			selectedNode = (Node) target;
		}
		Node parentNode = selectedNode;
		if (!selectedNode.isFolder())
			parentNode = selectedNode.getParent();
		AddOperation operation = new AddOperation();
		String viewerId = getContentService().getViewerId();
		IViewPart view = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(viewerId);
		if (view instanceof CommonNavigator) {
			CommonViewer viewer = ((CommonNavigator) view).getCommonViewer();
			operation.execute(names, parentNode, selectedNode, viewer);
		} else {
			ZipEditorPlugin.log(new Exception("No appropriate common viewer: " + view)); //$NON-NLS-1$
		}
		return Status.OK_STATUS;
	}
}
