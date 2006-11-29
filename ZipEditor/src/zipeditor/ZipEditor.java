/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.dialogs.SaveAsDialog;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.FileEditorInput;

import zipeditor.actions.CollapseAllAction;
import zipeditor.actions.OpenActionGroup;
import zipeditor.actions.SelectAllAction;
import zipeditor.actions.ToggleViewModeAction;
import zipeditor.actions.ZipActionGroup;
import zipeditor.model.IModelListener;
import zipeditor.model.ZipModel;
import zipeditor.model.ZipNode;
import zipeditor.model.ZipNodeProperty;

public class ZipEditor extends EditorPart {
	private class ModelListener implements IModelListener, Runnable {
		private boolean fWaiting;

		public void modelChanged() {
			if (fWaiting)
				return;
			fWaiting = true;
			getSite().getShell().getDisplay().syncExec(new Runnable() {
				public void run() {
					getSite().getShell().getDisplay().timerExec(2000, ModelListener.this);
				}
			});
		}
		
		public void run() {
			if (!fZipViewer.getControl().isDisposed())
				fZipViewer.refresh();
			setDirty(true);
			fWaiting = false;
		}
	};

	private class InputFileListener implements IResourceChangeListener, IResourceDeltaVisitor {
		public void resourceChanged(IResourceChangeEvent event) {
			if (event.getType() == IResourceChangeEvent.POST_CHANGE) {
				IResourceDelta delta = event.getDelta();
				try {
					delta.accept(this);
				} catch (CoreException e) {
					ZipEditorPlugin.log(e);
				}
			}
		}
		public boolean visit(IResourceDelta delta) throws CoreException {
			IResource resource = delta.getResource();
			if (resource instanceof IFile) {
				IFile file = (IFile)resource;
				if (file.equals(((IFileEditorInput)getEditorInput()).getFile())) {
					if (delta.getKind() == IResourceDelta.REMOVED ||
							delta.getKind() == IResourceDelta.REPLACED)
						close();
					return false;
				}
			}
			return true;
		}
	};

	private StructuredViewer fZipViewer;
	private ZipActionGroup fZipActionGroup;
	private OpenActionGroup fOpenActionGroup;
	private boolean fDirty;
	private Map fActions = new HashMap();
	private IResourceChangeListener fInputFileListener;
	private ZipModel fModel;
	private Map fFileToNode = new HashMap();
	private DisposeListener fTableDisposeListener = new DisposeListener() {
		public void widgetDisposed(DisposeEvent e) {
			storeTableColumnPreferences();
		}
	};

	public final static String ACTION_TOGGLE_MODE = "ToggleViewMode"; //$NON-NLS-1$
	public final static String ACTION_COLLAPSE_ALL = "CollapseAll"; //$NON-NLS-1$
	public final static String ACTION_SELECT_ALL = "SelectAll"; //$NON-NLS-1$
	
	public void doSave(IProgressMonitor monitor) {
		IEditorInput input = getEditorInput();
		if (input instanceof IFileEditorInput)
			internalSave(((IFileEditorInput) getEditorInput()).getFile().getLocation(), monitor);
		else if (input instanceof IPathEditorInput)
			internalSave(((IPathEditorInput) input).getPath(), monitor);
		else
			ZipEditorPlugin.log("The input " + input + " cannot be saved"); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	private void internalSave(IPath path, IProgressMonitor monitor) {
		ZipNode root = getRootNode();
		monitor.beginTask(Messages.getString("ZipEditor.3"), 100); //$NON-NLS-1$
		monitor.worked(1);
		int totalWork = Utils.computeTotalNumber(root.getChildren(), monitor);
		SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 99);
		monitor.setTaskName(Messages.getString("ZipEditor.2")); //$NON-NLS-1$
		monitor.subTask(path.lastSegment());
		subMonitor.beginTask(Messages.getString("ZipEditor.2") + path, totalWork); //$NON-NLS-1$
		try {
			File tmpFile = new File(root.getModel().getTempDir(), Integer.toString((int) System.currentTimeMillis()));
			FileOutputStream fileOut = new FileOutputStream(tmpFile); 
			ZipOutputStream out = new ZipOutputStream(fileOut);
			try {
				saveNodes(out, root, subMonitor);
				out.finish();
				fileOut.close();
			} catch (Exception e) {
				ZipEditorPlugin.log(e);
			}
			FileInputStream in = new FileInputStream(tmpFile);
			IFile file = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(path);
			IEditorInput newInput = null;
			if (file != null)
				newInput = internalSaveWorkspaceFile(file, in, subMonitor);
			else
				newInput = internalSaveLocalFile(path.toFile(), in, subMonitor);
	
			in.close();
			setDirty(false);
			setInput(newInput);
			setPartName(newInput.getName());
			doRevert();
		} catch (Exception e) {
			ZipEditorPlugin.log(e);
		} finally {
			subMonitor.done();
			monitor.done();
		}
	}

