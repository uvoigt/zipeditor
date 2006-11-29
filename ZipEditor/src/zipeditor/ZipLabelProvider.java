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
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import zipeditor.model.ZipNode;
import zipeditor.model.ZipNodeProperty;

public class ZipLabelProvider extends LabelProvider implements ITableLabelProvider {
	private static final DateFormat DATE_FORMAT = DateFormat.getDateTimeInstance();
	private int[] fOrder;

	public String getText(Object element) {
		return element instanceof ZipNode ? ((ZipNode) element).getName() : super
				.getText(element);
	}
	
	public Image getImage(Object element) {
		if (element instanceof ZipNode) {
			ZipNode node = (ZipNode) element;
			if (node.isFolder())
				return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
			
			ImageDescriptor descriptor = PlatformUI.getWorkbench().getEditorRegistry().getImageDescriptor(node.getName(), null);
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
		case ZipNodeProperty.NAME:
			return getImage(element);
		}
	}
	
	public String getColumnText(Object element, int columnIndex) {
		if (!(element instanceof ZipNode))
			return new String();
		if (fOrder == null)
			fOrder = initializeOrder();
		if (fOrder.length == 0)
			return new String();
		ZipNode node = (ZipNode) element;
		switch (fOrder[columnIndex]) {
		default:
			return getText(element);
		case ZipNodeProperty.NAME:
			return node.getName();
		case ZipNodeProperty.TYPE:
			return node.getType();
		case ZipNodeProperty.DATE:
			return formatDate(node.getTime());
		case ZipNodeProperty.SIZE:
			return Long.toString(node.getSize());
		case ZipNodeProperty.PACKED_SIZE:
			return Long.toString(node.getCompressedSize());
		case ZipNodeProperty.PATH:
			return node.getPath();
		case ZipNodeProperty.ATTR:
			return new String(node.getExtra());
		case ZipNodeProperty.CRC:
			return Long.toHexString(node.getCrc());
		case ZipNodeProperty.RATIO:
			return Long.toString(Math.max(Math.round(node.getRatio()), 0)) + "%"; //$NON-NLS-1$
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
