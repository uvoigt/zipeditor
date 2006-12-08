package zipeditor.actions;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.navigator.ICommonActionConstants;

import zipeditor.Utils;
import zipeditor.ZipEditor;
import zipeditor.actions.DeferredMenuManager.MenuJob;
import zipeditor.model.FileAdapter;
import zipeditor.model.Node;

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
	
	private void fillOpenWithMenu(IMenuManager menu, final IStructuredSelection selection) {
        if (selection.size() != 1)
			return;

        Object element = selection.getFirstElement();
        if (!(element instanceof Node))
			return;

        final FileAdapter adapter = new FileAdapter((Node) element);
        boolean isRunning = DeferredMenuManager.isRunning(adapter, null);
		if (adapter.isAdapted() && !isRunning) {
	        MenuManager subMenu = new MenuManager(ActionMessages.getString("ZipActionGroup.0"), PlatformUI.PLUGIN_ID + ".OpenWithSubMenu");  //$NON-NLS-1$//$NON-NLS-2$
			doAddToMenu(subMenu, adapter);
        	menu.add(subMenu);
		} else {
			MenuJob menuJob = new MenuJob(adapter, null) {
				protected IStatus addToMenu(IProgressMonitor monitor, IMenuManager menu) {
					doAddToMenu(menu, adapter);
					return Status.OK_STATUS;
				}
			};
			DeferredMenuManager.addToMenu(menu, ActionMessages.getString("ZipActionGroup.0"), PlatformUI.PLUGIN_ID + ".OpenWithSubMenu", menuJob); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private void doAddToMenu(IMenuManager subMenu, FileAdapter adapter) {
        subMenu.add(new OpenWithMenu(getActivePage(), adapter));
	}
	
	private IWorkbenchPage getActivePage() {
		if (Utils.isUIThread())
			return PlatformUI.getWorkbench().getActiveWorkbenchWindow()
					.getActivePage();
		else {
			final IWorkbenchPage[] result = { null };
			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					result[0] = PlatformUI.getWorkbench()
							.getActiveWorkbenchWindow().getActivePage();
				}
			});
			return result[0];
		}
	}
}
