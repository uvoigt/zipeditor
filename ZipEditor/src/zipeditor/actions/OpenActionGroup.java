package zipeditor.actions;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.navigator.ICommonActionConstants;

import zipeditor.Utils;
import zipeditor.actions.DeferredMenuManager.MenuJob;
import zipeditor.model.FileAdapter;
import zipeditor.model.Node;

public class OpenActionGroup extends ActionGroup {
	private OpenAction fOpenAction;
	private boolean fDisposed;

	private final static String GROUP_OPEN_RECENTLY_USED = "openRecentlyUsed"; //$NON-NLS-1$

	public OpenActionGroup(StructuredViewer viewer, boolean useOpenActionHandler) {
		fOpenAction = new OpenAction(viewer);
		if (useOpenActionHandler)
			fOpenAction.setActionDefinitionId(ICommonActionConstants.OPEN);
	}
	
	public void dispose() {
		fDisposed = true;
		super.dispose();
	}

	public void fillContextMenu(IMenuManager menu) {
		IStructuredSelection selection = (IStructuredSelection) getContext().getSelection();
		boolean onlyFilesSelected = !selection.isEmpty() && Utils.allNodesAreFileNodes(selection);
		if (onlyFilesSelected) {
			if (menu.find(IWorkbenchActionConstants.MB_ADDITIONS) != null) {
				menu.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, fOpenAction);
				menu.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, new GroupMarker(GROUP_OPEN_RECENTLY_USED));
			} else {
				menu.add(fOpenAction);
				menu.add(new GroupMarker(GROUP_OPEN_RECENTLY_USED));
			}
	        if (selection.size() == 1) {
		        Object element = selection.getFirstElement();
		        if (element instanceof Node) {
		        	Node node = (Node) element;
		            FileAdapter adapter = new FileAdapter(node);
		        	FileOpener fileOpener = new FileOpener(getActivePage(), node);
		        	fillOpenWithMenu(menu, adapter, fileOpener);
		        	fillMostRecentlyUsedItems(menu, adapter, fileOpener);
		        }
	        }
		}
	}

	public void fillToolBarManager(IToolBarManager manager) {
		manager.add(new Separator());
		manager.add(fOpenAction);
	}
	
	public void fillActionBars(IActionBars actionBars) {
		if (actionBars.getGlobalActionHandler(fOpenAction.getActionDefinitionId()) == null)
			actionBars.setGlobalActionHandler(fOpenAction.getActionDefinitionId(), fOpenAction);
		updateActionBars();
	}
	
	private void fillOpenWithMenu(IMenuManager menu, final FileAdapter fileAdapter, final FileOpener fileOpener) {
        boolean isRunning = DeferredMenuManager.isRunning(fileAdapter, null);
		if (fileAdapter.isAdapted() && !isRunning) {
	        MenuManager subMenu = new MenuManager(ActionMessages.getString("OpenActionGroup.0"), PlatformUI.PLUGIN_ID + ".OpenWithSubMenu");  //$NON-NLS-1$//$NON-NLS-2$
			doAddToMenu(subMenu, null, fileAdapter);
			if (menu.find(IWorkbenchActionConstants.MB_ADDITIONS) != null)
				menu.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, subMenu);
			else
				menu.add(subMenu);
		} else {
			MenuJob menuJob = new MenuJob(fileAdapter, null) {
				protected IStatus addToMenu(IProgressMonitor monitor, IMenuManager menu) {
					if (!fDisposed)
						doAddToMenu(menu, fileOpener, fileAdapter);
					return Status.OK_STATUS;
				}
			};
			DeferredMenuManager.addToMenu(menu, IWorkbenchActionConstants.MB_ADDITIONS, ActionMessages.getString("OpenActionGroup.0"), PlatformUI.PLUGIN_ID + ".OpenWithSubMenu", menuJob); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private void doAddToMenu(IMenuManager subMenu, FileOpener fileOpener, FileAdapter adapter) {
        subMenu.add(new OpenWithMenu(getActivePage(), fileOpener, adapter));
	}
	
	private void fillMostRecentlyUsedItems(IMenuManager menu, FileAdapter fileAdapter, FileOpener fileOpener) {
		if (menu.find(IWorkbenchActionConstants.MB_ADDITIONS) != null)
			menu.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, new MostRecentlyUsedMenu(fileOpener, fileAdapter));
		else
			menu.add(new MostRecentlyUsedMenu(fileOpener, fileAdapter));
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
