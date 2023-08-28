/*
 * (c) Copyright 2002, 2014 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.actions;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILazyContentProvider;
import org.eclipse.jface.viewers.ILazyTreeContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.fieldassist.ContentAssistCommandAdapter;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.part.PluginDropAdapter;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;

import zipeditor.ZipEditorPlugin;

public class FileSystemChooseControl extends Composite implements ISelectionProvider {
	private class FileSystemContentProvider implements ITreeContentProvider, ILazyTreeContentProvider, ILazyContentProvider {
		private Viewer fViewer;
		private File[] fTableFiles;

		private FileSystemContentProvider(Viewer viewer) {
			fViewer = viewer;
		}

		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof File[])
				return (Object[]) parentElement;
			else if (parentElement instanceof File) {
				File file = (File) parentElement;
				if (file.isDirectory()) {
					File[] files = getFiles(file);
					if (files != null)
						return files;
				}
			}
			return new Object[0];
		}

		public boolean hasChildren(Object element) {
			return getChildren(element).length > 0;
		}
		
		public Object[] getElements(Object inputElement) {
			return getChildren(inputElement);
		}
		
		public Object getParent(Object element) {
			return element instanceof File ? ((File) element).getParentFile() : null;
		}

		public void dispose() {
			filesCache.clear();
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			if (newInput != null && viewer instanceof TableViewer)
				fTableFiles = getFiles((File) newInput);
		}

		public void updateElement(Object parent, int index) {
			File[] files = parent == fViewer.getInput() ? (File[]) parent : getFiles((File) parent);
			if (files.length > index) {
				((TreeViewer) fViewer).replace(parent, index, files[index]);
				((TreeViewer) fViewer).setChildCount(files[index], getFiles(files[index]).length);
			}
		}

		public void updateChildCount(Object element, int currentChildCount) {
			File[] files = element == fViewer.getInput() ? (File[]) element : getFiles((File) element);
			if (currentChildCount != files.length)
				((TreeViewer) fViewer).setChildCount(element, files.length);
		}

		public void updateElement(int index) {
			if (fTableFiles.length > index) {
				((TableViewer) fViewer).replace(fTableFiles[index], index);
				if (fTableSelection.containsKey(fTableFiles[index]))
					((TableViewer) fViewer).getTable().select(index);
			}
		}
	}
	private class FileSystemLabelProvider extends LabelProvider {
		public String getText(Object element) {
			if (element instanceof File) {
				File file = (File) element;
				return file.getName().length() > 0 ? file.getName() : file.getPath();
			}
			return super.getText(element);
		}
		
		public Image getImage(Object element) {
			if (element instanceof File) {
				File file = (File) element;
				if (file.isDirectory())
					return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
				ImageDescriptor descriptor = PlatformUI.getWorkbench().getEditorRegistry().getImageDescriptor(file.getName(), null);
				if (descriptor != null)
					return ZipEditorPlugin.getImage(descriptor);
				return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FILE);
			}

			return super.getImage(element);
		}
	}
	private class WorkbenchContentProvider extends org.eclipse.ui.model.WorkbenchContentProvider {
		private boolean fReturnFiles;
		WorkbenchContentProvider(boolean returnFiles) {
			fReturnFiles = returnFiles;
		}
		public Object[] getChildren(Object element) {
			Object[] children = super.getChildren(element);
			if (!fReturnFiles)
				children = filterIFiles(children);
			return children;
		}
	}
	private class ContentProposalLabelProvider extends FileSystemLabelProvider {
		private LocalResourceManager resources;
		public Image getImage(Object element) {
			ContentProposal proposal = (ContentProposal) element;
			Object file = proposal.getFile();
			if (file instanceof IResource) {
				IResource resource = (IResource) file;
				IWorkbenchAdapter adapter = (IWorkbenchAdapter) resource.getAdapter(IWorkbenchAdapter.class);
				ImageDescriptor imageDescriptor = adapter.getImageDescriptor(resource);
				return imageDescriptor == null ? null : (Image) getResourceManager().get(imageDescriptor);
			} else {
				return super.getImage(file);
			}
		}
		private LocalResourceManager getResourceManager() {
			if (resources == null)
				resources = new LocalResourceManager(JFaceResources.getResources());
			return resources;
		}
		public void dispose() {
			super.dispose();
			if (resources != null)
				resources.dispose();
		}
		public String getText(Object element) {
			return ((IContentProposal) element).getLabel();
		}
	}
	private class FilterArea extends Composite implements FocusListener, KeyListener, DisposeListener, TraverseListener {
		private Color fGrayColor;
		private final String fEmptyText = ActionMessages.getString("FileSystemChooseControl.0"); //$NON-NLS-1$
		private final Text fText;
		private ContentProposalLabelProvider fContentProposalLabelProvider;

		public FilterArea(Composite parent) {
			super(parent, SWT.NONE);
			GridLayout layout = new GridLayout();
			layout.marginWidth = 0;
			setLayout(layout);
			setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			fText = new Text(this, SWT.LEFT | SWT.BORDER);
			fText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			fText.setText(fEmptyText);
			fText.setForeground(getGrayColor());
			fText.addFocusListener(this);
			fText.addKeyListener(this);
			fText.addTraverseListener(this);

			ContentAssistCommandAdapter adapter = new ContentAssistCommandAdapter(fText, new TextContentAdapter(),
					new IContentProposalProvider() {
						public IContentProposal[] getProposals(String contents, int position) {
							return doGetProposals(contents, position);
						}
					}, ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS, new char[0], true);
			fContentProposalLabelProvider = new ContentProposalLabelProvider();
			adapter.setLabelProvider(fContentProposalLabelProvider);
			addDisposeListener(this);
		}

		public void keyTraversed(TraverseEvent e) {
			if (e.detail == SWT.TRAVERSE_RETURN) {
				List files = getFileSelection(false);
				boolean isFile = files.size() == 1 && (files.get(0) instanceof File && ((File) files.get(0)).isFile()
						|| files.get(0) instanceof IFile);
				if (!isFile)
					e.doit = false;
			}
		}

		public void keyPressed(KeyEvent e) {
		}

		public void keyReleased(KeyEvent e) {
			pathChanged(fText.getText());
		}

		public String getText() {
			return fText.getText();
		}

		void setText(String text) {
			if (text == null || text.length() == 0)
				fText.setText(fEmptyText);
			else
				fText.setText(text);
			focusLost(null);
		}

		private Color getGrayColor() {
			if (fGrayColor == null)
				fGrayColor = new Color(getDisplay(), 160, 160, 160);
			return fGrayColor;
		}

		public void widgetDisposed(DisposeEvent e) {
			if (fGrayColor != null)
				fGrayColor.dispose();
			fContentProposalLabelProvider.dispose();
		}

		public void focusLost(FocusEvent e) {
			if (fText.getText().length() == 0)
				fText.setText(fEmptyText);
			fText.setForeground(fEmptyText.equals(fText.getText()) ? getGrayColor() : getForeground());
		}
		
		public void focusGained(FocusEvent e) {
			if (fEmptyText.equals(fText.getText())) {
				fText.setForeground(getForeground());
				fText.setText(""); //$NON-NLS-1$
			}
		}
	}

	private class DropAdapter extends PluginDropAdapter {

		private DropAdapter(StructuredViewer viewer) {
			super(viewer);
			setFeedbackEnabled(false);
			setScrollExpandEnabled(false);
		}

		public void dragEnter(DropTargetEvent event) {
			if (FileTransfer.getInstance().isSupportedType(event.currentDataType)
					&& event.detail == DND.DROP_DEFAULT) {
				event.detail = DND.DROP_COPY;
			}
			super.dragEnter(event);
		}

		public boolean validateDrop(Object target, int operation, TransferData transferType) {
			
			if (FileTransfer.getInstance().isSupportedType(transferType)) {
				String[] paths = (String[]) FileTransfer.getInstance().nativeToJava(transferType);
				if (paths.length == 1) {
					File file = new File(paths[0]);
					if (file.exists() && file.isDirectory())
						return true;
				}
			}
			return super.validateDrop(target, operation, transferType);
		}

		public boolean performDrop(Object data) {
			String[] paths = (String[]) data;
			List files = new ArrayList(1);
			files.add(new File(paths[0]));
			setFileSelection(files);
			return true;
		}
	}

	private FilterArea fText;
	private TreeViewer fTree;
	private TableViewer fTable;
	private final Map fTableSelection = new HashMap();
	private boolean fInPathChange;
	private final boolean fUseWorkspace;
	private final boolean fShowFiles;
	private final boolean fMultiSelect;
	private final ListenerList fSelectionChangedListeners = new ListenerList();
	private Map filesCache = new HashMap();

	public FileSystemChooseControl(Composite parent, boolean useWorkspace, boolean showFiles, boolean multiselect) {
		super(parent, SWT.NONE);
		fUseWorkspace = useWorkspace;
		fShowFiles = showFiles;
		fMultiSelect = multiselect;
		setLayout(new GridLayout());
		createControl(this);
	}

	private void createControl(Composite parent) {
		fText = new FilterArea(parent);
		fText.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		SashForm sash = new SashForm(parent, SWT.HORIZONTAL | SWT.SMOOTH);
		sash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		createTree(sash);
		createTable(sash);
	}

	private void createTree(Composite parent) {
		Composite control = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		control.setLayout(layout);
		fTree = new TreeViewer(control, (fMultiSelect ? SWT.MULTI : SWT.SINGLE) | SWT.VIRTUAL | SWT.BORDER);
		fTree.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		fTree.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				setTableInput(((IStructuredSelection) event.getSelection()).getFirstElement());
				propagateSelectionEvent(event);
			}
		});
		fTree.setUseHashlookup(true);
		fTree.setContentProvider(fUseWorkspace ? (IContentProvider) new WorkbenchContentProvider(false) : new FileSystemContentProvider(fTree));
		fTree.setLabelProvider(fUseWorkspace ? (IBaseLabelProvider) new WorkbenchLabelProvider() : new FileSystemLabelProvider());
		fTree.setInput(fUseWorkspace ? (Object) ResourcesPlugin.getWorkspace().getRoot() : File.listRoots());
		fTree.addDropSupport(DND.DROP_COPY | DND.DROP_DEFAULT, new Transfer[] { FileTransfer.getInstance() }, new DropAdapter(fTree));
	}

	private void createTable(Composite parent) {
		Composite control = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		control.setLayout(layout);
		fTable = new TableViewer(control, (fMultiSelect ? SWT.MULTI : SWT.SINGLE) | SWT.VIRTUAL | SWT.BORDER);
		fTable.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		fTable.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				setTableInput(((IStructuredSelection) event.getSelection()).getFirstElement());
			}
		});
		fTable.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				propagateSelectionEvent(event);
			}
		});
		fTable.getTable().addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				if (e.keyCode == 8)
					setTableInput(fUseWorkspace ? (Object) ((IResource) fTable.getInput()).getParent() : ((File) fTable.getInput()).getParentFile());
			}
		});
		fTable.setUseHashlookup(true);
		fTable.setContentProvider(fUseWorkspace ? (IContentProvider) new WorkbenchContentProvider(fShowFiles) : new FileSystemContentProvider(fTable));
		fTable.setLabelProvider(fUseWorkspace ? (IBaseLabelProvider) new WorkbenchLabelProvider() : new FileSystemLabelProvider());
		fTable.addDropSupport(DND.DROP_COPY | DND.DROP_DEFAULT, new Transfer[] { FileTransfer.getInstance() }, new DropAdapter(fTable));
	}

	private void pathChanged(String path) {
		fInPathChange = true;
		if (fUseWorkspace) {
			IResource file = ResourcesPlugin.getWorkspace().getRoot().findMember(path);
			if (file != null && file.exists() && file instanceof IContainer) {
				fTree.setSelection(new StructuredSelection(file));
			} else if (file != null) {
				IContainer parent = file.getParent();
				if (parent != null && parent.exists()) {
					fTree.setSelection(new StructuredSelection(parent));
					IResource[] files = null;
					try {
						files = parent.members();
					} catch (CoreException e) {
						ZipEditorPlugin.log(e);
					}
					StringMatcher matcher = new StringMatcher(file.getName(), true, false);
					List selection = new ArrayList();
					for (int i = 0; i < files.length; i++) {
						if (files[i] instanceof IFile && matcher.match(files[i].getName()))
							selection.add(files[i]);
					}
					fTable.setSelection(new StructuredSelection(selection));
				}
			}
		} else {
			File file = new File(path);
			if (file.exists() && file.isDirectory()) {
				fTree.setSelection(new StructuredSelection(file));
			} else {
				File parent = file.getParentFile();
				if (parent != null && parent.exists()) {
					fTree.setSelection(new StructuredSelection(parent));
					File[] files = parent.listFiles();
					StringMatcher matcher = new StringMatcher(file.getName(), true, false);
					List selection = new ArrayList();
					for (int i = 0; i < files.length; i++) {
						if (files[i].isFile() && matcher.match(files[i].getName()))
							selection.add(files[i]);
					}
					fTable.setSelection(new StructuredSelection(selection));
				}
			}
		}
		fInPathChange = false;
	}

	private void setTableInput(Object parentFile) {
		if (parentFile != null) {
			Object[] files = {};
			if (fUseWorkspace) {
				if (parentFile instanceof IContainer)
					files = ((WorkbenchContentProvider) fTable.getContentProvider()).getChildren(parentFile);
				else if (parentFile instanceof IFile)
					return;
			} else {
				if (((File) parentFile).isDirectory())
					files = getFiles((File) parentFile);
				else if (((File) parentFile).isFile())
					return;
			}
//			fTree.setInput(fUseWorkspace ? (Object) ResourcesPlugin.getWorkspace().getRoot() : File.listRoots());
			fTable.setInput(parentFile);
			fTable.setItemCount(files.length);
			fTable.setSelection(StructuredSelection.EMPTY);
			if (!fInPathChange)
				fText.setText(fUseWorkspace ? ((IResource) parentFile).getFullPath().toString() : ((File) parentFile).getAbsolutePath());
		} else {
			fTree.getTree().removeAll();
			fTable.getTable().removeAll();
			fTree.setInput(fUseWorkspace ? (Object) ResourcesPlugin.getWorkspace().getRoot() : File.listRoots());
		}
	}

	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		fText.setEnabled(enabled);
		fTree.getTree().setEnabled(enabled);
		fTable.getTable().setEnabled(enabled);
	}

	public List getFileSelection(boolean ignoreTextField) {
		if (!fTable.getSelection().isEmpty())
			return ((IStructuredSelection) fTable.getSelection()).toList();
		if (!fTree.getSelection().isEmpty())
			return ((IStructuredSelection) fTree.getSelection()).toList();
		return ignoreTextField ? Collections.EMPTY_LIST : getFileSelectionFromTextField();
	}

	public List getFileSelectionFromTextField() {
		List list = new ArrayList(1);
		String text = fText.getText();
		if (!fText.fEmptyText.equals(text)) {
			if (fUseWorkspace)
				list.add(ResourcesPlugin.getWorkspace().getRoot().findMember(fText.getText()));
			else
				list.add(new File(fText.getText()));
		}
		return list;
	}

	public void setFileSelection(List files) {
		List oldSelection = getFileSelection(false);
		if (oldSelection == files || oldSelection != null && oldSelection.equals(files))
			return;
		fTree.setSelection(StructuredSelection.EMPTY);
		fTable.setSelection(StructuredSelection.EMPTY);
		if (files != null) {
			fTree.setExpandedElements(new Object[0]);
			List treeSelection = new ArrayList();
			List tableSelection = new ArrayList();
			for (int i = 0; i < files.size(); i++) {
				Object file = files.get(i);
				if (isDirectory(file))
					treeSelection.add(file);
				else if (isFile(file))
					tableSelection.add(file);
			}
			fTree.setSelection(new StructuredSelection(treeSelection), true);
			if (treeSelection.isEmpty() && !tableSelection.isEmpty()) {
				Object input = fTable.getInput();
				Object firstSelected = tableSelection.get(0);
				Object parentFile = null;
				if (firstSelected instanceof File)
					parentFile = ((File) firstSelected).getParentFile();
				else if (firstSelected instanceof IResource)
					parentFile = ((IResource) firstSelected).getParent();
				if (input == null || !input.equals(parentFile))
					setTableInput(parentFile);
			}
			fTableSelection.clear();
			for (int i = 0; i < tableSelection.size(); i++) {
				fTableSelection.put(tableSelection.get(i), null);
			}
			Object selectedObect = files.size() == 1 ? files.get(0) : null;
			if (isDirectory(selectedObect))
				setTableInput(selectedObect);
			if (isFile(selectedObect))
				fText.setText(selectedObect instanceof File ? ((File) selectedObect).getAbsolutePath() :
					((IResource) selectedObect).getFullPath().toString());
		} else {
			setTableInput(null);
			fText.setText(null);
		}
	}
	
	private boolean isDirectory(Object object) {
		return object instanceof File ? ((File) object).isDirectory() : object instanceof IContainer;
	}

	private boolean isFile(Object object) {
		return object instanceof File ? ((File) object).isFile() : object instanceof IFile;
	}

	public void deselectAll() {
		fTree.getTree().deselectAll();
		fTable.getTable().deselectAll();
	}

	private IContentProposal[] doGetProposals(String contents, int position) {
		List proposals = new ArrayList();
		String word = contents.substring(0, position);
		Object[] files = null;
		if (contents.trim().length() == 0) {
			files = fUseWorkspace ? new Object[] { fTree.getInput()} : (Object[]) fTree.getInput();
		} else {
			if (fUseWorkspace) {
				String fileName = word;
				String parentName = word;
				int slashIndex = word.lastIndexOf('/');
				if (slashIndex != -1) {
					parentName = word.substring(0,  slashIndex + 1);
					fileName = word.substring(slashIndex + 1).toLowerCase();
				}
				IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(word);
				IResource parent = null;
				if (resource != null && resource.exists()) {
					parent = resource;
					fileName = ""; //$NON-NLS-1$
				} else {
					parent = ResourcesPlugin.getWorkspace().getRoot().findMember(parentName);
				}
				if (parent instanceof IContainer) {
					try {
						files = ((IContainer) parent).members();
						if (!fShowFiles)
							files = filterIFiles(files);
						List result = new ArrayList(Arrays.asList(files));
						for (Iterator it = result.iterator(); it.hasNext(); ) {
							if (!((IResource) it.next()).getName().toLowerCase().startsWith(fileName))
								it.remove();
						}
						files = result.toArray();
					} catch (CoreException e) {
						ZipEditorPlugin.log(e);
					}
				}
			} else {
				File parent = new File(word);
				if (!parent.exists())
					parent = parent.getParentFile();
				if (parent != null)
					files = getFiles(parent);
			}
		}
		if (files != null) {
			String wordLowerCase = word.toLowerCase();
			for (int i = 0; i < files.length; i++) {
				Object file = files[i];
				String path = fUseWorkspace ? ((IResource) file).getFullPath().toString() : ((File) file).getPath();
				String name = fUseWorkspace ? ((IResource) file).getName() : ((File) file).getName();
				if (name.length() == 0)
					name = path;
				if (path.toLowerCase().startsWith(wordLowerCase))
					proposals.add(new ContentProposal(path.substring(word.length()), name, null, file));
			}
		}
		return (IContentProposal[]) proposals.toArray(new IContentProposal[proposals.size()]);
	}

	private Object[] filterIFiles(final Object[] resources) {
		List result = new ArrayList(Arrays.asList(resources));
		for (Iterator it = result.iterator(); it.hasNext(); ) {
			if (it.next() instanceof IFile)
				it.remove();
		}
		return result.toArray();
	}

	private File[] getFiles(File parent) {
		File[] files = null;
		files = (File[]) filesCache.get(parent);
		if (files == null) {
			if (parent != null) {
				files = parent.listFiles(new FileFilter() {
					public boolean accept(File file) {
						return fShowFiles && file.isFile() || file.isDirectory();
					}
				});
			}
			if (files == null)
				files = new File[0];
			Arrays.sort(files, new Comparator() {
				public int compare(Object o1, Object o2) {
					File f1 = (File) o1;
					File f2 = (File) o2;
					int result = f1.compareTo(f2);
					result = result < 0 ? -1 : result > 0 ? 1 : 0;
					if (f1.isDirectory() && f2.isFile())
						result = -1;
					else if (f1.isFile() && f2.isDirectory())
						result = 1;
					return result;
				}
			});
			filesCache.put(parent, files);
		}
		return files;
	}

	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		fSelectionChangedListeners.add(listener);
	}

	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		fSelectionChangedListeners.remove(listener);
	}

	private void propagateSelectionEvent(SelectionChangedEvent event) {
		if (!fSelectionChangedListeners.isEmpty()) {
			SelectionChangedEvent newEvent = new SelectionChangedEvent(this, event.getSelection());
			Object[] listeners = fSelectionChangedListeners.getListeners();
			for (int i = 0; i < listeners.length; i++) {
				((ISelectionChangedListener) listeners[i]).selectionChanged(newEvent);
			}
		}
	}

	public ISelection getSelection() {
		return null;
	}

	public void setSelection(ISelection selection) {
	}
}
