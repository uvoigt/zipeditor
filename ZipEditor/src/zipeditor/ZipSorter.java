/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;

import zipeditor.actions.ToggleViewModeAction;
import zipeditor.model.ZipNode;
import zipeditor.model.ZipNodeProperty;

public class ZipSorter extends ViewerSorter {
	private int fSortBy;
	private int fSortDirection;
	private boolean fSortEnabled;
	private int fMode;
	
	public ZipSorter() {
		update(); 
	}

	public int compare(Viewer viewer, Object e1, Object e2) {
		if (!fSortEnabled)
			return 0;
		if (e1 instanceof ZipNode && e2 instanceof ZipNode) {
			return compareNodes(viewer, (ZipNode) e1, (ZipNode) e2);
		}
		return super.compare(viewer, e1, e2);
	}
	
	private int compareNodes(Viewer viewer, ZipNode z1, ZipNode z2) {
		boolean ascending = fSortDirection == SWT.UP;
		if (fMode == ToggleViewModeAction.MODE_TREE) {
			if (z1.isFolder() && !z2.isFolder())
				return -1;
			if (z2.isFolder() && !z1.isFolder())
				return 1;
			return compare(z1.getName(), z2.getName(), true);
		}
			
		switch (fSortBy) {
		default:
			return 0;
		case ZipNodeProperty.NAME:
			return compare(z1.getName(), z2.getName(), ascending);
		case ZipNodeProperty.TYPE:
			return compare(z1.getType(), z2.getType(), ascending);
		case ZipNodeProperty.DATE:
			return compare(z1.getTime(), z2.getTime(), ascending);
		case ZipNodeProperty.SIZE:
			return compare(z1.getSize(), z2.getSize(), ascending);
		case ZipNodeProperty.RATIO:
			return compare(Math.round(z1.getRatio()), Math.round(z2.getRatio()), ascending);
		case ZipNodeProperty.PACKED_SIZE:
			return compare(z1.getCompressedSize(), z2.getCompressedSize(), ascending);
		case ZipNodeProperty.CRC:
			return compare(z1.getCrc(), z2.getCrc(), ascending);
		case ZipNodeProperty.ATTR:
			return 0;
		case ZipNodeProperty.PATH:
			return compare(z1.getPath(), z2.getPath(), ascending);
		}
	}
	
	private int compare(String s1, String s2, boolean ascending) {
		return ascending ? s1.compareToIgnoreCase(s2) : s2.compareToIgnoreCase(s1);
	}

	private int compare(long l1, long l2, boolean ascending) {
		return (int) (ascending ? l1 - l2 : l2 - l1);
	}

	public void update() {
		IPreferenceStore store = ZipEditorPlugin.getDefault().getPreferenceStore();
		fSortBy = store.getInt(PreferenceConstants.SORT_BY);
		fSortDirection = store.getInt(PreferenceConstants.SORT_DIRECTION);
		fSortEnabled = store.getBoolean(PreferenceConstants.SORT_ENABLED);
		fMode = store.getInt(PreferenceConstants.VIEW_MODE);
	}
	
}
