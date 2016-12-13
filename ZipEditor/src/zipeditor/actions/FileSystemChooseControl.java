/*
 * (c) Copyright 2002, 2014 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.actions;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
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
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;

import zipeditor.ZipEditorPlugin;

public class FileSystemChooseControl extends Composite implements ISelectionProvider {
	private class FileSystemContentProvider implements ITreeContentProvider, ILazyTreeContentProvider, ILazyContentProvider {
		private boolean fReturnFiles;
		private Viewer fViewer;
		private File[] fTableFiles;

		private FileSystemContentProvider(Viewer viewer, boolean returnFiles) {
			fViewer = viewer;
			fReturnFiles = returnFiles;
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

		private File[] getFiles(File file) {
			File[] files = file == null ? null : file.listFiles(new FileFilter() {
				public boolean accept(File file) {
					return fReturnFiles && file.isFile() || file.isDirectory();
				}
			});
			if (files == null)
				return new File[0];
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
			return files;
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
			if (!fReturnFiles) {
				List result = new ArrayList(Arrays.asList(children));
				for (Iterator it = result.iterator(); it.hasNext(); ) {
					if (it.next() instanceof IFile)
						it.remove();
				}
				children = result.toArray();
			}
			return children;
		}
	}
	private class FilterArea extends Composite implements FocusListener, KeyListener, DisposeListener, TraverseListener {
		private Color fGrayColor;
		private final String fEmptyText = ActionMessages.getString("FileSystemChooseControl.0"); //$NON-NLS-1$
		private final Text fText;

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

			new ContentAssistCommandAdapter(fText, new TextContentAdapter(),
					new IContentProposalProvider() {
						public IContentProposal[] getProposals(String contents, int position) {
							return doGetProposals(contents, position);
						}
					}, ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS, new char[0], true);
			addDisposeListener(this);
		}

		public void keyTraversed(TraverseEvent e) {
			if (e.detail == SWT.TRAVERSE_RETURN)
				e.doit = false;
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

	private FilterArea fText;
	private TreeViewer fTree;
	private TableViewer fTable;
	private final Map fTableSelection = new HashMap();
	private boolean fInPathChange;
	private final boolean fUseWorkspace;
	private final boolean fShowFiles;
	private final boolean fMultiSelect;
	private final ListenerList fSelectionChangedListeners = new ListenerList();

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
		fTree.setContentProvider(fUseWorkspace ? (IContentProvider) new WorkbenchContentProvider(false) : new FileSystemContentProvider(fTree, false));
		fTree.setLabelProvider(fUseWorkspace ? (IBaseLabelProvider) new WorkbenchLabelProvider() : new FileSystemLabelProvider());
		fTree.setInput(fUseWorkspace ? (Object) ResourcesPlugin.getWorkspace().getRoot() : File.listRoots());
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
					setTableInput(((File) fTable.getInput()).getParentFile());
			}
		});
		fTable.setUseHashlookup(true);
		fTable.setContentProvider(fUseWorkspace ? (IContentProvider) new WorkbenchContentProvider(fShowFiles) : new FileSystemContentProvider(fTable, fShowFiles));
		fTable.setLabelProvider(fUseWorkspace ? (IBaseLabelProvider) new WorkbenchLabelProvider() : new FileSystemLabelProvider());
	}

	private void pathChanged(String path) {
		fInPathChange = true;
		if (fUseWorkspace) {
			IResource file = ResourcesPlugin.getWorkspace().getRoot().findMember(path);
			if (file.exists() && file instanceof IContainer) {
				fTree.setSelection(new StructuredSelection(file));
			} else {
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
					files = ((FileSystemContentProvider) fTable.getContentProvider()).getFiles((File) parentFile);
				else if (((File) parentFile).isFile())
					return;
			}
			fTable.setInput(parentFile);
			fTable.setItemCount(files.length);
			fTable.setSelection(StructuredSelection.EMPTY);
			if (!fInPathChange)
				fText.setText(fUseWorkspace ? ((IResource) parentFile).getFullPath().toString() : ((File) parentFile).getAbsolutePath());
		} else {
			fTable.setItemCount(0);
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
		List list = new ArrayList(1);
		if (!ignoreTextField)
			list.add(new File(fText.getText()));
		return list;
	}

	public void setFileSelection(List files) {
		fTree.setSelection(StructuredSelection.EMPTY);
		fTable.setSelection(StructuredSelection.EMPTY);
		if (files != null) {
			fTree.setExpandedElements(new Object[0]);
			List treeSelection = new ArrayList();
			List tableSelection = new ArrayList();
			for (int i = 0; i < files.size(); i++) {
				File file = (File) files.get(i);
				if (file.isDirectory())
					treeSelection.add(file);
				else if (file.isFile())
					tableSelection.add(file);
			}
			fTree.setSelection(new StructuredSelection(treeSelection), true);
			if (treeSelection.isEmpty() && !tableSelection.isEmpty()) {
				File input = (File) fTable.getInput();
				File parentFile = ((File) tableSelection.get(0)).getParentFile();
				if (input == null || !input.equals(parentFile))
					setTableInput(parentFile);
			}
			fTableSelection.clear();
			for (int i = 0; i < tableSelection.size(); i++) {
				fTableSelection.put(tableSelection.get(i), null);
			}
			if (files.size() == 1 && ((File) files.get(0)).isDirectory())
				setTableInput(files.get(0));
			if (files.size() == 1 && ((File) files.get(0)).isFile())
				fText.setText(((File) files.get(0)).getAbsolutePath());
		} else {
			setTableInput(null);
		}
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
				IResource parent = ResourcesPlugin.getWorkspace().getRoot().findMember(word);
				if (!parent.exists())
					parent = parent.getParent();
				if (parent instanceof IContainer) {
					try {
						files = ((IContainer) parent).members();
					} catch (CoreException e) {
						ZipEditorPlugin.log(e);
					}
				}
			} else {
				File parent = new File(word);
				if (!parent.exists())
					parent = parent.getParentFile();
				if (parent != null)
					files = parent.listFiles();
			}
		}
		if (files != null) {
			String wordLowerCase = word.toLowerCase();
			for (int i = 0; i < files.length; i++) {
				String path = fUseWorkspace ? ((IResource) files[i]).getFullPath().toString() : ((File) files[i]).getPath();
				String name = fUseWorkspace ? ((IResource) files[i]).getName() : ((File) files[i]).getName();
				if (name.length() == 0)
					name = path;
				if (path.toLowerCase().startsWith(wordLowerCase))
					proposals.add(new ContentProposal(path.substring(word.length()), name, null));
			}
		}
		return (IContentProposal[]) proposals.toArray(new IContentProposal[proposals.size()]);
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
