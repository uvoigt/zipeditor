/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor;

import java.text.DateFormat;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.program.Program;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import zipeditor.model.Node;
import zipeditor.model.NodeProperty;
import zipeditor.model.ZipNode;
import zipeditor.model.ZipNodeProperty;

public class ZipLabelProvider extends LabelProvider implements ITableLabelProvider {
	private static final DateFormat DATE_FORMAT = DateFormat.getDateTimeInstance();
	private int[] fOrder;

	public String getText(Object element) {
		return element instanceof Node ? ((Node) element).getName() : super
				.getText(element);
	}
	
	public Image getImage(Object element) {
		if (element instanceof Node) {
			Node node = (Node) element;
			if (node.isFolder())
				return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
			
			ImageDescriptor descriptor = PlatformUI.getWorkbench().getEditorRegistry().getImageDescriptor(node.getName());
			if (descriptor != null)
				return ZipEditorPlugin.getImage(descriptor);
			return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FILE);
		}
		return super.getImage(element);
	}

	public Image getColumnImage(Object element, int columnIndex) {
		if (fOrder == null)
			fOrder = initializeOrder();if (fOrder.length == 0)
				return null;
		switch (fOrder[columnIndex]) {
		default:
			return null;
		case NodeProperty.NAME:
			return getImage(element);
		}
	}
	
	public String getColumnText(Object element, int columnIndex) {
		if (!(element instanceof Node))
			return new String();
		if (fOrder == null)
			fOrder = initializeOrder();
		if (fOrder.length == 0)
			return new String();
		Node node = (Node) element;
		switch (fOrder[columnIndex]) {
		default:
			return getText(element);
		case NodeProperty.NAME:
			return node.getName();
		case NodeProperty.TYPE:
			Program program = Program.findProgram(node.getType());
			return program != null ? program.getName() : Messages.getFormattedString("ZipNodePropertyPage.0", node.getType()); //$NON-NLS-1$
		case NodeProperty.DATE:
			return formatDate(node.getTime());
		case NodeProperty.SIZE:
			return Long.toString(node.getSize());
		case ZipNodeProperty.PACKED_SIZE:
			return Long.toString(node instanceof ZipNode ? ((ZipNode) node).getCompressedSize() : 0);
		case NodeProperty.PATH:
			return node.getPath();
		case ZipNodeProperty.ATTR:
			return new String(node instanceof ZipNode ? ((ZipNode) node).getExtra() : new byte[0]);
		case ZipNodeProperty.CRC:
			return Long.toHexString(node instanceof ZipNode ? ((ZipNode) node).getCrc() : 0);
		case ZipNodeProperty.RATIO:
			return Long.toString(Math.max(Math.round(node instanceof ZipNode ? ((ZipNode) node).getRatio() : 0), 0)) + "%"; //$NON-NLS-1$
		}
	}

	private int[] initializeOrder() {
		IPreferenceStore store = ZipEditorPlugin.getDefault().getPreferenceStore();
		int[] values = (int[]) PreferenceInitializer.split(store.getString(PreferenceConstants.VISIBLE_COLUMNS), PreferenceConstants.COLUMNS_SEPARATOR, int.class);
		return values;
	}

	private String formatDate(long time) {
		return DATE_FORMAT.format(new Long(time));
	}
}
