package zipeditor.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
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
import zipeditor.model.ZipNodeProperty;

public class PreferencesAction extends EditorAction {
	private class ColumnAction extends Action {
		private int fType;

		private ColumnAction(ZipNodeProperty nodeProperty) {
			super(nodeProperty.toString());
			Integer[] columns = (Integer[]) PreferenceInitializer.split(
					fEditor.getPreferenceStore().getString(PreferenceConstants.VISIBLE_COLUMNS), PreferenceConstants.COLUMNS_SEPARATOR, Integer.class);
			setChecked(indexOf(columns, nodeProperty.getType()) != -1);
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
				return integers;
			} else {
				if (index != -1) {
					integers[index] = null;
				}
				return integers; 
			}
		}
		
		public void run() {
			fEditor.storeTableColumnPreferences();
			IPreferenceStore store = fEditor.getPreferenceStore();
			Integer[] columns = (Integer[]) PreferenceInitializer.split(store.getString(PreferenceConstants.VISIBLE_COLUMNS), PreferenceConstants.COLUMNS_SEPARATOR, Integer.class);
			columns = update(columns, isChecked());
			String newValue = PreferenceInitializer.join(columns, PreferenceConstants.COLUMNS_SEPARATOR);
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
	}

	private void fillColumnsMenu(MenuManager manager) {
		manager.add(new ColumnAction(ZipNodeProperty.PNAME));
		manager.add(new ColumnAction(ZipNodeProperty.PTYPE));
		manager.add(new ColumnAction(ZipNodeProperty.PDATE));
		manager.add(new ColumnAction(ZipNodeProperty.PSIZE));
		manager.add(new ColumnAction(ZipNodeProperty.PPACKED_SIZE));
		manager.add(new ColumnAction(ZipNodeProperty.PRATIO));
		manager.add(new ColumnAction(ZipNodeProperty.PCRC));
		manager.add(new ColumnAction(ZipNodeProperty.PPATH));
		manager.add(new ColumnAction(ZipNodeProperty.PATTR));
	}
	
}