	private IEditorInput internalSaveWorkspaceFile(IFile file, InputStream in, IProgressMonitor monitor) {
		try {
			if (file.exists())
				file.setContents(in, true, true, monitor);
			else
				file.create(in, true, monitor);
		} catch (CoreException e) {
			ZipEditorPlugin.log(e);
		}
		return new FileEditorInput(file);
	}

	private IEditorInput internalSaveLocalFile(File file, InputStream in, IProgressMonitor monitor) {
		try {
			Utils.readAndWrite(in, new FileOutputStream(file), true);
		} catch (Exception e) {
			ZipEditorPlugin.log(e);
		}
		IFileStore fileStore = EFS.getLocalFileSystem().getStore(new Path(file.getParentFile().getAbsolutePath()));
		fileStore = fileStore.getChild(file.getName());
		return new LocalFileEditorInput(fileStore);
	}

	public void doSaveAs() {
		SaveAsDialog dialog = new SaveAsDialog(getSite().getShell());
		IFile original = (getEditorInput() instanceof IFileEditorInput) ? ((IFileEditorInput) getEditorInput()).getFile() : null;
		if (original != null)
			dialog.setOriginalFile(original);

		dialog.create();
		if (dialog.open() == Window.CANCEL) {
			return;
		}
		final IPath filePath = dialog.getResult();
		if (filePath == null)
			return;

		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				internalSave(filePath, monitor);
			}
		};
		try {
			getSite().getWorkbenchWindow().run(true, true, op);
		} catch (Exception e) {
			ZipEditorPlugin.log(e);
		}
	}

	private void saveNodes(ZipOutputStream out, ZipNode node, IProgressMonitor monitor) throws IOException {
		ZipNode[] children = node.getChildren();
		for (int i = 0; i < children.length; i++) {
			if (monitor.isCanceled())
				break;
			ZipNode child = children[i];
			if (child.isFolder()) {
				saveNodes(out, child, monitor);
				continue;
			}
			ZipEntry entry = new ZipEntry(child.getPath() + child.getName());
			entry.setTime(child.getTime());
			entry.setComment(child.getComment());
			
			out.putNextEntry(entry);
			Utils.readAndWrite(child.getContent(), out, false);
			monitor.worked(1);
		}
	}

	public void doRevert() {
		if (fModel != null)
			fModel.dispose();
		fModel = null;
		setDirty(false);
		setViewerInput(fZipViewer);
	}

	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		setSite(site);
		setInput(input);
		setPartName(input.getName());
		fInputFileListener = new InputFileListener();
		ResourcesPlugin.getWorkspace().addResourceChangeListener(fInputFileListener);
	}
	
	protected void setInput(IEditorInput input) {
        if (input != getEditorInput()) {
            super.setInput(input);
            doFirePropertyChange(PROP_INPUT);
        }
	}
	
	private void doFirePropertyChange(final int property) {
        if (Utils.isUIThread()) {
        	firePropertyChange(property);
        } else {
        	getSite().getShell().getDisplay().asyncExec(new Runnable() {
        		public void run() {
                	firePropertyChange(property);
        		}
        	});
        }
	}

	public void close() {
		Display display = getSite().getShell().getDisplay();
		display.asyncExec(new Runnable() {
			public void run() {
				getSite().getPage().closeEditor(ZipEditor.this, false);
			}
		});
	}
	
	public void dispose() {
		if (fInputFileListener != null) {
			ResourcesPlugin.getWorkspace().removeResourceChangeListener(fInputFileListener);
			fInputFileListener = null;
		}
		fFileToNode.clear();
		fFileToNode = null;
		fActions.clear();
		fActions = null;
		super.dispose();		
	}

	private ZipModel createModel() {
		IEditorInput input = getEditorInput();
		IPath path = null;
		InputStream in = null;
		if (input instanceof IFileEditorInput) {
			IFileEditorInput fileEditorInput = (IFileEditorInput) input;
			path = fileEditorInput.getFile().getLocation();
			try {
				in = fileEditorInput.getFile().getContents();
			} catch (CoreException e) {
				throw new RuntimeException(e);
			}
		} else if (input instanceof IPathEditorInput && input instanceof IStorageEditorInput) {
			path = ((IPathEditorInput) input).getPath();
			try {
				in = ((IStorageEditorInput) input).getStorage().getContents();
			} catch (CoreException e) {
				ZipEditorPlugin.log(e);
			}
		}
		return new ZipModel(path.toFile(), in);
	}
	
	public boolean isDirty() {
		return fDirty;
	}

	public void setDirty(boolean dirty) {
		fDirty = dirty;
		doFirePropertyChange(PROP_DIRTY);
	}

	public boolean isSaveAsAllowed() {
		return true;
	}

	public void createPartControl(Composite parent) {
		createActions();
		Composite control = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		control.setLayout(layout);
		createContent(control, getMode());
		handleViewerSelectionChanged(StructuredSelection.EMPTY);
	}
	
	private void createContent(Composite parent, int mode) {
		createToolBar(parent, mode);
		fZipViewer = createZipViewer(parent, mode);
	}
	
	private ToolBarManager createToolBar(Composite parent, int mode) {
		ToolBarManager bar = new ToolBarManager(SWT.HORIZONTAL | SWT.FLAT);
		bar.add(getAction(ACTION_TOGGLE_MODE));
		bar.add(new Separator());
		fOpenActionGroup.fillToolBarManager(bar);
		fZipActionGroup.fillToolBarManager(bar, mode);
		if (mode == ToggleViewModeAction.MODE_TREE) {
			bar.add(new Separator());
			bar.add(getAction(ACTION_COLLAPSE_ALL));
		}
		Control control = bar.createControl(parent);
		control.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		return bar;
	}
	
	public void updateView(int mode, boolean savePreferences) {
		Composite parent = fZipViewer.getControl().getParent();
		ISelection selection = fZipViewer.getSelection();
		Control[] children = parent.getChildren();
		for (int i = 0; i < children.length; i++) {
			if (!savePreferences)
				children[i].removeDisposeListener(fTableDisposeListener);
			children[i].dispose();
		}
		createContent(parent, mode);
		parent.layout();
		fZipViewer.setSelection(selection);
	}

	private StructuredViewer createZipViewer(Composite parent, int mode) {
		StructuredViewer viewer = null;
		switch (mode) {
		default:
			return null;
		case ToggleViewModeAction.MODE_TREE:
			viewer = new TreeViewer(parent);
			break;
		case ToggleViewModeAction.MODE_FOLDER:
			viewer = createTableViewer(parent);
			break;
		}
		
		viewer.setContentProvider(new ZipContentProvider(mode));
		viewer.setLabelProvider(new ZipLabelProvider());
		viewer.setSorter(new ZipSorter());
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				handleViewerDoubleClick();
			}
		});
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				handleViewerSelectionChanged(event.getSelection());
			}
		});
		viewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
		MenuManager manager = new MenuManager();
		manager.setRemoveAllWhenShown(true);
		manager.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				fOpenActionGroup.setContext(new ActionContext(fZipViewer.getSelection()));
				fOpenActionGroup.fillContextMenu(manager);
				fZipActionGroup.setContext(new ActionContext(fZipViewer.getSelection()));
				fZipActionGroup.fillContextMenu(manager);
			}
		});
		Menu contextMenu = manager.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(contextMenu);

		setViewerInput(viewer);
		initDragAndDrop(viewer);

		return viewer;
	}
	
	private void setViewerInput(StructuredViewer viewer) {
		if (fModel == null) {
			fModel = createModel();
			fModel.addModelListener(new ModelListener());
		}
		viewer.setInput(fModel.getRoot());
	}
	
	private void handleViewerSelectionChanged(ISelection selection) {
		activateActions();
		fZipActionGroup.setContext(new ActionContext(selection));
		fZipActionGroup.updateActionBars();
		fOpenActionGroup.setContext(new ActionContext(selection));
		fOpenActionGroup.updateActionBars();
		IStructuredSelection structuredSelection = (IStructuredSelection) selection;
		int size = structuredSelection.size();
		setStatusText(size == 0 ? null : size == 1 ?
				structuredSelection.getFirstElement() : Messages.getFormattedString("ZipEditor.9", new Integer(size))); //$NON-NLS-1$
	}

	private void setStatusText(Object object) {
		getEditorSite().getActionBars().getStatusLineManager().setMessage(object instanceof ZipNode
				? ((ZipNode) object).getName() : object != null ? object.toString() : null);
	}

	private TableViewer createTableViewer(Composite parent) {
		TableViewer viewer = new TableViewer(parent, SWT.MULTI | SWT.FULL_SELECTION);
		Table table = viewer.getTable();
		table.setHeaderVisible(true);
		createTableColumns(table);
		table.addDisposeListener(fTableDisposeListener);
		return viewer;
	}
	
	private void createTableColumns(Table table) {
		IPreferenceStore store = getPreferenceStore();
		int sortColumn = store.getInt(PreferenceConstants.SORT_BY);
		int sortDirection = store.getInt(PreferenceConstants.SORT_DIRECTION);
		Integer[] visibleColumns = (Integer[]) PreferenceInitializer.split(store.getString(PreferenceConstants.VISIBLE_COLUMNS), PreferenceConstants.COLUMNS_SEPARATOR, Integer.class);
		for (int i = 0; i < visibleColumns.length; i++) {
			int type = visibleColumns[i].intValue();
			createTableColumn(table, Messages.getString("ZipNodeProperty." + type), type, sortColumn, sortDirection); //$NON-NLS-1$
		}
		TableLayout layout = new TableLayout();
		TableColumn[] columns = table.getColumns();
		for (int i = 0; i < columns.length; i++) {
			int width = store.getInt(PreferenceConstants.SORT_COLUMN_WIDTH + columns[i].getData());
			if (width == 0)
				width = 150;
			layout.addColumnData(new ColumnPixelData(width));
		}
		table.setLayout(layout);
	}

	private TableColumn createTableColumn(Table table, String text, int colType, int sortColumn, int sortDirection) {
		int style = colType == ZipNodeProperty.PACKED_SIZE || colType == ZipNodeProperty.SIZE ? SWT.RIGHT : SWT.LEFT;
		TableColumn column = new TableColumn(table, style);
		column.setText(text);
		column.setData(new Integer(colType));
		column.setMoveable(true);
		column.setImage(getSortImage(sortColumn == colType ? sortDirection : SWT.NONE));
		column.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleSortColumnSelected((TableColumn) e.widget);
			}
		});
		return column;
	}
	
	public void storeTableColumnPreferences() {
		if (!(fZipViewer instanceof TableViewer))
			return;
		Table table = ((TableViewer) fZipViewer).getTable();
		IPreferenceStore store = getPreferenceStore();
		TableColumn[] columns = table.getColumns();
		int[] order = table.getColumnOrder();
		for (int i = 0; i < columns.length; i++) {
			store.setValue(PreferenceConstants.SORT_COLUMN_WIDTH + columns[i].getData(), columns[i].getWidth());
		}
		for (int i = 0; i < order.length; i++) {
			order[i] = ((Integer) columns[order[i]].getData()).intValue();
		}
		store.setValue(PreferenceConstants.VISIBLE_COLUMNS, PreferenceInitializer.join(order, PreferenceConstants.COLUMNS_SEPARATOR));
	}

	private void handleSortColumnSelected(TableColumn column) {
		IPreferenceStore store = getPreferenceStore();
		int sortColumn = store.getInt(PreferenceConstants.SORT_BY);
		int sortDirection = store.getInt(PreferenceConstants.SORT_DIRECTION);
		
		if (((Integer) column.getData()).intValue() == sortColumn) {
			sortDirection = sortDirection == SWT.UP ? SWT.DOWN : SWT.UP;
			column.setImage(getSortImage(sortDirection));
		} else {
			sortColumn = ((Integer) column.getData()).intValue();
			column.setImage(getSortImage(sortDirection = SWT.UP));
			TableColumn[] columns = column.getParent().getColumns();
			for (int i = 0; i < columns.length; i++) {
				if (columns[i] != column)
					columns[i].setImage(getSortImage(SWT.NONE));
			}
		}
		store.setValue(PreferenceConstants.SORT_DIRECTION, sortDirection);
		store.setValue(PreferenceConstants.SORT_BY, sortColumn);
		((ZipSorter) fZipViewer.getSorter()).update();
		fZipViewer.refresh();
	}

	private Image getSortImage(int direction) {
		if (!getPreferenceStore().getBoolean(PreferenceConstants.SORT_ENABLED))
			direction = 0;
		switch (direction) {
		default:
			return ZipEditorPlugin.getImage("icons/sort_none.gif"); //$NON-NLS-1$
		case SWT.UP:
			return ZipEditorPlugin.getImage("icons/sort_asc.gif"); //$NON-NLS-1$
		case SWT.DOWN:
			return ZipEditorPlugin.getImage("icons/sort_desc.gif"); //$NON-NLS-1$
		}
	}

	private void createActions() {
		fZipActionGroup = new ZipActionGroup(this);
		fOpenActionGroup = new OpenActionGroup(this);
		setAction(ACTION_TOGGLE_MODE, new ToggleViewModeAction(this));
		setAction(ACTION_COLLAPSE_ALL, new CollapseAllAction(this));
		setAction(ACTION_SELECT_ALL, new SelectAllAction(this));

		activateActions();
	}
	
	private void setAction(String name, IAction action) {
		fActions.put(name, action);
	}
	
	private IAction getAction(String name) {
		return (IAction) fActions.get(name);
	}

	private void initDragAndDrop(StructuredViewer viewer) {
		int ops = DND.DROP_DEFAULT | DND.DROP_COPY;
		Transfer[] transfers = new Transfer[] { FileTransfer.getInstance() };

        viewer.addDragSupport(ops, transfers, new ZipEditorDragAdapter(this));
        viewer.addDropSupport(ops | DND.DROP_DEFAULT, transfers, new ZipEditorDropAdapter(this, viewer));
	}
	
	public IPreferenceStore getPreferenceStore() {
		return ZipEditorPlugin.getDefault().getPreferenceStore();
	}

	private void handleViewerDoubleClick() {
		ZipNode[] nodes = getSelectedNodes();
		if (nodes == null || nodes.length == 0)
			return;
		if (nodes.length == 1 && nodes[0].isFolder() && fZipViewer instanceof TreeViewer) {
			((TreeViewer) fZipViewer).setExpandedState(nodes[0], !((TreeViewer) fZipViewer).getExpandedState(nodes[0]));
			return;
		}
		Utils.openFilesFromNodes(this, nodes);
	}

	public ZipNode[] getSelectedNodes() {
		IStructuredSelection selection = (IStructuredSelection) fZipViewer.getSelection();
		Object[] objects = selection.toArray();
		ZipNode[] nodes = new ZipNode[objects.length];
		System.arraycopy(objects, 0, nodes, 0, objects.length);
		return nodes;
	}

	public void setFocus() {
		if (fZipViewer != null)
			fZipViewer.getControl().setFocus();
		checkFilesForModification();
		activateActions();
	}

	private void activateActions() {
		getEditorSite().getActionBars().setGlobalActionHandler(ActionFactory.SELECT_ALL.getId(), getAction(ACTION_SELECT_ALL));
		fZipActionGroup.fillActionBars(getEditorSite().getActionBars());
	}

	public void addFileMonitor(File file, ZipNode node) {
		fFileToNode.put(file, new Object[] { node, new Long(System.currentTimeMillis()) });
	}

	private void checkFilesForModification() {
		for (Iterator it = fFileToNode.keySet().iterator(); it.hasNext(); ) {
			File file = (File) it.next();
			Object[] value = (Object[]) fFileToNode.get(file);
			long creationTime = ((Long) value[1]).longValue();
			if (file.lastModified() > creationTime) {
				indicateModification(file, (ZipNode) value[0]);
				value[1] = new Long(file.lastModified());
			}
		}
	}

	private void indicateModification(File file, ZipNode node) {
		if (MessageDialog.openQuestion(getSite().getShell(),
				Messages.getString("ZipEditor.1"), Messages.getFormattedString("ZipEditor.0", //$NON-NLS-1$ //$NON-NLS-2$
						new Object[] { file.getName(), node.getModel().getZipPath().getName() }))) {
			node.updateContent(file);
			setDirty(true);
			fZipViewer.refresh();
		}
	}

	public StructuredViewer getViewer() {
		return fZipViewer;
	}
	
	public ZipNode getRootNode() {
		return (ZipNode) fZipViewer.getInput();
	}

	public int getMode() {
		return ((ToggleViewModeAction) getAction(ACTION_TOGGLE_MODE)).getMode();
	}

}
