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
import java.util.Map;

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
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IElementComparer;
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
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
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
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.dialogs.SaveAsDialog;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

import zipeditor.actions.CollapseAllAction;
import zipeditor.actions.NewFolderAction;
import zipeditor.actions.OpenActionGroup;
import zipeditor.actions.SelectAllAction;
import zipeditor.actions.SortAction;
import zipeditor.actions.ToggleViewModeAction;
import zipeditor.actions.ZipActionGroup;
import zipeditor.model.IModelListener;
import zipeditor.model.Node;
import zipeditor.model.NodeProperty;
import zipeditor.model.ZipModel;
import zipeditor.model.ZipNodeProperty;

public class ZipEditor extends EditorPart implements IPropertyChangeListener {
	static class NodeComparer implements IElementComparer {
		public boolean equals(Object a, Object b) {
			return a instanceof Node && b instanceof Node ? ((Node) a).getFullPath().equals(((Node) b).getFullPath()) : a
					.equals(b);
		}

		public int hashCode(Object element) {
			return ((Node) element).getFullPath().hashCode();
		}
	};

	private class ModelListener extends UIJob implements IModelListener {
		public ModelListener() {
			super(Messages.getString("ZipEditor.11")); //$NON-NLS-1$
		}

		private ModelChangeEvent fModelChangeEvent;
		private String fOriginalPartName;
		private Object jobFamily = new Object();

		public void modelChanged(ModelChangeEvent event) {
			if (fModelChangeEvent != null && !event.isInitFinished())
				return;
			fModelChangeEvent = event;
			long scheduleTime = 0;
			if (event.isInitializing() && !event.isInitStarted() && !event.isInitFinished())
				scheduleTime = 2000;
			if (event.isInitFinished()) {
				Job[] jobs = Platform.getJobManager().find(jobFamily);
				if (jobs != null) {
					for (int i = 0; i < jobs.length; i++) {
						jobs[i].cancel();
					}
				}
			}
			setPriority(Job.INTERACTIVE);
			schedule(scheduleTime);
		}
		
		public boolean belongsTo(Object family) {
			return family == jobFamily;
		}
		
		public IStatus runInUIThread(IProgressMonitor monitor) {
			if (!monitor.isCanceled())
				doRun();
			fModelChangeEvent = null;
			return Status.OK_STATUS;
		}

