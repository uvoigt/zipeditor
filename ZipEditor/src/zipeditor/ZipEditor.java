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
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.AbstractInformationControlManager;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
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
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IKeyBindingService;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.dialogs.SaveAsDialog;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.views.framelist.BackAction;
import org.eclipse.ui.views.framelist.ForwardAction;
import org.eclipse.ui.views.framelist.FrameList;
import org.eclipse.ui.views.framelist.GoIntoAction;
import org.eclipse.ui.views.framelist.UpAction;

import zipeditor.actions.ActionMessages;
import zipeditor.actions.CollapseAllAction;
import zipeditor.actions.NewFolderAction;
import zipeditor.actions.OpenActionGroup;
import zipeditor.actions.OutlineInformationControl;
import zipeditor.actions.QuickOutlineAction;
import zipeditor.actions.RenameNodeAction;
import zipeditor.actions.ReverseSelectionAction;
import zipeditor.actions.SelectAllAction;
import zipeditor.actions.SelectPatternAction;
import zipeditor.actions.SortAction;
import zipeditor.actions.ToggleViewModeAction;
import zipeditor.actions.ZipActionGroup;
import zipeditor.model.IModelListener;
import zipeditor.model.Node;
import zipeditor.model.NodeProperty;
import zipeditor.model.ZipContentDescriber;
import zipeditor.model.ZipContentDescriber.ContentTypeId;
import zipeditor.model.ZipEditorZstdException;
import zipeditor.model.ZipModel;
import zipeditor.model.ZipModel.IErrorReporter;
import zipeditor.model.ZipNodeProperty;

