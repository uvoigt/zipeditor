/*
 * (c) Copyright 2006 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.actions;

import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;

public class ZipActionProvider extends CommonActionProvider {
	private OpenActionGroup fOpenActionGroup;
	private ViewerAction fExtractAction;
	private IAction fPropertiesAction;
	
	public void init(ICommonActionExtensionSite aSite) {
		super.init(aSite);
		fOpenActionGroup = new OpenActionGroup(aSite.getStructuredViewer(), true);
		fExtractAction = new ExtractAction(aSite.getStructuredViewer());
	}
	
	public void dispose() {
		super.dispose();
		fOpenActionGroup.dispose();
	}

	public void fillContextMenu(IMenuManager menu) {
		fOpenActionGroup.setContext(getContext());
		fOpenActionGroup.fillContextMenu(menu);
		menu.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, new Separator());
		menu.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, fExtractAction);
		menu.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, new Separator());
		if (fPropertiesAction == null)
			fPropertiesAction = new MultiPropertyDialogAction(PlatformUI.getWorkbench().getActiveWorkbenchWindow(), getActionSite().getViewSite().getSelectionProvider());
		if (!hasProperties(menu))
			menu.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, fPropertiesAction);
	}

	private boolean hasProperties(IMenuManager menu) {
		IContributionItem[] items = menu.getItems();
		for (int i = 0; i < items.length; i++) {
			if (items[i] instanceof ActionContributionItem && ActionFactory.PROPERTIES.getCommandId().equals(((ActionContributionItem) items[i]).getAction().getActionDefinitionId()))
				return true;
		}
		return false;
	}

	public void fillActionBars(IActionBars actionBars) {
		fOpenActionGroup.setContext(getContext());
		fOpenActionGroup.fillActionBars(actionBars);
	}

}