		private void doRun() {
			if (!fZipViewer.getControl().isDisposed()) {
				fZipViewer.getControl().setRedraw(false);
				fZipViewer.refresh();
				fZipViewer.getControl().setRedraw(true);
			}
			if (fOutlinePage != null && !fOutlinePage.getControl().isDisposed()) {
				if (fOutlinePage.getInput() == null)
					fOutlinePage.setInput(fModel.getRoot());
				else
					fOutlinePage.refresh();
			}
			firePropertyChange(PROP_DIRTY);
			if (fModelChangeEvent != null && fModelChangeEvent.isInitializing()) {
				String suffix = Messages.getString("ZipEditor.10"); //$NON-NLS-1$
				if (!getPartName().endsWith(suffix)) {
					fOriginalPartName = getPartName();
					setPartName(fOriginalPartName + suffix);
				}							
			} else if (fOriginalPartName != null) {
				setPartName(fOriginalPartName);
				fOriginalPartName = null;
			}
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
				if (getEditorInput() instanceof IFileEditorInput && file.equals(
						((IFileEditorInput) getEditorInput()).getFile())) {
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
	private IToolBarManager fToolBar;
	private ZipActionGroup fZipActionGroup;
	private OpenActionGroup fOpenActionGroup;
	private Map fActions = new HashMap();
	private IResourceChangeListener fInputFileListener;
	private ZipModel fModel;
	private ZipOutlinePage fOutlinePage;
	private ISelectionChangedListener fOutlineSelectionChangedListener;
	private DisposeListener fTableDisposeListener = new DisposeListener() {
		public void widgetDisposed(DisposeEvent e) {
			storeTableColumnPreferences();
		}
	};

	public final static String ACTION_TOGGLE_MODE = "ToggleViewMode"; //$NON-NLS-1$
	public final static String ACTION_COLLAPSE_ALL = "CollapseAll"; //$NON-NLS-1$
	public final static String ACTION_SELECT_ALL = "SelectAll"; //$NON-NLS-1$
	public final static String ACTION_NEW_FOLDER = "NewFolder"; //$NON-NLS-1$
	
	public void doSave(IProgressMonitor monitor) {
		IEditorInput input = getEditorInput();
		if (input instanceof IFileEditorInput)
			internalSave(((IFileEditorInput) getEditorInput()).getFile().getLocation(), monitor);
		else if (input instanceof IPathEditorInput)
			internalSave(((IPathEditorInput) input).getPath(), monitor);
		else
			ZipEditorPlugin.log("The input " + input + " cannot be saved"); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	private void internalSave(IPath locationPath, IProgressMonitor monitor) {
		Node root = getRootNode();
		monitor.beginTask(Messages.getString("ZipEditor.3"), 100); //$NON-NLS-1$
		monitor.worked(1);
		int totalWork = Utils.computeTotalNumber(root.getChildren(), monitor);
		SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 99);
		monitor.setTaskName(Messages.getString("ZipEditor.2")); //$NON-NLS-1$
		monitor.subTask(locationPath.lastSegment());
		subMonitor.beginTask(Messages.getString("ZipEditor.2") + locationPath, totalWork); //$NON-NLS-1$
		InputStream in = null;
		try {
			in = root.getModel().save(ZipModel.typeFromName(locationPath.lastSegment()), subMonitor);
			IFile file = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(locationPath);
			IEditorInput newInput = null;
			if (file != null) {
				newInput = internalSaveWorkspaceFile(file, in, monitor);
			} else {
				newInput = internalSaveLocalFile(locationPath.toFile(), in);
			}
			doFirePropertyChange(PROP_DIRTY);
			setInput(newInput);
			if (Utils.isUIThread())
				setPartName(newInput.getName());
			else {
				final String newName = newInput.getName();
				getSite().getShell().getDisplay().syncExec(new Runnable() {
					public void run() {
						setPartName(newName);
					}
				});
			}
			doRevert();
		} catch (Exception e) {
			ZipEditorPlugin.log(e);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException ignore) {
				}
			}
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

	private IEditorInput internalSaveLocalFile(File file, InputStream in) {
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
				internalSave(ResourcesPlugin.getWorkspace().getRoot().getFile(filePath).getLocation(), monitor);
			}
		};
		try {
			getSite().getWorkbenchWindow().run(true, true, op);
		} catch (Exception e) {
			ZipEditorPlugin.log(e);
		}
	}

	public void doRevert() {
		if (fModel != null)
			fModel.dispose();
		fModel = null;
		doFirePropertyChange(PROP_DIRTY);
		setViewerInput(fZipViewer);
	}

	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
		setPartName(input.getName());
		fInputFileListener = new InputFileListener();
		ResourcesPlugin.getWorkspace().addResourceChangeListener(fInputFileListener);
		getPreferenceStore().addPropertyChangeListener(this);
	}
	
	protected void setInput(IEditorInput input) {
        if (input != getEditorInput()) {
            super.setInput(input);
            doFirePropertyChange(PROP_INPUT);
    		if (fOutlinePage != null)
    			fOutlinePage.setInput(fModel.getRoot());
        }
	}
	