public class ZipEditor extends EditorPart implements IPropertyChangeListener, IErrorReporter
		/*,IPersistableEditor*/ {
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
				Job[] jobs = Job.getJobManager().find(jobFamily);
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
			activateActions();
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
				IEditorInput input = doGetEditorInput();
				if (input instanceof IFileEditorInput && file.equals(
						((IFileEditorInput) input).getFile())) {
					if (delta.getKind() == IResourceDelta.REMOVED ||
							delta.getKind() == IResourceDelta.REPLACED)
						close();
					return false;
				}
			}
			return true;
		}
	};
	
	private class DelegateEditorInput implements IEditorInput {
	    private IEditorInput delegate;

		private DelegateEditorInput(IEditorInput delegate) {
	    	this.delegate = delegate;
		}

		public boolean exists() {
	        return delegate.exists();
	    }

	    public ImageDescriptor getImageDescriptor() {
	        return delegate.getImageDescriptor();
	    }

	    public String getName() {
	    	return delegate.getName();
	    }

	    public IPersistableElement getPersistable() {
	        return delegate.getPersistable();
	    }

	    public String getToolTipText() {
	    	return delegate.getToolTipText();
	    }

	    public Object getAdapter(Class adapter) {
	        return null;
	    }
	}

	private class InformationPresenter extends AbstractInformationControlManager {
		private class Closer implements IInformationControlCloser, Listener {
			private Control fSubjectControl;

			public void setSubjectControl(Control subject) {
				fSubjectControl = subject;
			}

			public void setInformationControl(IInformationControl control) {
			}

			public void start(Rectangle subjectArea) {
				if (fSubjectControl == null || fSubjectControl.isDisposed())
					return;
				fSubjectControl.addListener(SWT.FocusIn, this);
				fSubjectControl.getDisplay().addFilter(SWT.Activate, this);
			}

			public void stop() {
				if (fSubjectControl == null || fSubjectControl.isDisposed())
					return;
				fSubjectControl.removeListener(SWT.FocusIn, this);
				fSubjectControl.getDisplay().removeFilter(SWT.Activate, this);
			}

			public void handleEvent(Event event) {
				switch (event.type) {
				case SWT.FocusIn:
				case SWT.Activate:
					IInformationControl control = getInformationControl();
					if (!control.isFocusControl())
						hideInformationControl();
					break;
				}
			}
		}

		private InformationPresenter(IInformationControlCreator creator) {
			super(creator);
			takesFocusWhenVisible(true);
			setCloser(new Closer());
			setAnchor(ANCHOR_GLOBAL);
		}

		protected void computeInformation() {
			setInformation(getModel().getRoot(), new Rectangle(0, 0, 0, 0));
		}
	}
	
	private StructuredViewer fZipViewer;
	private IToolBarManager fToolBar;
	private ZipActionGroup fZipActionGroup;
	private OpenActionGroup fOpenActionGroup;
	private Map fActions = new HashMap();
	private IResourceChangeListener fInputFileListener;
	private ZipModel fModel;
	private long fModelModified;
	private ZipOutlinePage fOutlinePage;
	private FrameList fFrameList;
	private IMemento fState;
	private boolean fCheckedDeletion;
	private ISelectionChangedListener fOutlineSelectionChangedListener;
	private DisposeListener fTableDisposeListener = new DisposeListener() {
		public void widgetDisposed(DisposeEvent e) {
			storeTableColumnPreferences();
		}
	};
	private InformationPresenter fInformationPresenter;
	private boolean createColumns;

	public final static String ACTION_TOGGLE_MODE = "ToggleViewMode"; //$NON-NLS-1$
	public final static String ACTION_COLLAPSE_ALL = "CollapseAll"; //$NON-NLS-1$
	public final static String ACTION_SELECT_ALL = "SelectAll"; //$NON-NLS-1$
	public final static String ACTION_SELECT_PATTERN = "SelectPattern"; //$NON-NLS-1$
	public final static String ACTION_REVERSE_SELECTION = "ReverseSelection"; //$NON-NLS-1$
	public final static String ACTION_NEW_FOLDER = "NewFolder"; //$NON-NLS-1$
	public final static String ACTION_BACK = "Back"; //$NON-NLS-1$
	public final static String ACTION_FORWARD = "Forward"; //$NON-NLS-1$
	public final static String ACTION_UP = "Up"; //$NON-NLS-1$
	public final static String ACTION_GO_INTO = "GoInto"; //$NON-NLS-1$
	public final static String ACTION_RENAME = "Rename"; //$NON-NLS-1$
	public final static String ACTION_QUICK_OUTLINE = "QuickOutline"; //$NON-NLS-1$
	
	public void doSave(IProgressMonitor monitor) {
		IEditorInput input = doGetEditorInput();
		if (input instanceof IFileEditorInput)
			internalSave(((IFileEditorInput) input).getFile().getLocation(), monitor);
		else if (input instanceof IPathEditorInput)
			internalSave(((IPathEditorInput) input).getPath(), monitor);
		else if (input instanceof IURIEditorInput)
			internalSave(new Path(((IURIEditorInput) input).getURI().getPath()), monitor);
		else
			fModel.logError("The input " + input + " cannot be saved"); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	private void internalSave(IPath locationPath, IProgressMonitor monitor) {
		Node root = getRootNode();
		monitor.beginTask(Messages.getString("ZipEditor.3"), 100); //$NON-NLS-1$
		monitor.worked(1);
		int totalWork = Utils.computeTotalNumber(root.getChildren(), monitor);
		SubMonitor subMonitor = SubMonitor.convert(monitor, 99);
		monitor.setTaskName(Messages.getString("ZipEditor.2")); //$NON-NLS-1$
		monitor.subTask(locationPath.lastSegment());
		subMonitor.beginTask(Messages.getString("ZipEditor.2") + locationPath, totalWork); //$NON-NLS-1$
		InputStream in = null;
		try {
			ContentTypeId typeToSave = ZipContentDescriber.getContentTypeForFileExtension(locationPath.getFileExtension());
			if (typeToSave == null) {
				ZipEditorPlugin.log(new Status(IStatus.WARNING, ZipEditorPlugin.PLUGIN_ID, 0,
						"No content type found for file extension " + locationPath.lastSegment() + ", using zip", null)); //$NON-NLS-1$ //$NON-NLS-2$
				typeToSave = ContentTypeId.ZIP_FILE;
			}
			in = root.getModel().save(typeToSave, subMonitor);
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
			if (fZipViewer instanceof TableViewer &&
					PreferenceConstants.getPreferenceSuffix(typeToSave) != PreferenceConstants.getPreferenceSuffix(root.getModel().getType()))
				createColumns = true;
			doRevert();
		} catch (final Exception e) {
			if (Utils.isUIThread()) {
				doShowErrorDialog(e);
			} else {
	        	getSite().getShell().getDisplay().syncExec(new Runnable() {
	        		public void run() {
	    				doShowErrorDialog(e);
	        		}
	        	});
			}
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

	private void doShowErrorDialog(Throwable e) {
		ZipEditorPlugin.showErrorDialog(getSite().getShell(),
				Messages.getFormattedString("ZipEditor.12", getEditorInput().getName()), //$NON-NLS-1$
				e);
	}

	private IEditorInput internalSaveWorkspaceFile(IFile file, InputStream in, IProgressMonitor monitor) throws Exception {
		if (file.exists())
			file.setContents(in, true, true, monitor);
		else
			file.create(in, true, monitor);
		return new FileEditorInput(file);
	}

	private IEditorInput internalSaveLocalFile(File file, InputStream in) throws Exception {
		Utils.readAndWrite(in, new FileOutputStream(file), true);
		IFileStore fileStore = EFS.getLocalFileSystem().getStore(new Path(file.getParentFile().getAbsolutePath()));
		fileStore = fileStore.getChild(file.getName());
		return Utils.createEditorInput(fileStore);
	}

	public void doSaveAs() {
		SaveAsDialog dialog = new SaveAsDialog(getSite().getShell());
		IEditorInput input = doGetEditorInput();
		IFile original = (input instanceof IFileEditorInput) ? ((IFileEditorInput) input).getFile() : null;
		if (original != null)
			dialog.setOriginalFile(original);

		dialog.create();
		if (dialog.open() == Window.CANCEL) {
			return;
		}
		final IPath filePath = dialog.getResult();
		if (filePath == null)
			return;

		final IFile newFile = ResourcesPlugin.getWorkspace().getRoot().getFile(filePath);
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				internalSave(newFile.getLocation(), monitor);
			}
		};
		try {
			getSite().getWorkbenchWindow().run(true, true, op);
			doFirePropertyChange(PROP_TITLE);
		} catch (Exception e) {
			fModel.logError(e);
		}
	}

	public void doRevert() {
		if (fModel != null)
			fModel.dispose();
		fModel = null;
		doFirePropertyChange(PROP_DIRTY);
		if (Utils.isUIThread())
			activateActions();
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
			fOutlinePage = null;
		}
		getPreferenceStore().removePropertyChangeListener(this);
		fActions.clear();
		fActions = null;
		fOpenActionGroup.dispose();
		fZipActionGroup.dispose();
		super.dispose();		
	}

	private ZipModel createModel() {
		Object[] info = getEditorInputFileInfo(true);
		File file = (File) info[0];
		InputStream in = (InputStream) info[1];
		boolean isReadOnly = ((Boolean) info[2]).booleanValue();
		if (in != null && file == null || !file.exists()) {
			try {
				file = File.createTempFile("tmp", null); //$NON-NLS-1$
				file.deleteOnExit();
				Utils.readAndWrite(in, new FileOutputStream(file), true);
				in = new FileInputStream(file);
			} catch (IOException e) {
				ZipEditorPlugin.log(e);
			}
		}
		return new ZipModel(file, in, isReadOnly, this);
	}
	
	public void reportError(IStatus message) {
		ZipEditorActionBarContributor contributor = (ZipEditorActionBarContributor) getEditorSite().getActionBarContributor();
		if (message.getException() instanceof ZipEditorZstdException) {
			doShowErrorDialog(message.getException());
		}
		contributor.reportError(this, message);
	}
	
	private Object[] getEditorInputFileInfo(boolean getInputStream) {
		IEditorInput input = doGetEditorInput();
		IPath path = null;
		InputStream in = null;
		File file = null;
		Boolean readonly = Boolean.TRUE;
		if (input instanceof IFileEditorInput) {
			path = ((IFileEditorInput) input).getFile().getLocation();
			file = path.toFile();
			if (getInputStream) {
				try {
					in = ((IFileEditorInput) input).getFile().getContents();
				} catch (CoreException e) {
					throw new RuntimeException(e);
				}
			}
			readonly = new Boolean(((IFileEditorInput) input).getFile().isReadOnly());
		} else {
			if (input instanceof IPathEditorInput) {
				path = ((IPathEditorInput) input).getPath();
				file = path.toFile();
				readonly = new Boolean(!file.canWrite());
			}
			if (input instanceof IStorageEditorInput) {
				try {
					if (path == null) {
						path = ((IStorageEditorInput) input).getStorage().getFullPath();
						if (path != null)
							file = path.toFile();
					}
					if (getInputStream)
						in = ((IStorageEditorInput) input).getStorage().getContents();
					readonly = new Boolean(((IStorageEditorInput) input).getStorage().isReadOnly());
				} catch (CoreException e) {
					ZipEditorPlugin.log(e);
				}
			}
			if (path == null && input instanceof IURIEditorInput) {
				try {
					file = new File(((IURIEditorInput) input).getURI());
					path = new Path(file.getAbsolutePath());
					// file exists cause it can be read but is set readonly
					readonly = new Boolean(!file.canWrite() && file.canRead());
					if (getInputStream)
						in = (((IURIEditorInput) input).getURI().toURL()).openStream();
				} catch (Exception e) {
					ZipEditorPlugin.log(e);
				}
			}
		}
		return new Object[] { file, in, readonly };
	}
	
	private IEditorInput doGetEditorInput() {
		IEditorInput input = getEditorInput();
		return input instanceof DelegateEditorInput ? ((DelegateEditorInput) input).delegate : input;
	}
	
	public boolean isDirty() {
		return fModel != null && fModel.isDirty() && !fModel.isReadonly();
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
	}
	
	private void createContent(Composite parent, int mode) {
		createControls(parent, mode);
		installOutlineInformationControl();
		createActions(mode);
		fillToolBar(fToolBar, mode);
		addViewerListener(getViewer());
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
		if ((mode & PreferenceConstants.VIEW_MODE_TREE) == 0
				&& (mode & PreferenceConstants.VIEW_MODE_FOLDERS_VISIBLE) > 0) {
			bar.add(getAction(ACTION_BACK));
			bar.add(getAction(ACTION_FORWARD));
			bar.add(getAction(ACTION_UP));
			bar.add(new Separator());
		}
		bar.add(getAction(ACTION_TOGGLE_MODE));
		fOpenActionGroup.fillToolBarManager(bar);
		fZipActionGroup.fillToolBarManager(bar, mode);
		bar.add(new Separator());
		if ((mode & PreferenceConstants.VIEW_MODE_TREE) > 0) {
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
		fZipViewer.setSelection(selection);
		fZipViewer.getControl().setFocus();
		((ZipContentProvider) fZipViewer.getContentProvider()).disposeModel(true);
	}
	
	private StructuredViewer createZipViewer(Composite parent, int mode) {
		StructuredViewer viewer = null;
		if ((mode & PreferenceConstants.VIEW_MODE_TREE) > 0) {
			viewer = new TreeViewer(parent);
		} else {
			viewer = createTableViewer(parent);
		}
		
		viewer.setContentProvider(new LazyZipContentProvider(mode));
		viewer.setLabelProvider(new ZipLabelProvider());
		viewer.setComparator(new ZipSorter(PreferenceConstants.PREFIX_EDITOR));
		viewer.setComparer(new NodeComparer());
		viewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
		MenuManager manager = new MenuManager();
		manager.setRemoveAllWhenShown(true);
		final IEditorInput originalInput = doGetEditorInput();
		manager.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				handleContextMenuAboutToShow(manager);
				// no better idea to prevent other items being added to the menu
				if (!(getEditorInput() instanceof DelegateEditorInput))
					ZipEditor.super.setInput(new DelegateEditorInput(originalInput));
			}
		});
		Menu contextMenu = manager.createContextMenu(viewer.getControl());
		contextMenu.addListener(SWT.Hide, new Listener() {
			public void handleEvent(Event event) {
				if (getEditorInput() instanceof DelegateEditorInput)
					ZipEditor.super.setInput(originalInput);
			}
		});
		viewer.getControl().setMenu(contextMenu);
		getSite().registerContextMenu(manager, fZipViewer);
		
		viewer.getControl().addFocusListener(new FocusAdapter() {
			public void focusGained(FocusEvent e) {
				ZipEditorPlugin.getDefault().checkFilesForModification(fModel);
				firePropertyChange(PROP_DIRTY);
			}
		});

		if (viewer instanceof TableViewer)
			createColumns = true;
		setViewerInput(viewer);
		initDragAndDrop(viewer);

		return viewer;
	}
	
	private void addViewerListener(StructuredViewer viewer) {
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
	}
	
	private Object updateFrameList() {
		Node currentNode = null;
		for (int i = 0, n = fFrameList.size(); i < n; i++) {
			TableFrame tableFrame = (TableFrame) fFrameList.getFrame(i);
			String path = tableFrame.getInput() != null ? ((Node) tableFrame.getInput()).getPath() : null;
			Node node = fModel.findNode(path);
			if (node == null)
				continue;
			tableFrame.setInput(node);
			if (i == fFrameList.getCurrentIndex())
				currentNode = node;
		}
		return currentNode;
	}
	
	private void setViewerInput(final StructuredViewer viewer) {
		Object input = null;
		if (fModel == null) {
			fModel = createModel();
			if (fModel.getZipPath() != null)
				fModelModified = fModel.getZipPath().lastModified();
			fModel.addModelListener(new ModelListener());
			input = fModel.getRoot();
			if (fFrameList != null) {
				Object node = updateFrameList();
				if (node != null)
					input = node;
			}
		} else {
			input = fModel.getRoot();
		}
		if (input != null) {
			if (Utils.isUIThread()) {
				doSetViewerInput(viewer, input);
			} else {
				viewer.getControl().getDisplay().syncExec(new Runnable() {
					public void run() {
						doSetViewerInput(viewer, fModel.getRoot());
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

	private void doSetViewerInput(StructuredViewer viewer, Object input) {
		viewer.setInput(input);
		if (createColumns) {
			TableViewer tableViewer = (TableViewer) viewer;
			TableColumn[] columns = tableViewer.getTable().getColumns();
			for (int i = 0; i < columns.length; i++) {
				columns[i].dispose();
			}
			createTableColumns(((TableViewer) viewer).getTable());
			((Composite) viewer.getControl()).layout();
			((ZipLabelProvider) viewer.getLabelProvider()).resetOrder();
			createColumns = false;
		}
		if (fFrameList != null)
			viewer.refresh(true);
	}

	private void setViewerInputAgain(final StructuredViewer viewer){
		viewer.getControl().getDisplay().timerExec(100, new Runnable() {
			public void run() {
				setViewerInput(viewer);
			}
		});
	}
	
	private void handleContextMenuAboutToShow(IMenuManager manager) {
		manager.add(new Separator(IWorkbenchActionConstants.OPEN_EXT));
		manager.add(new Separator());
		manager.add(getAction(ACTION_NEW_FOLDER));
		manager.add(new Separator());
		fOpenActionGroup.setContext(new ActionContext(fZipViewer.getSelection()));
		fOpenActionGroup.fillContextMenu(manager);
		fZipActionGroup.setContext(new ActionContext(fZipViewer.getSelection()));
		fZipActionGroup.fillContextMenu(manager);
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	private void handleViewerDoubleClick() {
		Node[] nodes = getSelectedNodes();
		if (nodes == null || nodes.length == 0)
			return;
		if (nodes.length == 1 && nodes[0].isFolder()) {
			if (fZipViewer instanceof TreeViewer) {
				((TreeViewer) fZipViewer).setExpandedState(nodes[0], !((TreeViewer) fZipViewer).getExpandedState(nodes[0]));
				return;
			} else if (fZipViewer instanceof TableViewer) {
				((GoIntoAction) getAction(ACTION_GO_INTO)).run();
				return;
			}
		}
		Utils.openFilesFromNodes(nodes);
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
		if (fFrameList != null)
			((TableFrame) fFrameList.getCurrentFrame()).setSelection(structuredSelection);
	}

	private void handleOutlineSelectionChanged() {
		IStructuredSelection selection = (IStructuredSelection) fOutlinePage.getSelection();
		if (selection.size() != 1)
			return;
		fZipViewer.setSelection(selection, true);
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
		TableViewer viewer = new ZipTableViewer(parent, SWT.MULTI | SWT.FULL_SELECTION | SWT.VIRTUAL);
		Table table = viewer.getTable();
		table.setHeaderVisible(true);
		table.addDisposeListener(fTableDisposeListener);
		return viewer;
	}
	
	private void createTableColumns(Table table) {
		IPreferenceStore store = getPreferenceStore();
		String suffix = PreferenceConstants.getPreferenceSuffix(fModel.getType());
		int sortColumn = store.getInt(PreferenceConstants.SORT_BY + suffix);
		int sortDirection = store.getInt(PreferenceConstants.SORT_DIRECTION + suffix);
		Integer[] visibleColumns = (Integer[]) PreferenceInitializer.split(store.getString(PreferenceConstants.VISIBLE_COLUMNS + suffix),
				PreferenceConstants.COLUMNS_SEPARATOR, Integer.class);
		for (int i = 0; i < visibleColumns.length; i++) {
			int type = visibleColumns[i].intValue();
			createTableColumn(table, Messages.getString("ZipNodeProperty." + type), type, sortColumn, sortDirection); //$NON-NLS-1$
		}
		TableLayout layout = new TableLayout();
		TableColumn[] columns = table.getColumns();
		for (int i = 0; i < columns.length; i++) {
			int width = store.getInt(PreferenceConstants.SORT_COLUMN_WIDTH + suffix + columns[i].getData());
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
		String suffix = PreferenceConstants.getPreferenceSuffix(fModel.getType());
		TableColumn[] columns = table.getColumns();
		int[] order = table.getColumnOrder();
		for (int i = 0; i < columns.length; i++) {
			store.setValue(PreferenceConstants.SORT_COLUMN_WIDTH + suffix + columns[i].getData(), columns[i].getWidth());
		}
		for (int i = 0; i < order.length; i++) {
			order[i] = ((Integer) columns[order[i]].getData()).intValue();
		}
		store.setValue(PreferenceConstants.VISIBLE_COLUMNS + suffix, PreferenceInitializer.join(order, PreferenceConstants.COLUMNS_SEPARATOR));
	}

	private void handleSortColumnSelected(TableColumn column) {
		IPreferenceStore store = getPreferenceStore();
		String suffix = PreferenceConstants.getPreferenceSuffix(fModel.getType());
		int sortColumn = store.getInt(PreferenceConstants.SORT_BY + suffix);
		int sortDirection = store.getInt(PreferenceConstants.SORT_DIRECTION + suffix);
		
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
		store.setValue(PreferenceConstants.SORT_DIRECTION + suffix, sortDirection);
		store.setValue(PreferenceConstants.SORT_BY + suffix, sortColumn);
		((ZipSorter) fZipViewer.getComparator()).update();
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

	private void createActions(int mode) {
		fZipActionGroup = new ZipActionGroup(this);
		fOpenActionGroup = new OpenActionGroup(getViewer());
		setAction(ACTION_NEW_FOLDER, new NewFolderAction(getViewer()));
		ToggleViewModeAction toggleViewModeAction = new ToggleViewModeAction(this, ActionMessages.getString("ToggleViewModeAction.0"), PreferenceConstants.PREFIX_EDITOR, PreferenceConstants.VIEW_MODE_TREE); //$NON-NLS-1$
		toggleViewModeAction.setToolTipText(ActionMessages.getString("ToggleViewModeAction.1")); //$NON-NLS-1$
		toggleViewModeAction.setImageDescriptor(ZipEditorPlugin.getImageDescriptor("icons/togglemode.gif")); //$NON-NLS-1$
		setAction(ACTION_TOGGLE_MODE, toggleViewModeAction);

		setAction(ACTION_COLLAPSE_ALL, new CollapseAllAction(getViewer()));
		setAction(ACTION_SELECT_ALL, new SelectAllAction(getViewer()));
		setAction(ACTION_SELECT_PATTERN, new SelectPatternAction(getViewer()));
		setAction(ACTION_REVERSE_SELECTION, new ReverseSelectionAction(getViewer()));
		setAction(ACTION_RENAME, new RenameNodeAction(getViewer()));
		setAction(ACTION_QUICK_OUTLINE, new QuickOutlineAction(this));

		fFrameList = createFrameList(mode);
		if (fFrameList != null) {
			setAction(ACTION_BACK, new BackAction(fFrameList));
			setAction(ACTION_FORWARD, new ForwardAction(fFrameList));
			setAction(ACTION_UP, new UpAction(fFrameList));
			setAction(ACTION_GO_INTO, new GoIntoAction(fFrameList));
		}

		activateActions();
	}

	private FrameList createFrameList(int mode) {
		if (!(fZipViewer instanceof TableViewer))
			return null;
		TableViewerFrameSource frameSource = new TableViewerFrameSource((TableViewer) fZipViewer);
		FrameList frameList = new FrameList(frameSource);
		frameList.addPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				String inputName = getEditorInput().getName();
				TableFrame frame = (TableFrame) ((FrameList) event.getSource()).getCurrentFrame();
				Node node = (Node) frame.getInput();
				if (node != null){
					String frameName = node.getPath();
					frameName = inputName + (frameName.length() > 0 ? " - " + frameName : new String()); //$NON-NLS-1$
					setPartName(frameName);
				}
			}
		});
		frameSource.connectTo(frameList);
		setPartName(getEditorInput().getName());
		if (fState != null && (mode & PreferenceConstants.VIEW_MODE_FOLDERS_VISIBLE) > 0) {
			TableFrame currentFrame = (TableFrame) frameList.getCurrentFrame();
			currentFrame.restoreState(fState, fModel);
			if (fModel.getRoot() == currentFrame.getInput())
				fZipViewer.setSelection(currentFrame.getSelection());
			else
				frameList.gotoFrame(currentFrame);
		}
		return frameList;
	}

	private void installOutlineInformationControl() {
		fInformationPresenter = new InformationPresenter(new IInformationControlCreator() {
			public IInformationControl createInformationControl(Shell parent) {
				return new OutlineInformationControl(fZipViewer, parent, new NodeComparer());
			}
		});
		fInformationPresenter.setSizeConstraints(60, 20, true, false);
		IDialogSettings section = ZipEditorPlugin.getDefault().getDialogSettings().getSection("outlinePresenterBounds"); //$NON-NLS-1$
		if (section == null)
			section = ZipEditorPlugin.getDefault().getDialogSettings().addNewSection("outlinePresenterBounds"); //$NON-NLS-1$
		fInformationPresenter.setRestoreInformationControlBounds(section, false, true);

		fInformationPresenter.install(fZipViewer.getControl());
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
			}
			getEditorSite().getShell().getDisplay().asyncExec(new Runnable() {
				public void run() {
					fOutlinePage.setInput(fModel.getRoot());
				}
			});
			return fOutlinePage;
		}
		return super.getAdapter(adapter);
	}

	public Node[] getSelectedNodes() {
		return Utils.getSelectedNodes(fZipViewer.getSelection());
	}
	
	public void setFocus() {
		if (fZipViewer != null)
			fZipViewer.getControl().setFocus();
		activateActions();
		checkIfFileHasBeenChangedLocaly(doGetEditorInput());
	}

	private void checkIfFileHasBeenChangedLocaly(IEditorInput editorInput) {
		Object[] info = getEditorInputFileInfo(false);
		File file = (File) info[0];
		boolean readOnly = ((Boolean) info[2]).booleanValue();
		boolean fileDeleted = file != null && !file.exists();
		boolean fileModified = file != null && file.exists() && file.lastModified() > fModelModified;
		if (editorInput instanceof IFileEditorInput) {
			fileModified = !((IFileEditorInput) editorInput).getFile().isSynchronized(IResource.DEPTH_ONE);
		}
		if (fileModified) {
			if (MessageDialog.openQuestion(getSite().getShell(),
					Messages.getString("ZipEditor.4"), //$NON-NLS-1$
					Messages.getString("ZipEditor.5"))) { //$NON-NLS-1$
				if (editorInput instanceof IFileEditorInput) {
					try {
						((IFileEditorInput) editorInput).getFile().refreshLocal(IResource.DEPTH_ONE, new NullProgressMonitor());
					} catch (CoreException e) {
						fModel.logError(e);
					}
				}
				doRevert();
			}
		} else if (fileDeleted && !readOnly && !fCheckedDeletion) {
			fCheckedDeletion = true;
			MessageDialog dialog = new MessageDialog(getSite().getShell(),
					Messages.getString("ZipEditor.6"), null, Messages //$NON-NLS-1$
							.getFormattedString("ZipEditor.7", editorInput //$NON-NLS-1$
									.getName()), MessageDialog.QUESTION,
					new String[] { Messages.getString("ZipEditor.13"), //$NON-NLS-1$
							Messages.getString("ZipEditor.14"), //$NON-NLS-1$
							IDialogConstants.CANCEL_LABEL }, 0);
			switch (dialog.open()) {
			case 0:
				close();
				break;
			case 1:
				doSave(new NullProgressMonitor());
				break;
			}
		} else if (!fileDeleted) {
			fCheckedDeletion = false;
		}
	}

	private void activateActions() {
		getEditorSite().getActionBars().setGlobalActionHandler(ActionFactory.SELECT_ALL.getId(), getAction(ACTION_SELECT_ALL));
		getEditorSite().getActionBars().setGlobalActionHandler(SelectPatternAction.ID, getAction(ACTION_SELECT_PATTERN));
		getEditorSite().getActionBars().setGlobalActionHandler(ReverseSelectionAction.ID, getAction(ACTION_REVERSE_SELECTION));
		getEditorSite().getActionBars().setGlobalActionHandler(QuickOutlineAction.ID, getAction(ACTION_QUICK_OUTLINE));
		getEditorSite().getActionBars().setGlobalActionHandler(ActionFactory.RENAME.getId(), getAction(ACTION_RENAME));
		IKeyBindingService service = getEditorSite().getKeyBindingService();
		service.setScopes(new String[] {"zipeditor.zipEditorContext"}); //$NON-NLS-1$
		fZipActionGroup.setContext(new ActionContext(fZipViewer.getSelection()));
		fZipActionGroup.fillActionBars(getEditorSite().getActionBars());
		getEditorSite().getActionBars().updateActionBars();
	}

	public void showQuickOutline() {
		fInformationPresenter.showInformation();
	}

	public StructuredViewer getViewer() {
		return fZipViewer;
	}
	
	public Node getRootNode() {
		return (Node) fZipViewer.getInput();
	}

	public ZipModel getModel() {
		return fModel;
	}

	public int getMode() {
		return getPreferenceStore().getInt(PreferenceConstants.PREFIX_EDITOR + PreferenceConstants.VIEW_MODE);
	}
	
	public void propertyChange(PropertyChangeEvent event) {
		if ((PreferenceConstants.PREFIX_EDITOR + PreferenceConstants.SORT_ENABLED + SortAction.SORTING_CHANGED).equals(event.getProperty()))
			updateView(getMode(), true);
	}
	
	public void saveState(IMemento memento) {
		if (fFrameList != null)
			((TableFrame) fFrameList.getCurrentFrame()).saveState(memento);
	}
	
	public void restoreState(IMemento memento) {
		fState = memento;
	}
}
