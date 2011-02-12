/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.actions;


import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionGroup;

import zipeditor.PreferenceConstants;
import zipeditor.ZipEditor;

public class ZipActionGroup extends ActionGroup {
	private IAction fAddAction;
	private IAction fCopyAction;
	private IAction fCopyQualifiedAction;
	private IAction fDeleteAction;
	private IAction fExtractAction;
	private IAction fSortAction;
	private IAction fSaveAction;
	private IAction fRevertAction;
	private IAction fPreferencesAction;
	private IAction fPropertiesAction;
	private ZipEditor fEditor;
	private Clipboard fClipboard;
	
	public ZipActionGroup(ZipEditor editor) {
		fRevertAction = new RevertAction(editor);
		fRevertAction.setActionDefinitionId(IWorkbenchCommandConstants.FILE_REVERT);
		fSaveAction = new SaveAction(editor);
		fSaveAction.setActionDefinitionId(IWorkbenchCommandConstants.FILE_SAVE);
		fPreferencesAction = new PreferencesAction(editor);

		fEditor = editor;
		fClipboard = new Clipboard(fEditor.getViewer().getControl().getDisplay());
	}
	
	public void fillContextMenu(IMenuManager menu) {
		lazilyCreateActions();
		menu.add(new Separator());
		menu.add(fCopyAction);
		menu.add(fCopyQualifiedAction);
		menu.add(fDeleteAction);
		menu.add(new Separator());
		menu.add(fSaveAction);
		menu.add(fRevertAction);
		menu.add(new Separator());
		menu.add(fAddAction);
		menu.add(fExtractAction);
		menu.add(new Separator());
		menu.add(fPropertiesAction);
		
		updateActionBars();
	}
	
    private void lazilyCreateActions() {
		if (fPropertiesAction == null) {
			fPropertiesAction = new MultiPropertyDialogAction(fEditor.getSite(), fEditor.getViewer());
			fPropertiesAction.setActionDefinitionId(IWorkbenchCommandConstants.FILE_PROPERTIES);
		}
		if (fAddAction == null)
			fAddAction = new AddAction(fEditor.getViewer());
		if (fExtractAction == null)
			fExtractAction = new ExtractAction(fEditor.getViewer());
		if (fCopyAction == null) {
			fCopyAction = new CopyAction(fEditor.getViewer(), false, fClipboard);
			fCopyAction.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_COPY);
		}
		if (fCopyQualifiedAction == null)
			fCopyQualifiedAction = new CopyAction(fEditor.getViewer(), true, fClipboard);
		if (fDeleteAction == null) {
			fDeleteAction = new DeleteAction(fEditor.getViewer());
			fDeleteAction.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_DELETE);
		}
		if (fSortAction == null)
			fSortAction = new SortAction(fEditor.getViewer(), PreferenceConstants.PREFIX_EDITOR);
	}

	public void fillToolBarManager(IToolBarManager manager, int mode) {
		lazilyCreateActions();
		manager.add(new Separator());
		manager.add(fAddAction);
		manager.add(fExtractAction);
		manager.add(new Separator());
		manager.add(fDeleteAction);
		manager.add(new Separator());
		manager.add(fSortAction);
		if ((mode & PreferenceConstants.VIEW_MODE_TREE) == 0) {
			manager.add(new Separator());
			manager.add(fPreferencesAction);
		}
	}
    
    public void fillActionBars(IActionBars actionBars) {
		lazilyCreateActions();

		actionBars.setGlobalActionHandler(ActionFactory.COPY.getId(), fCopyAction);
		actionBars.setGlobalActionHandler(ActionFactory.DELETE.getId(), fDeleteAction);
		actionBars.setGlobalActionHandler(ActionFactory.SAVE.getId(), fSaveAction);
		actionBars.setGlobalActionHandler(ActionFactory.REVERT.getId(), fRevertAction);
		actionBars.setGlobalActionHandler(ActionFactory.PROPERTIES.getId(), fPropertiesAction);
		updateActionBars();
    }
	
	public void updateActionBars() {
		if (getContext() == null)
			return;
		IStructuredSelection selection = (IStructuredSelection) getContext().getSelection();
		boolean empty = selection.isEmpty();
		
		lazilyCreateActions();
		fCopyAction.setEnabled(!empty);
		fCopyQualifiedAction.setEnabled(!empty);
		fDeleteAction.setEnabled(!empty);
		fSaveAction.setEnabled(fEditor.isDirty());
		fRevertAction.setEnabled(fEditor.isDirty());
		fPropertiesAction.setEnabled(!empty);
	}
	
	public void dispose() {
		super.dispose();
		fClipboard.dispose();
	}
}
