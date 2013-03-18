package zipeditor.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ToolItem;

import zipeditor.PreferenceConstants;
import zipeditor.PreferenceInitializer;
import zipeditor.ZipEditor;
import zipeditor.ZipEditorPlugin;
import zipeditor.model.NodeProperty;
import zipeditor.model.ZipNodeProperty;

public class PreferencesAction extends EditorAction {
	private class ColumnAction extends Action {
		private int fType;
		private Integer[] fColumnsState;

		private ColumnAction(NodeProperty nodeProperty) {
			super(nodeProperty.toString());
			fColumnsState = (Integer[]) PreferenceInitializer.split(
					fEditor.getPreferenceStore().getString(PreferenceConstants.VISIBLE_COLUMNS), PreferenceConstants.COLUMNS_SEPARATOR, Integer.class);
			setChecked(indexOf(fColumnsState, nodeProperty.getType()) != -1);
			fType = nodeProperty.getType();
		}
		
		private int indexOf(Integer[] integers, int type) {
			for (int i = 0; i < integers.length; i++) {
				if (type == integers[i].intValue())
					return i;
			}
			return -1;
		}
		
		private Integer[] update(Integer[] integers, boolean set) {
			int index = indexOf(integers, fType);
			if (set) {
				if (index == -1) {
					int size = integers.length;
					System.arraycopy(integers, 0, integers = new Integer[size + 1], 1, size);
					integers[0] = new Integer(fType);
				}
			} else {
				if (index != -1) {
					integers[index] = null;
				}
			}
			return integers;
		}
		
		public void run() {
			fEditor.storeTableColumnPreferences();
			IPreferenceStore store = fEditor.getPreferenceStore();
			fColumnsState = update(fColumnsState, isChecked());
			String newValue = PreferenceInitializer.join(fColumnsState, PreferenceConstants.COLUMNS_SEPARATOR);
			store.setValue(PreferenceConstants.VISIBLE_COLUMNS, newValue);
			fEditor.updateView(fEditor.getMode(), false);
		}
	};
	
	private MenuManager fMenuManager;
	
	public PreferencesAction(ZipEditor editor) {
		super(ActionMessages.getString("PreferencesAction.0"), editor); //$NON-NLS-1$
		setToolTipText(ActionMessages.getString("PreferencesAction.1")); //$NON-NLS-1$
		setImageDescriptor(ZipEditorPlugin.getImageDescriptor("icons/arrow_down.gif")); //$NON-NLS-1$
	}
	
	public void runWithEvent(Event event) {
		if (fMenuManager == null)
			fillMenuManager(fMenuManager = new MenuManager());
		ToolItem item = (ToolItem) event.widget;
		Menu menu = fMenuManager.createContextMenu((item.getParent()));

		Rectangle bounds = item.getBounds();
		Point topLeft = new Point(bounds.x, bounds.y + bounds.height);
		topLeft = item.getParent().toDisplay(topLeft);
		menu.setLocation(topLeft.x, topLeft.y);
		menu.setVisible(true);
	}

	private void fillMenuManager(MenuManager manager) {
		MenuManager columns = new MenuManager(ActionMessages.getString("PreferencesAction.2")); //$NON-NLS-1$
		manager.add(columns);
		fillColumnsMenu(columns);
		MenuManager folders = new MenuManager(ActionMessages.getString("PreferencesAction.3")); //$NON-NLS-1$
		manager.add(folders);
		fillFoldersMenu(folders);
		manager.add(new Separator());
		manager.add(new ToggleStoreFoldersAction(fEditor));
	}

	private void fillColumnsMenu(MenuManager manager) {
		manager.add(new ColumnAction(NodeProperty.PNAME));
		manager.add(new ColumnAction(NodeProperty.PTYPE));
		manager.add(new ColumnAction(NodeProperty.PDATE));
		manager.add(new ColumnAction(NodeProperty.PSIZE));
		manager.add(new ColumnAction(ZipNodeProperty.PPACKED_SIZE));
		manager.add(new ColumnAction(ZipNodeProperty.PRATIO));
		manager.add(new ColumnAction(ZipNodeProperty.PCRC));
		manager.add(new ColumnAction(NodeProperty.PPATH));
		manager.add(new ColumnAction(ZipNodeProperty.PATTR));
	}
	
	private void fillFoldersMenu(MenuManager manager) {
		Action foldersVisibleAction = new ToggleViewModeAction(fEditor, ActionMessages.getString("PreferencesAction.4"), PreferenceConstants.PREFIX_EDITOR, PreferenceConstants.VIEW_MODE_FOLDERS_VISIBLE); //$NON-NLS-1$
		ToggleViewModeAction allInOneLayerAction = new ToggleViewModeAction(fEditor, ActionMessages.getString("PreferencesAction.5"), PreferenceConstants.PREFIX_EDITOR, PreferenceConstants.VIEW_MODE_FOLDERS_ONE_LAYER); //$NON-NLS-1$
		allInOneLayerAction.setEnabled(foldersVisibleAction.isChecked());
		
		manager.add(foldersVisibleAction);
		manager.add(allInOneLayerAction);
	}
}