	private void doFirePropertyChange(final int property) {
        if (Utils.isUIThread()) {
        	firePropertyChange(property);
        } else {
        	getSite().getShell().getDisplay().syncExec(new Runnable() {
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
		if (fOutlinePage != null) {
			fOutlinePage.removeSelectionChangedListener(fOutlineSelectionChangedListener);
			fOutlinePage.dispose();
		}
		getPreferenceStore().removePropertyChangeListener(this);
		fActions.clear();
		fActions = null;
		fOpenActionGroup.dispose();
		fZipActionGroup.dispose();
		super.dispose();		
	}

	private ZipModel createModel() {
		IEditorInput input = getEditorInput();
		IPath path = null;
		InputStream in = null;
		File file = null;
		if (input instanceof IFileEditorInput) {
			IFileEditorInput fileEditorInput = (IFileEditorInput) input;
			path = fileEditorInput.getFile().getLocation();
			file = path.toFile();
			try {
				in = fileEditorInput.getFile().getContents();
			} catch (CoreException e) {
				throw new RuntimeException(e);
			}
		} else {
			if (input instanceof IPathEditorInput) {
				path = ((IPathEditorInput) input).getPath();
				file = path.toFile();
			}
			if (input instanceof IStorageEditorInput) {
				try {
					if (path == null) {
						path = ((IStorageEditorInput) input).getStorage().getFullPath();
						file = path.toFile();
					}
					in = ((IStorageEditorInput) input).getStorage().getContents();
				} catch (CoreException e) {
					ZipEditorPlugin.log(e);
				}
			}
			if (input instanceof IURIEditorInput) {
				try {
					file = new File(((IURIEditorInput) input).getURI());
					path = new Path(file.getAbsolutePath());
					in = (((IURIEditorInput) input).getURI().toURL()).openStream();
				} catch (Exception e) {
					ZipEditorPlugin.log(e);
				}
			}
			if (in != null && file != null && !file.exists()) {
				try {
					file = File.createTempFile("tmp", null); //$NON-NLS-1$
					file.deleteOnExit();
					Utils.readAndWrite(in, new FileOutputStream(file), true);
					in = new FileInputStream(file);
				} catch (IOException e) {
					ZipEditorPlugin.log(e);
				}
			}
		}
		return new ZipModel(file, in);
	}
	
	
	
	public boolean isDirty() {
		return fModel != null && fModel.isDirty();
	}

	public boolean isSaveAsAllowed() {
		return true;
	}

	public void createPartControl(Composite parent) {
		Composite control = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		control.setLayout(layout);
		createContent(control, getMode());
		handleViewerSelectionChanged(StructuredSelection.EMPTY);
	}
	
	private void createContent(Composite parent, int mode) {
		createControls(parent, mode);
		createActions();
		fillToolBar(fToolBar, mode);
	}
	
	private void createControls(Composite parent, int mode) {
		fToolBar = createToolBar(parent);
		fZipViewer = createZipViewer(parent, mode);
	}
	
	private ToolBarManager createToolBar(Composite parent) {
		ToolBarManager bar = new ToolBarManager(SWT.HORIZONTAL | SWT.FLAT);
		Control control = bar.createControl(parent);
		control.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		return bar;
	}
	
	private void fillToolBar(IToolBarManager bar, int mode) {
		bar.add(getAction(ACTION_TOGGLE_MODE));
		fOpenActionGroup.fillToolBarManager(bar);
		fZipActionGroup.fillToolBarManager(bar, mode);
		bar.add(new Separator());
		if (mode == PreferenceConstants.VIEW_MODE_TREE) {
			bar.add(new Separator());
			bar.add(getAction(ACTION_COLLAPSE_ALL));
		}
		bar.update(false);
	}

	public void updateView(int mode, boolean savePreferences) {
		Composite parent = fZipViewer.getControl().getParent();
		ISelection selection = fZipViewer.getSelection();
		((ZipContentProvider) fZipViewer.getContentProvider()).disposeModel(false);
		Control[] children = parent.getChildren();
		for (int i = 0; i < children.length; i++) {
			if (!savePreferences)
				children[i].removeDisposeListener(fTableDisposeListener);
			children[i].dispose();
		}
		createContent(parent, mode);
		parent.layout();
		fZipViewer.setSelection(selection);
		fZipViewer.getControl().setFocus();
		((ZipContentProvider) fZipViewer.getContentProvider()).disposeModel(true);
	}
	
	private StructuredViewer createZipViewer(Composite parent, int mode) {
		StructuredViewer viewer = null;
		switch (mode) {
		default:
			return null;
		case PreferenceConstants.VIEW_MODE_TREE:
			viewer = new TreeViewer(parent);
			break;
		case PreferenceConstants.VIEW_MODE_FOLDER:
			viewer = createTableViewer(parent);
			break;
		}
		
		viewer.setContentProvider(new ZipContentProvider(mode));
		viewer.setLabelProvider(new ZipLabelProvider());
		viewer.setSorter(new ZipSorter(PreferenceConstants.PREFIX_EDITOR));
		viewer.setComparer(new NodeComparer());
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
				manager.add(getAction(ACTION_NEW_FOLDER));
				manager.add(new Separator());
				fOpenActionGroup.setContext(new ActionContext(fZipViewer.getSelection()));
				fOpenActionGroup.fillContextMenu(manager);
				fZipActionGroup.setContext(new ActionContext(fZipViewer.getSelection()));
				fZipActionGroup.fillContextMenu(manager);
			}
		});
		Menu contextMenu = manager.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(contextMenu);
		
		viewer.getControl().addFocusListener(new FocusAdapter() {
			public void focusGained(FocusEvent e) {
				ZipEditorPlugin.getDefault().checkFilesForModification(fModel);
				firePropertyChange(PROP_DIRTY);
			}
		});

		setViewerInput(viewer);
		initDragAndDrop(viewer);

		return viewer;
	}
	
