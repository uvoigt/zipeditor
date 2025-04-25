/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.actions;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import zipeditor.ZipEditorPlugin;
import zipeditor.preferences.PreferenceUtils;

public abstract class DialogAction extends ViewerAction {

	private boolean zstdCompression = false;
	
	private class FileDialog extends Dialog implements ISelectionChangedListener {
		private FileSystemChooseControl fWorkspaceViewer;
		private FileSystemChooseControl fFileSystemViewer;
		private Label fStatusLabel;
		private List fWorkspaceSelection;
		private List fFileSystemSelection;
		private String fText;
		private boolean fMultiSelection;
		private boolean fShowFiles;
		private File fInitialSelection;
		private boolean fAllowZstd;

		private FileDialog(String text, boolean allowZstd) {
			super(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
			fAllowZstd = allowZstd;
			setShellStyle(getShellStyle() | SWT.SHELL_TRIM);
			fText = text;
		}
		
		protected void configureShell(Shell newShell) {
			super.configureShell(newShell);
			newShell.setText(fText);
		}

		protected Control createDialogArea(Composite parent) {
			Composite control = (Composite) super.createDialogArea(parent);
			SashForm sashForm = new SashForm(control, SWT.VERTICAL);
			sashForm.setLayoutData(new GridData(GridData.FILL, SWT.FILL, true, true));
			fWorkspaceViewer = createWorkspaceArea(sashForm);
			fFileSystemViewer = createFileSystemArea(sashForm);
			fWorkspaceViewer.addSelectionChangedListener(this);
			fFileSystemViewer.addSelectionChangedListener(this);
			
			GridData data = (GridData) control.getLayoutData();
			data.widthHint = convertWidthInCharsToPixels(80);
			data.heightHint = convertWidthInCharsToPixels(80);

			fStatusLabel = new Label(control, SWT.LEFT | SWT.WRAP);
			fStatusLabel.setLayoutData(new GridData(GridData.FILL, SWT.END, true, false));
			
			if (fAllowZstd) {
				Button useZstdCheckbox = new Button(control, SWT.CHECK);
				useZstdCheckbox.setText("Use Zstd Compression"); //$NON-NLS-1$
				zstdCompression = PreferenceUtils.isZstdDefaultCompression();
				useZstdCheckbox.setSelection(zstdCompression);
				useZstdCheckbox.addSelectionListener(new SelectionAdapter() {
					
					@Override
					public void widgetSelected(SelectionEvent e) {
						zstdCompression = useZstdCheckbox.getSelection();			
					}
					
				});
			}
			applyDialogFont(control);
			return control;
		}

		public void create() {
			super.create();
			setInitialSelection();
			updateStatusText();
		}

		private void setInitialSelection() {
			if (fInitialSelection == null)
				return;
			List fileSelection = new ArrayList(1);
			IResource[] files = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(fInitialSelection.toURI());
			if (files.length == 0)
				files = ResourcesPlugin.getWorkspace().getRoot().findContainersForLocationURI(fInitialSelection.toURI());
			if (files.length > 0) {
				fileSelection.add(0, files[0]);
				fWorkspaceViewer.setFileSelection(fileSelection);
			} else {
				fileSelection.add(fInitialSelection);
				fFileSystemViewer.setFileSelection(fileSelection);
			}
		}

		private FileSystemChooseControl createWorkspaceArea(Composite parent) {
			FileSystemChooseControl control = new FileSystemChooseControl(parent, true, fShowFiles, fMultiSelection);
			control.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			return control;
		}

		private FileSystemChooseControl createFileSystemArea(Composite parent) {
			FileSystemChooseControl control = new FileSystemChooseControl(parent, false, fShowFiles, fMultiSelection);
			control.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			return control;
		}
		
		protected void okPressed() {
			fWorkspaceSelection = fWorkspaceViewer.getFileSelection(false);
			fFileSystemSelection = fFileSystemViewer.getFileSelection(false);
			super.okPressed();
		}

		public File[] getFiles() {
			List allNames = new ArrayList();
			if (fWorkspaceSelection != null) {
				for (int i = 0; i < fWorkspaceSelection.size(); i++) {
					allNames.add(((IResource) fWorkspaceSelection.get(i)).getLocation().toFile());
				}
			}
			if (fFileSystemSelection != null)
				allNames.addAll(fFileSystemSelection);
			return (File[]) allNames.toArray(new File[allNames.size()]);
		}
		
		public void setMultiSelection(boolean multiSelection) {
			fMultiSelection = multiSelection;
		}

		public void setShowFiles(boolean showFiles) {
			fShowFiles = showFiles;
		}

		public void setSelection(File path) {
			fInitialSelection = path;
		}
		
		public void selectionChanged(SelectionChangedEvent event) {
			if (!fMultiSelection) {
				FileSystemChooseControl control = event.getSource() == fFileSystemViewer ? fWorkspaceViewer : fFileSystemViewer;
				control.removeSelectionChangedListener(this);
				control.setFileSelection(null);
				control.addSelectionChangedListener(this);
			}
			updateStatusText();
		}

		private void updateStatusText() {
			int wsSize = fWorkspaceViewer.getFileSelection(true).size();
			int fsSize = fFileSystemViewer.getFileSelection(true).size();
			fStatusLabel.setText(ActionMessages.getFormattedString("DialogAction.0", //$NON-NLS-1$
					new Object[] { new Integer(wsSize), new Integer(fsSize) }));
			if (getShell().isVisible())
				getButton(IDialogConstants.OK_ID).setEnabled(wsSize + fsSize > 0);
		}

		protected IDialogSettings getDialogBoundsSettings() {
			String name = "dialog." + getClass().getName(); //$NON-NLS-1$
			IDialogSettings section = ZipEditorPlugin.getDefault().getDialogSettings().getSection(name);
			if (section == null)
				section = ZipEditorPlugin.getDefault().getDialogSettings().addNewSection(name);
			return section;
		}
	}

	protected DialogAction(String text, StructuredViewer viewer, boolean useFilter) {
		super(text, viewer);
	}

	protected File[] openDialog(String text, File path, boolean multiSelection, boolean showFiles, boolean allowZstd) {
		FileDialog dialog = new FileDialog(text, allowZstd);
		dialog.setMultiSelection(multiSelection);
		dialog.setShowFiles(showFiles);
		dialog.setSelection(path);
		int result = dialog.open();
		return result == Window.OK ? dialog.getFiles() : null;
	}
	
	public boolean isZstdCompression() {
		return zstdCompression;
	}
}
