/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.actions;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.ImageDescriptor;
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
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
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
		public int compare(Viewer viewer, Object e1, Object e2) {
			if (e1 instanceof File && e2 instanceof File) {
				File f1 = (File) e1;
				File f2 = (File) e2;
				if (f1.isDirectory())
					return -1;
				if (f2.isDirectory())
					return 1;
				e1 = f1.getName();
				e2 = f2.getName();
			}
			return super.compare(viewer, e1, e2);
		}
	};
	
	private class FileFilter extends ViewerFilter {
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			return element instanceof IFolder || element instanceof IProject ||
					element instanceof File && ((File) element).isDirectory();
		}
	};
	
	private class FileDialog extends Dialog implements ISelectionChangedListener {
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
			Label label = new Label(parent, SWT.LEFT);
			label.setText(ActionMessages.getString("DialogAction.0")); //$NON-NLS-1$
			label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			TreeViewer viewer = new TreeViewer(parent, SWT.BORDER | (fMultiSelection ? SWT.MULTI : SWT.SINGLE));
			viewer.setContentProvider(new WorkbenchContentProvider());
			viewer.setLabelProvider(new WorkbenchLabelProvider());
			viewer.setSorter(new ResourceSorter(ResourceSorter.NAME));
			if (!fShowFiles)
				viewer.addFilter(new FileFilter());
			viewer.setInput(ResourcesPlugin.getWorkspace().getRoot());
			GridData data = new GridData(GridData.FILL_HORIZONTAL);
			data.heightHint = convertHeightInCharsToPixels(10);
			viewer.getControl().setLayoutData(data);
			return viewer;
		}

		private TreeViewer createFileSystemArea(Composite parent) {
			Label label = new Label(parent, SWT.LEFT);
			label.setText(ActionMessages.getString("DialogAction.1")); //$NON-NLS-1$
			label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			TreeViewer viewer = new TreeViewer(parent, SWT.BORDER | (fMultiSelection ? SWT.MULTI : SWT.SINGLE));
			viewer.setContentProvider(new FileSystemContentProvider());
			viewer.setLabelProvider(new FileSystemLabelProvider());
			viewer.setSorter(new FileSorter());
			if (!fShowFiles)
				viewer.addFilter(new FileFilter());
			viewer.setInput(File.listRoots());
			GridData data = new GridData(GridData.FILL_HORIZONTAL);
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
	};

	protected DialogAction(String text, StructuredViewer viewer) {
		super(text, viewer);
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