	private void setViewerInput(final StructuredViewer viewer) {
		if (fModel == null) {
			fModel = createModel();
			fModel.addModelListener(new ModelListener());
		}
		if (fModel.getRoot() != null) {
			if (Utils.isUIThread())
				viewer.setInput(fModel.getRoot());
			else {
				viewer.getControl().getDisplay().syncExec(new Runnable() {
					public void run() {
						viewer.setInput(fModel.getRoot());
					}
				});
			}
			if (fOutlinePage != null)
				fOutlinePage.setInput(fModel.getRoot());
		} else {
			if (Utils.isUIThread())
				setViewerInputAgain(viewer);
			else {
				viewer.getControl().getDisplay().syncExec(new Runnable() {
					public void run() {
						setViewerInputAgain(viewer);
					}
				});
			}
		}
	}
	
	private void setViewerInputAgain(final StructuredViewer viewer){
		viewer.getControl().getDisplay().timerExec(100, new Runnable() {
			public void run() {
				setViewerInput(viewer);
			}
		});
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
		if (size == 1)
			synchronizeOutlineSelection(getSelectedNodes()[0]);
	}

	private void handleOutlineSelectionChanged() {
		IStructuredSelection selection = (IStructuredSelection) fOutlinePage.getSelection();
		if (selection.size() != 1)
			return;
		fZipViewer.setSelection(selection);
	}

	private void synchronizeOutlineSelection(Node node) {
		if (node == null || fOutlinePage == null || !fOutlinePage.isLinkingEnabled())
			return;
		fOutlinePage.removeSelectionChangedListener(fOutlineSelectionChangedListener);
		fOutlinePage.setSelection(new StructuredSelection(node));
		fOutlinePage.addSelectionChangedListener(fOutlineSelectionChangedListener);
	}

