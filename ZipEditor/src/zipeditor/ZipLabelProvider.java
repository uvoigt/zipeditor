/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
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
	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss"); //$NON-NLS-1$

	private static Object TYPE_LABEL_KEY = new Object();
	
	public static String getTypeLabel(Node node) {
		String label = (String) node.getProperty(TYPE_LABEL_KEY);
		if (label == null)
			node.setProperty(TYPE_LABEL_KEY, label = doGetTypeLabel(node));
		return label;
	}
	
	private static String doGetTypeLabel(Node node) {
		Program program = Program.findProgram(node.getType());
		IContentType contentType = Platform.getContentTypeManager().findContentTypeFor(node.getName());
		return node.isFolder() ? Messages.getString("ZipLabelProvider.1") //$NON-NLS-1$
				: contentType != null ? contentType.getName()
						: program != null ? program.getName()
								: node.getType() != null && node.getType().length() > 0 ?
										Messages.getFormattedString("ZipLabelProvider.2", node.getType()) //$NON-NLS-1$
										: Messages.getString("ZipLabelProvider.0"); //$NON-NLS-1$
	}

	private int[] fOrder;

	public String getText(Object element) {
		return element instanceof Node ? getNodeText((Node) element) : super
				.getText(element);
	}
	
	private String getNodeText(Node node) {
		String prefix = node.isAdded() ? "*" : node.isModified() ? ">" : new String(); //$NON-NLS-1$ //$NON-NLS-2$
		return prefix + node.getName();
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
			return getNodeText(node);
		case NodeProperty.TYPE:
			return getTypeLabel(node);
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
