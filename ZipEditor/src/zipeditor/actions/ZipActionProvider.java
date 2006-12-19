/*
 * (c) Copyright 2006 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PropertyDialogAction;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;

public class ZipActionProvider extends CommonActionProvider {
	private OpenActionGroup fOpenActionGroup;
	private ViewerAction fExtractAction;
	private AddAction fAddAction;
	private IAction fPropertiesAction;
	
	public void init(ICommonActionExtensionSite aSite) {
		super.init(aSite);
		fOpenActionGroup = new OpenActionGroup(null);
		fExtractAction = new ExtractAction(aSite.getStructuredViewer());
		fAddAction = new AddAction(aSite.getStructuredViewer());
	}

	public void fillContextMenu(IMenuManager menu) {
		fOpenActionGroup.setContext(getContext());
		fOpenActionGroup.fillContextMenu(menu);
		IStructuredSelection selection = (IStructuredSelection) getContext().getSelection();
		menu.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, new Separator());
		menu.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, fAddAction);
		menu.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, fExtractAction);
		fExtractAction.setSelection(selection);
		fAddAction.setSelection(selection);
		menu.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, new Separator());
		if (fPropertiesAction == null)
			fPropertiesAction = new PropertyDialogAction(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart().getSite(), getActionSite().getViewSite().getSelectionProvider());
		menu.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, fPropertiesAction);
	}

	public void fillActionBars(IActionBars actionBars) {
		fOpenActionGroup.setContext(getContext());
		fOpenActionGroup.fillActionBars(actionBars);
	}

}
