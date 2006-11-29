package zipeditor.actions;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.navigator.ICommonActionConstants;

import zipeditor.Utils;
import zipeditor.ZipEditor;
import zipeditor.model.FileAdapter;
import zipeditor.model.ZipNode;

public class OpenActionGroup extends ActionGroup {
	private OpenAction fOpenAction;

	public OpenActionGroup(ZipEditor editor) {
		fOpenAction = new OpenAction(editor);
		fOpenAction.setActionDefinitionId(ICommonActionConstants.OPEN);
	}

	public void fillContextMenu(IMenuManager menu) {
		IStructuredSelection selection = (IStructuredSelection) getContext().getSelection();
		boolean onlyFilesSelected = !selection.isEmpty() && Utils.allNodesAreFileNodes(selection);
		if (onlyFilesSelected) {
			fOpenAction.setSelection(selection);
			if (menu.find(IWorkbenchActionConstants.MB_ADDITIONS) != null)
				menu.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, fOpenAction);
			else
				menu.add(fOpenAction);
			fillOpenWithMenu(menu, selection);
		}
	}

	public void fillToolBarManager(IToolBarManager manager) {
		manager.add(new Separator());
		manager.add(fOpenAction);
	}
	
	public void fillActionBars(IActionBars actionBars) {
		if (actionBars.getGlobalActionHandler(ICommonActionConstants.OPEN) == null)
			actionBars.setGlobalActionHandler(ICommonActionConstants.OPEN, fOpenAction);
		updateActionBars();
	}
	
	public void updateActionBars() {
		IStructuredSelection selection = (IStructuredSelection) getContext().getSelection();
		boolean onlyFilesSelected = Utils.allNodesAreFileNodes(selection);
		boolean empty = selection.isEmpty();

		fOpenAction.setEnabled(!empty && onlyFilesSelected);
		fOpenAction.setSelection(selection);
	}
	
	private void fillOpenWithMenu(IMenuManager menu, IStructuredSelection selection) {

        if (selection.size() != 1)
			return;

        Object element = selection.getFirstElement();
        if (!(element instanceof ZipNode))
			return;

        MenuManager submenu = new MenuManager(ActionMessages.getString("ZipActionGroup.0"), PlatformUI.PLUGIN_ID + ".OpenWithSubMenu");  //$NON-NLS-1$//$NON-NLS-2$
        submenu.add(new OpenWithMenu(PlatformUI.getWorkbench().getActiveWorkbenchWindow().
        		getActivePage(), new FileAdapter((ZipNode) element)));
        if (menu.find(IWorkbenchActionConstants.MB_ADDITIONS) != null)
        	menu.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, submenu);
        else
        	menu.add(submenu);
	}

}
