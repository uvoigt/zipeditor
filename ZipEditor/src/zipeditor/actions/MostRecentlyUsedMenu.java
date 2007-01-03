/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.actions;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import zipeditor.PreferenceConstants;
import zipeditor.PreferenceInitializer;
import zipeditor.ZipEditorPlugin;
import zipeditor.actions.FileOpener.Editor;
import zipeditor.model.FileAdapter;

public class MostRecentlyUsedMenu extends ContributionItem {
	private FileOpener fFileOpener;
	private FileAdapter fFileAdapter;

	public MostRecentlyUsedMenu(FileOpener fileOpener, FileAdapter fileAdapter) {
		fFileOpener = fileOpener;
		fFileAdapter = fileAdapter;
	}
	
	public void fill(Menu menu, int index) {
		createMostRecentlyUsedItems(menu, index);
	}

	private void createMostRecentlyUsedItems(Menu menu, int index) {
		IPreferenceStore store = ZipEditorPlugin.getDefault().getPreferenceStore();
		Editor[] recentlyUsedEditors = (Editor[]) PreferenceInitializer.split(store.getString(PreferenceConstants.RECENTLY_USED_EDITORS), PreferenceConstants.RECENTLY_USED_SEPARATOR, Editor.class);
		for (int i = 0, j = 0; i < recentlyUsedEditors.length; i++) {
			final Editor editor = recentlyUsedEditors[i];
			if (editor == null || editor.getLabel() == null) // error case
				continue;
			MenuItem item = new MenuItem(menu, SWT.PUSH, index + j++);
			item.setText(editor.getLabel());
			if (editor.getDescriptor() != null)
				item.setImage(ZipEditorPlugin.getImage(editor.getDescriptor().getImageDescriptor()));
			Listener listener = new Listener() {
				public void handleEvent(Event event) {
					switch (event.type) {
					case SWT.Selection:
						fFileOpener.openFromOther((IFileStore) fFileAdapter.getAdapter(IFileStore.class), editor);
						break;
					}
				}
			};
			item.addListener(SWT.Selection, listener);
		}
	}


}
