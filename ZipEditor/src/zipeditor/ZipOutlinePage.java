/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;

import zipeditor.ZipEditor.NodeComparer;
import zipeditor.actions.AddAction;
import zipeditor.actions.CollapseAllAction;
import zipeditor.actions.DeleteAction;
import zipeditor.actions.ExtractAction;
import zipeditor.actions.MultiPropertyDialogAction;
import zipeditor.actions.NewFolderAction;
import zipeditor.actions.RenameNodeAction;
import zipeditor.actions.SelectAllAction;
import zipeditor.actions.SortAction;
import zipeditor.actions.ViewerAction;
import zipeditor.model.Node;

public class ZipOutlinePage extends ContentOutlinePage {
	private class LinkAction extends Action  {
		public LinkAction() {
			super(Messages.getString("ZipOutlinePage.0")); //$NON-NLS-1$
			setToolTipText(Messages.getString("ZipOutlinePage.1")); //$NON-NLS-1$
			setImageDescriptor(ZipEditorPlugin.getImageDescriptor("icons/synced.gif")); //$NON-NLS-1$
			setChecked(ZipEditorPlugin.getDefault().getPreferenceStore().getBoolean(PREFERENCE_LINKED));
		}
		
		public void run() {
			boolean checked = isChecked();
			ZipEditorPlugin.getDefault().getPreferenceStore().setValue(PREFERENCE_LINKED, checked);
			IEditorPart editor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
			if (editor instanceof ZipEditor) {
				Node[] nodes = ((ZipEditor) editor).getSelectedNodes();
				if (nodes.length == 1)
				setSelection(new StructuredSelection(nodes[0]));
			}
		}
	};

	private final static String PREFERENCE_LINKED = "outline_linked"; //$NON-NLS-1$

	private AddAction fAddAction;
	private ViewerAction fExtractAction;
	private ViewerAction fDeleteAction;
	private IAction fPropertiesAction;
	private SelectAllAction fSelectAllAction;
	private NewFolderAction fNewFolderAction;
	private RenameNodeAction fRenameNodeAction;

	public void createControl(Composite parent) {
		super.createControl(parent);
		getTreeViewer().setContentProvider(new ZipContentProvider(PreferenceConstants.VIEW_MODE_TREE));
		getTreeViewer().setLabelProvider(new ZipLabelProvider());
		getTreeViewer().setSorter(new ZipSorter(PreferenceConstants.PREFIX_OUTLINE));
		getTreeViewer().setComparer(new NodeComparer());

		createActions();
		MenuManager manager = new MenuManager();
		manager.setRemoveAllWhenShown(true);
		manager.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				contextMenuAboutToShow(manager);
			}
		});
		Menu contextMenu = manager.createContextMenu(getTreeViewer().getControl());
		getTreeViewer().getControl().setMenu(contextMenu);

		initDragAndDrop(getTreeViewer());
	}

	private void createActions() {
		fAddAction = new AddAction(getTreeViewer());
		fExtractAction = new ExtractAction(getTreeViewer());
		fDeleteAction = new DeleteAction(getTreeViewer());
		fSelectAllAction = new SelectAllAction(getTreeViewer());
		fPropertiesAction = new MultiPropertyDialogAction(getSite(), getTreeViewer());
		fNewFolderAction = new NewFolderAction(getTreeViewer());
		fRenameNodeAction = new RenameNodeAction(getTreeViewer());

		updateActions();
	}

	private void updateActions() {
		boolean empty = getSelection().isEmpty();
		fDeleteAction.setEnabled(!empty);
		fPropertiesAction.setEnabled(!empty);
		getSite().getActionBars().setGlobalActionHandler(ActionFactory.DELETE.getId(), fDeleteAction);
		getSite().getActionBars().setGlobalActionHandler(ActionFactory.SELECT_ALL.getId(), fSelectAllAction);
		getSite().getActionBars().setGlobalActionHandler(ActionFactory.PROPERTIES.getId(), fPropertiesAction);
		getSite().getActionBars().setGlobalActionHandler(ActionFactory.RENAME.getId(), fRenameNodeAction);
	}

	private void initDragAndDrop(StructuredViewer viewer) {
		int ops = DND.DROP_DEFAULT | DND.DROP_COPY;
		Transfer[] transfers = new Transfer[] { FileTransfer.getInstance() };

        viewer.addDragSupport(ops, transfers, new ZipEditorDragAdapter(this));
        viewer.addDropSupport(ops | DND.DROP_DEFAULT, transfers, new ZipEditorDropAdapter(viewer));
	}
	
	private void contextMenuAboutToShow(IContributionManager manager) {
		manager.add(fNewFolderAction);
		manager.add(new Separator());
		manager.add(fAddAction);
		manager.add(fExtractAction);
		manager.add(new Separator());
		manager.add(fDeleteAction);
		manager.add(new Separator());
		manager.add(fPropertiesAction);
	}

	public void makeContributions(IMenuManager menuManager, IToolBarManager toolBarManager,
			IStatusLineManager statusLineManager) {
		toolBarManager.add(new SortAction(getTreeViewer(), PreferenceConstants.PREFIX_OUTLINE));
		toolBarManager.add(new Separator());
		toolBarManager.add(new CollapseAllAction(getTreeViewer()));
		toolBarManager.add(new LinkAction());
	}
	
	public void selectionChanged(SelectionChangedEvent event) {
		super.selectionChanged(event);
		updateActions();
	}

	public void setFocus() {
		super.setFocus();
		updateActions();
	}

	public void setInput(final Node node) {
		if (getTreeViewer() == null || getTreeViewer().getControl().isDisposed())
			return;
		if (Utils.isUIThread())
			getTreeViewer().setInput(node);
		else {
			getTreeViewer().getControl().getDisplay().syncExec(new Runnable() {
				public void run() {
					getTreeViewer().setInput(node);
				}
			});
		}
	}

	public Object getInput() {
		return getTreeViewer().getInput();
	}

	public void refresh() {
		if (getTreeViewer() == null || getTreeViewer().getControl().isDisposed())
			return;
		getTreeViewer().getControl().setRedraw(false);
		getTreeViewer().refresh();		
		getTreeViewer().getControl().setRedraw(true);
	}

	public boolean isLinkingEnabled() {
		return ZipEditorPlugin.getDefault().getPreferenceStore().getBoolean(PREFERENCE_LINKED);
	}

}