	private void setStatusText(Object object) {
		IWorkbenchPartSite activeSite = getSite().getPage().getActivePart() != null ?
				getSite().getPage().getActivePart().getSite() : getEditorSite();
		IActionBars actionBars = activeSite instanceof IEditorSite ? ((IEditorSite) activeSite).getActionBars()
				: activeSite instanceof IViewSite ? ((IViewSite) activeSite).getActionBars() : null;
		if (actionBars != null)
			actionBars.getStatusLineManager().setMessage(object instanceof Node
				? ((Node) object).getName() : object != null ? object.toString() : null);
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
		int style = colType == ZipNodeProperty.PACKED_SIZE || colType == NodeProperty.SIZE ? SWT.RIGHT : SWT.LEFT;
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
		if (!getPreferenceStore().getBoolean(PreferenceConstants.PREFIX_EDITOR + PreferenceConstants.SORT_ENABLED))
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
		fOpenActionGroup = new OpenActionGroup(this, null);
		setAction(ACTION_NEW_FOLDER, new NewFolderAction(this));
		setAction(ACTION_TOGGLE_MODE, new ToggleViewModeAction(this, PreferenceConstants.PREFIX_EDITOR));
		setAction(ACTION_COLLAPSE_ALL, new CollapseAllAction(getViewer()));
		setAction(ACTION_SELECT_ALL, new SelectAllAction(getViewer()));

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

        viewer.addDragSupport(ops, transfers, new ZipEditorDragAdapter(viewer));
        viewer.addDropSupport(ops | DND.DROP_DEFAULT, transfers, new ZipEditorDropAdapter(viewer));
	}
	
	public IPreferenceStore getPreferenceStore() {
		return ZipEditorPlugin.getDefault().getPreferenceStore();
	}
	
	public Object getAdapter(Class adapter) {
		if (IContentOutlinePage.class.equals(adapter)) {
			if (fOutlinePage == null) {
				fOutlinePage = new ZipOutlinePage();
				if (fOutlineSelectionChangedListener == null)
					fOutlineSelectionChangedListener = new ISelectionChangedListener() {
						public void selectionChanged(SelectionChangedEvent event) {
							handleOutlineSelectionChanged();
						}
				};
				fOutlinePage.addSelectionChangedListener(fOutlineSelectionChangedListener);
				getEditorSite().getShell().getDisplay().asyncExec(new Runnable() {
					public void run() {
						fOutlinePage.setInput(fModel.getRoot());
					}
				});
			}
			return fOutlinePage;
		}
		return super.getAdapter(adapter);
	}

	private void handleViewerDoubleClick() {
		Node[] nodes = getSelectedNodes();
		if (nodes == null || nodes.length == 0)
			return;
		if (nodes.length == 1 && nodes[0].isFolder() && fZipViewer instanceof TreeViewer) {
			((TreeViewer) fZipViewer).setExpandedState(nodes[0], !((TreeViewer) fZipViewer).getExpandedState(nodes[0]));
			return;
		}
		Utils.openFilesFromNodes(nodes);
	}

	public Node[] getSelectedNodes() {
		return Utils.getSelectedNodes(fZipViewer.getSelection());
	}

	public void setFocus() {
		if (fZipViewer != null)
			fZipViewer.getControl().setFocus();
		activateActions();
	}

	private void activateActions() {
		getEditorSite().getActionBars().setGlobalActionHandler(ActionFactory.SELECT_ALL.getId(), getAction(ACTION_SELECT_ALL));
		fZipActionGroup.setContext(new ActionContext(fZipViewer.getSelection()));
		fZipActionGroup.fillActionBars(getEditorSite().getActionBars());
		getEditorSite().getActionBars().updateActionBars();
	}

	public StructuredViewer getViewer() {
		return fZipViewer;
	}
	
	public Node getRootNode() {
		return (Node) fZipViewer.getInput();
	}

	public int getMode() {
		return getPreferenceStore().getInt(PreferenceConstants.PREFIX_EDITOR + PreferenceConstants.VIEW_MODE);
	}
	
	public void propertyChange(PropertyChangeEvent event) {
		if ((PreferenceConstants.PREFIX_EDITOR + PreferenceConstants.SORT_ENABLED + SortAction.SORTING_CHANGED).equals(event.getProperty()))
			updateView(getMode(), true);
	}
}
