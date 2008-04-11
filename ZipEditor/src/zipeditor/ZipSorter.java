/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;

import zipeditor.model.Node;
import zipeditor.model.NodeProperty;
import zipeditor.model.ZipNode;
import zipeditor.model.ZipNodeProperty;

public class ZipSorter extends ViewerSorter {
	private int fSortBy;
	private int fSortDirection;
	private boolean fSortEnabled;
	private int fMode;
	private String fPreferencePrefix;
	
	public ZipSorter() {
		this(PreferenceConstants.PREFIX_NAVIGATOR);
	}
	
	public ZipSorter(String preferencePrefix) {
		fPreferencePrefix = preferencePrefix;
		update(); 
	}

	public int compare(Viewer viewer, Object e1, Object e2) {
		if (!fSortEnabled)
			return 0;
		if (e1 instanceof Node && e2 instanceof Node) {
			return compareNodes((Node) e1, (Node) e2);
		}
		return super.compare(viewer, e1, e2);
	}
	
	private int compareNodes(Node z1, Node z2) {
		boolean ascending = fSortDirection == SWT.UP;
		if ((fMode & PreferenceConstants.VIEW_MODE_TREE) > 0)
			return compareByNames(z1, z2, true);
			
		switch (fSortBy) {
		default:
			return 0;
		case NodeProperty.NAME:
			return compareByNames(z1, z2, ascending);
		case NodeProperty.TYPE:
			return compare(ZipLabelProvider.getTypeLabel(z1), ZipLabelProvider.getTypeLabel(z2), ascending);
		case NodeProperty.DATE:
			return compare(z1.getTime(), z2.getTime(), ascending);
		case NodeProperty.SIZE:
			return compare(z1.getSize(), z2.getSize(), ascending);
		case ZipNodeProperty.RATIO:
			return z1 instanceof ZipNode && z2 instanceof ZipNode ? compare(Math.round(((ZipNode) z1).getRatio()), Math.round(((ZipNode) z2).getRatio()), ascending) : 0;
		case ZipNodeProperty.PACKED_SIZE:
			return z1 instanceof ZipNode && z2 instanceof ZipNode ? compare(((ZipNode) z1).getCompressedSize(), ((ZipNode) z2).getCompressedSize(), ascending) : 0;
		case ZipNodeProperty.CRC:
			return z1 instanceof ZipNode && z2 instanceof ZipNode ? compare(((ZipNode) z1).getCrc(), ((ZipNode) z2).getCrc(), ascending) : 0;
		case ZipNodeProperty.ATTR:
			return 0;
		case NodeProperty.PATH:
			return compare(z1.getPath(), z2.getPath(), ascending);
		}
	}
	
	private int compareByNames(Node z1, Node z2, boolean ascending) {
		if (z1.isFolder() && !z2.isFolder())
			return ascending ? -1 : 1;
		if (z2.isFolder() && !z1.isFolder())
			return ascending ? 1 : -1;
		return compare(z1.getName(), z2.getName(), ascending);
	}

	private int compare(String s1, String s2, boolean ascending) {
		return ascending ? s1.compareToIgnoreCase(s2) : s2.compareToIgnoreCase(s1);
	}

	private int compare(long l1, long l2, boolean ascending) {
		if (l1 == l2)
			return 0;
		return l1 < l2 ? ascending ? -1 : 1 : ascending ? 1 : -1;
	}

	public void update() {
		IPreferenceStore store = ZipEditorPlugin.getDefault().getPreferenceStore();
		fSortBy = store.getInt(PreferenceConstants.SORT_BY);
		fSortDirection = store.getInt(PreferenceConstants.SORT_DIRECTION);
		fSortEnabled = store.getBoolean(fPreferencePrefix + PreferenceConstants.SORT_ENABLED);
		fMode = store.getInt(fPreferencePrefix + PreferenceConstants.VIEW_MODE);
	}
	
}
