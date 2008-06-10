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
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionConstants;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;

public class ZipActionProvider extends CommonActionProvider {
	private OpenActionGroup fOpenActionGroup;
	private ViewerAction fExtractAction;
	private IAction fPropertiesAction;
	
	public void init(ICommonActionExtensionSite aSite) {
		super.init(aSite);
		fOpenActionGroup = new OpenActionGroup(null, ICommonActionConstants.OPEN);
		fExtractAction = new ExtractAction(aSite.getStructuredViewer());
	}
	
	public void dispose() {
		super.dispose();
		fOpenActionGroup.dispose();
	}

	public void fillContextMenu(IMenuManager menu) {
		fOpenActionGroup.setContext(getContext());
		fOpenActionGroup.fillContextMenu(menu);
		IStructuredSelection selection = (IStructuredSelection) getContext().getSelection();
		menu.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, new Separator());
		menu.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, fExtractAction);
		fExtractAction.setSelection(selection);
		menu.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, new Separator());
		if (fPropertiesAction == null)
			fPropertiesAction = new MultiPropertyDialogAction(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart().getSite(), getActionSite().getViewSite().getSelectionProvider());
		menu.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, fPropertiesAction);
	}

	public void fillActionBars(IActionBars actionBars) {
		fOpenActionGroup.setContext(getContext());
		fOpenActionGroup.fillActionBars(actionBars);
	}

}
