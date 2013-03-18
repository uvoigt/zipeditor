/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.actions;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.views.navigator.ResourceSorter;

import zipeditor.ZipEditorPlugin;

public abstract class DialogAction extends ViewerAction {
	private class FileSystemContentProvider implements ITreeContentProvider {
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof File[])
				return (Object[]) parentElement;
			else if (parentElement instanceof File) {
				File file = (File) parentElement;
				if (file.isDirectory()) {
					File[] files = file.listFiles();
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
		}
	};
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
	};
	
	private class FileSorter extends ViewerSorter {
		public int category(Object element) {
			return ((File) element).isDirectory() ? -1 : 1;
		}
	};
	
	private class FileFilter extends ViewerFilter {
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			return element instanceof IFolder || element instanceof IProject ||
					element instanceof File && ((File) element).isDirectory();
		}
	};
	
	private class FileDialog extends Dialog implements ISelectionChangedListener {
		private class FilterArea extends Composite implements FocusListener, ModifyListener {
			private class RefreshJob extends UIJob {
				public RefreshJob(Display display) {
					super(display, "Refresh"); //$NON-NLS-1$
				}

				public IStatus runInUIThread(IProgressMonitor monitor) {
					if (!fText.isDisposed()) {
						setPattern(fText.getText());
						selectMatches();
					}
					return Status.OK_STATUS;
				}
			}

			private TreeViewer fViewer;
			private Color fGrayColor;
			private final String fEmptyText = ActionMessages.getString("DialogAction.3"); //$NON-NLS-1$
			private final Text fText;
            private StringMatcher[] fMatchers;
			private final RefreshJob fRefreshJob = new RefreshJob(getDisplay());

			public FilterArea(Composite parent, String labelText) {
				super(parent, SWT.NONE);
				setLayout(new GridLayout(2, false));
				setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				Label label = new Label(this, SWT.LEFT);
				label.setText(labelText);
				fText = new Text(this, SWT.LEFT | SWT.BORDER);
				fText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				fText.setText(fEmptyText);
				fText.setForeground(getGrayColor());
				fText.addFocusListener(this);
				fText.addModifyListener(this);
			}

			private void setPattern(String patternString) {
				if (patternString == null || patternString.trim().length() == 0 || fEmptyText.equals(patternString)) {
					fMatchers = null;
				}
				else {
					String[] strings = patternString.split(","); //$NON-NLS-1$
					fMatchers = new StringMatcher[strings.length];
					for (int i = 0; i < strings.length; i++) {
						fMatchers[i] = new StringMatcher(strings[i].trim(), true, false);
					}
				}
			}

			private Color getGrayColor() {
				if (fGrayColor == null)
					fGrayColor = new Color(getDisplay(), 160, 160, 160);
				return fGrayColor;
			}
			
			public void dispose() {
				fGrayColor.dispose();
				super.dispose();
			}

			public void focusLost(FocusEvent e) {
				if (fText.getText().length() == 0 || fEmptyText.equals(fText.getText())) {
					fText.setForeground(getGrayColor());
					fText.setText(fEmptyText);
				}
				modifyText(null);
			}
			
			public void focusGained(FocusEvent e) {
				if (fEmptyText.equals(fText.getText())) {
					fText.setForeground(getForeground());
					fText.setText(""); //$NON-NLS-1$
				}
			}
			
			public void modifyText(ModifyEvent e) {
				fRefreshJob.cancel();
				fRefreshJob.schedule(500);
			}
		
			private void selectMatches() {
				if (fMatchers != null) {
					Tree tree = fViewer.getTree();
					List selection = new ArrayList();
					findElements(tree.getItems(), selection);
					fViewer.setSelection(new StructuredSelection(selection), true);
				} else {
					fViewer.setSelection(StructuredSelection.EMPTY);
				}
			}

			private Object findElements(TreeItem items[], List selection) {
				ILabelProvider labelProvider = (ILabelProvider) fViewer.getLabelProvider();
				for (int i = 0; i < items.length; i++) {
					Object element = items[i].getData();
					if (!(element instanceof IContainer) && (!(element instanceof File) || !((File) element).isDirectory())) {
						for (int j = 0; j < fMatchers.length; j++) {
							if (fMatchers[j].match(labelProvider.getText(element)))
								selection.add(element);
						}
					}
					findElements(items[i].getItems(), selection);
				}

				return null;
			}

			protected void setViewer(TreeViewer viewer) {
				fViewer = viewer;
				fViewer.setUseHashlookup(true);
			}
		}

		private TreeViewer fWorkspaceViewer;
		private TreeViewer fFileSystemViewer;
		private Label fStatusLabel;
		private List fWorkspaceSelection;
		private List fFileSystemSelection;
		private String fText;
		private boolean fMultiSelection;
		private boolean fShowFiles;
		private String fInitialSelection;

		private FileDialog(String text) {
			super(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
			setShellStyle(getShellStyle() | SWT.SHELL_TRIM);
			fText = text;
		}
		
		protected void configureShell(Shell newShell) {
			super.configureShell(newShell);
			newShell.setText(fText);
		}

		protected Control createDialogArea(Composite parent) {
			Composite control = (Composite) super.createDialogArea(parent);
			fWorkspaceViewer = createWorkspaceArea(control);
			fFileSystemViewer = createFileSystemArea(control);
			fWorkspaceViewer.addSelectionChangedListener(this);
			fFileSystemViewer.addSelectionChangedListener(this);
			ISelectionChangedListener listener = new ISelectionChangedListener() {
				public void selectionChanged(SelectionChangedEvent event) {
					if (fMultiSelection)
						return;
					fWorkspaceViewer.removeSelectionChangedListener(this);
					fFileSystemViewer.removeSelectionChangedListener(this);
					(event.getSource() == fFileSystemViewer ? fWorkspaceViewer : fFileSystemViewer).
							setSelection(StructuredSelection.EMPTY);
					fWorkspaceViewer.addSelectionChangedListener(this);
					fFileSystemViewer.addSelectionChangedListener(this);
				}
			};
			fWorkspaceViewer.addSelectionChangedListener(listener);
			fFileSystemViewer.addSelectionChangedListener(listener);
			
			GridData data = (GridData) control.getLayoutData();
			data.widthHint = convertWidthInCharsToPixels(80);
			
			fStatusLabel = new Label(control, SWT.LEFT | SWT.WRAP);
			fStatusLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			
			applyDialogFont(control);
			setInitialSelection();
			return control;
		}

		private void setInitialSelection() {
			if (fInitialSelection == null)
				return;
			fFileSystemViewer.setSelection(new StructuredSelection(new File(fInitialSelection)), true);
			try {
				fWorkspaceViewer.setSelection(new StructuredSelection(ResourcesPlugin.getWorkspace().
						getRoot().getFile(new Path(fInitialSelection))), true);
			} catch (Exception ignore) {
			}
		}

		private TreeViewer createWorkspaceArea(Composite parent) {
			Composite composite = new Composite(parent, SWT.NONE);
			composite.setLayout(new GridLayout(1, true));
			composite.setLayoutData(new GridData(GridData.FILL_BOTH));
			FilterArea filterArea = null;
			if (fUseFilter)
				filterArea = new FilterArea(composite, ActionMessages.getString("DialogAction.0")); //$NON-NLS-1$
			TreeViewer viewer = new TreeViewer(composite, SWT.BORDER | (fMultiSelection ? SWT.MULTI : SWT.SINGLE));
			if (filterArea != null)
				filterArea.setViewer(viewer);
			viewer.setContentProvider(new WorkbenchContentProvider());
			viewer.setLabelProvider(new WorkbenchLabelProvider());
			viewer.setSorter(new ResourceSorter(ResourceSorter.NAME));
			if (!fShowFiles)
				viewer.addFilter(new FileFilter());
			viewer.setInput(ResourcesPlugin.getWorkspace().getRoot());
			GridData data = new GridData(GridData.FILL_BOTH);
			data.heightHint = convertHeightInCharsToPixels(10);
			viewer.getControl().setLayoutData(data);
			return viewer;
		}

		private TreeViewer createFileSystemArea(Composite parent) {
			Composite composite = new Composite(parent, SWT.NONE);
			composite.setLayout(new GridLayout(1, true));
			composite.setLayoutData(new GridData(GridData.FILL_BOTH));
			FilterArea filterArea = null;
			if (fUseFilter)
				filterArea = new FilterArea(composite, ActionMessages.getString("DialogAction.1")); //$NON-NLS-1$
			TreeViewer viewer = new TreeViewer(composite, SWT.BORDER | (fMultiSelection ? SWT.MULTI : SWT.SINGLE));
			if (filterArea != null)
				filterArea.setViewer(viewer);
			viewer.setContentProvider(new FileSystemContentProvider());
			viewer.setLabelProvider(new FileSystemLabelProvider());
			viewer.setSorter(new FileSorter());
			if (!fShowFiles)
				viewer.addFilter(new FileFilter());
			viewer.setInput(File.listRoots());
			GridData data = new GridData(GridData.FILL_BOTH);
			data.heightHint = convertHeightInCharsToPixels(10);
			viewer.getControl().setLayoutData(data);
			return viewer;
		}
		
		protected void okPressed() {
			fWorkspaceSelection = toList(((IStructuredSelection) fWorkspaceViewer.getSelection()));
			fFileSystemSelection = toList(((IStructuredSelection) fFileSystemViewer.getSelection()));
			super.okPressed();
		}

		public String[] getFileNames() {
			List allNames = new ArrayList();
			if (fWorkspaceSelection != null)
				allNames.addAll(fWorkspaceSelection);
			if (fFileSystemSelection != null)
				allNames.addAll(fFileSystemSelection);
			return (String[]) allNames.toArray(new String[allNames.size()]);
		}
		
		private List toList(IStructuredSelection selection) {
			List list = selection.toList();
			for (int i = 0; i < list.size(); i++) {
				Object element = list.get(i);
				if (element instanceof IResource)
					list.set(i, ((IResource) element).getLocation().toFile().getAbsolutePath());
				else if (element instanceof File)
					list.set(i, ((File) element).getAbsolutePath());
			}
			return list;
		}

		public void setMultiSelection(boolean multiSelection) {
			fMultiSelection = multiSelection;
		}

		public void setShowFiles(boolean showFiles) {
			fShowFiles = showFiles;
		}

		public void setSelection(String path) {
			fInitialSelection = path;
		}
		
		public void selectionChanged(SelectionChangedEvent event) {
			updateStatusText();
		}

		private void updateStatusText() {
			int wsSize = ((IStructuredSelection) fWorkspaceViewer.getSelection()).size();
			int fsSize = ((IStructuredSelection) fFileSystemViewer.getSelection()).size();
			fStatusLabel.setText(ActionMessages.getFormattedString("DialogAction.2", //$NON-NLS-1$
					new Object[] { new Integer(wsSize), new Integer(fsSize) }));
		}
	}

	private boolean fUseFilter;

	protected DialogAction(String text, StructuredViewer viewer, boolean useFilter) {
		super(text, viewer);
		fUseFilter = useFilter;
	}

	protected String[] openDialog(String text, String path, boolean multiSelection, boolean showFiles) {
		FileDialog dialog = new FileDialog(text);
		dialog.setMultiSelection(multiSelection);
		dialog.setShowFiles(showFiles);
		dialog.setSelection(path);
		dialog.open();
		return dialog.getFileNames();
	}
}
