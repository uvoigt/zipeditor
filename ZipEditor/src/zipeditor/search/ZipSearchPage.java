/*
 * (c) Copyright 2002, 2016 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.search;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.search.ui.ISearchPage;
import org.eclipse.search.ui.ISearchPageContainer;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

import zipeditor.ZipEditor;
import zipeditor.ZipEditorPlugin;
import zipeditor.actions.FileSystemChooseControl;

public class ZipSearchPage extends DialogPage implements ISearchPage {

	public static final String ID = "zipeditor.search.ZipSearchPage"; //$NON-NLS-1$

	private ISearchPageContainer fContainer;
	private Combo fSearchText;
	private Combo fNodeNamePatterns;
	private Button fCaseSensitiveButton;

	private Button fRadioWorkspace;
	private Button fRadioSelected;
	private Button fRadioFileSystem;
	private FileSystemChooseControl fFileSystemChooser;

	private final List fPreviousSearches = new ArrayList();

	public void createControl(Composite parent) {

		Composite control = new Composite(parent, SWT.None) {
			public void setLayoutData(Object layoutData) {
				super.setLayoutData(new GridData(GridData.FILL, SWT.FILL, true, true));
			}
		};
		GridLayout layout = new GridLayout(3, false);
		layout.horizontalSpacing = 10;
		control.setLayout(layout);

		Label label = new Label(control, SWT.LEFT);
		label.setText(SearchMessages.getString("ZipSearchPage.1")); //$NON-NLS-1$
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		fSearchText = new Combo(control, SWT.NONE);
		fSearchText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		fSearchText.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				for (int i = 0; i < fPreviousSearches.size(); i++) {
					ZipSearchOptions options = (ZipSearchOptions) fPreviousSearches.get(i);
					if (options.getPattern().equals(fSearchText.getText())) {
						fNodeNamePatterns.setText(options.getNodeNamePattern());
						fCaseSensitiveButton.setSelection(options.isCaseSensitive());
						fRadioFileSystem.setSelection(options.getScope() == ZipSearchOptions.SCOPE_FILESYSTEM);
						if (fRadioSelected.isEnabled())
							fRadioSelected.setSelection(options.getScope() == ZipSearchOptions.SCOPE_SELECTED);
						fRadioWorkspace.setSelection(options.getScope() == ZipSearchOptions.SCOPE_WORKSPACE);
						fFileSystemChooser.setFileSelection(options.getScope() == ZipSearchOptions.SCOPE_FILESYSTEM ? options.getPath() : null);
						fFileSystemChooser.setEnabled(fRadioFileSystem.getSelection());
						break;
					}
				}
			}
		});
		fCaseSensitiveButton = new Button(control, SWT.CHECK);
		fCaseSensitiveButton.setText(SearchMessages.getString("ZipSearchPage.0")); //$NON-NLS-1$

		label = new Label(control, SWT.LEFT);
		label.setText(SearchMessages.getString("ZipSearchPage.5")); //$NON-NLS-1$
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		fNodeNamePatterns = new Combo(control, SWT.NONE);
		fNodeNamePatterns.setLayoutData(new GridData(SWT.FILL, 0, true, false, 3, 1));

		Control scopeGroup = createScopeGroup(control);
		scopeGroup.setLayoutData(new GridData(GridData.FILL, SWT.FILL, true, true, 3, 1));

		Dialog.applyDialogFont(control);
		setControl(control);

		readDialogSettings();
		fFileSystemChooser.setEnabled(fRadioFileSystem.getSelection());
	}

	private Control createScopeGroup(Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		group.setLayout(new GridLayout(3, false));
		fRadioWorkspace = new Button(group, SWT.RADIO);
		fRadioWorkspace.setText(SearchMessages.getString("ZipSearchPage.2")); //$NON-NLS-1$
		fRadioSelected = new Button(group, SWT.RADIO);
		fRadioSelected.setText(SearchMessages.getString("ZipSearchPage.3")); //$NON-NLS-1$
		fRadioSelected.setLayoutData(new GridData());
		fRadioFileSystem = new Button(group, SWT.RADIO);
		fRadioFileSystem.setText(SearchMessages.getString("ZipSearchPage.4")); //$NON-NLS-1$

		fFileSystemChooser = new FileSystemChooseControl(group, false, true, true);
		fFileSystemChooser.setLayoutData(new GridData(GridData.FILL, SWT.FILL, true, true, 3, 1));

		SelectionAdapter radioListener = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fFileSystemChooser.setEnabled(fRadioFileSystem.getSelection());
			}
		};
		fRadioWorkspace.addSelectionListener(radioListener);
		fRadioSelected.addSelectionListener(radioListener);
		fRadioFileSystem.addSelectionListener(radioListener);

		return group;
	}

	public boolean performAction() {
		int scope = fRadioWorkspace.getSelection() ? ZipSearchOptions.SCOPE_WORKSPACE : fRadioSelected.getSelection() ? ZipSearchOptions.SCOPE_SELECTED : ZipSearchOptions.SCOPE_FILESYSTEM;
		ZipSearchOptions options = new ZipSearchOptions(fNodeNamePatterns.getText(), fSearchText.getText(), fCaseSensitiveButton.getSelection(), scope);

		List elements = new ArrayList();
		String fileName = null;
		if (scope == ZipSearchOptions.SCOPE_WORKSPACE) {
			elements.add(ResourcesPlugin.getWorkspace().getRoot());
			options.setElements(elements);
		} else if (scope == ZipSearchOptions.SCOPE_SELECTED) {
			ZipEditor zipEditor = getActiveEditor();
			if (zipEditor != null) {
				File file = zipEditor.getModel().getZipPath();
				elements.add(file);
				fileName = zipEditor.getTitle();
			} else if (fContainer.getSelection() != null && !fContainer.getSelection().isEmpty()) {
				elements.addAll(((IStructuredSelection) fContainer.getSelection()).toList());
			}
			options.setElements(elements);
		} else if (scope == ZipSearchOptions.SCOPE_FILESYSTEM) {
			final List fileSelection = fFileSystemChooser.getFileSelection(false);
			options.setPath(fileSelection);
			options.setElements(fileSelection);
			elements.addAll(fileSelection);
		}

		NewSearchUI.activateSearchResultView();
		ZipSearchQuery searchQuery = new ZipSearchQuery(options, elements, fileName);
		NewSearchUI.runQueryInBackground(searchQuery);

		fPreviousSearches.remove(options);
		fPreviousSearches.add(0, options);
		saveDialogSettings();
		return true;
	}

	private ZipEditor getActiveEditor() {
		IWorkbenchPart activePart = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart();
		return activePart instanceof ZipEditor ? (ZipEditor) activePart : null;
	}

	public void setContainer(ISearchPageContainer container) {
		fContainer = container;
	}

	protected void readDialogSettings() {
		IDialogSettings section = ZipEditorPlugin.getDefault().getDialogSettings().getSection("searchPage"); //$NON-NLS-1$
		ZipSearchOptions options = null;
		List namePatterns = new ArrayList();
		if (section != null) {
			int historySize = Math.min(section.getInt("historySize"), 14); //$NON-NLS-1$
			while (--historySize >= 0) {
				IDialogSettings h = section.getSection("history" + historySize); //$NON-NLS-1$
				boolean caseSensitive = h.getBoolean("case"); //$NON-NLS-1$
				String nodeNamePattern = h.get("nodename"); //$NON-NLS-1$
				String pattern = h.get("pattern"); //$NON-NLS-1$
				int scope = h.getInt("scope"); //$NON-NLS-1$
				String[] path = h.getArray("path"); //$NON-NLS-1$
				options = new ZipSearchOptions(nodeNamePattern, pattern, caseSensitive, scope);
				if (path != null) {
					List files = new ArrayList();
					for (int i = 0; i < path.length; i++) {
						files.add(new File(path[i]));
					}
					options.setPath(files);
				}
				fPreviousSearches.add(0, options);
				fSearchText.add(pattern, 0);
				if (!namePatterns.contains(nodeNamePattern))
					namePatterns.add(nodeNamePattern);
			}
		}
		fNodeNamePatterns.setItems((String[]) namePatterns.toArray(new String[namePatterns.size()]));
		if (options == null)
			options = new ZipSearchOptions("*", "", false, ZipSearchOptions.SCOPE_WORKSPACE); //$NON-NLS-1$ //$NON-NLS-2$

		boolean isScopeSelectedPossible = isScopeSelectedPossible();
		if (options.getScope() == ZipSearchOptions.SCOPE_SELECTED && !isScopeSelectedPossible)
			options.setScope(ZipSearchOptions.SCOPE_WORKSPACE);
		fRadioSelected.setEnabled(isScopeSelectedPossible);
	
		fSearchText.setText(options.getPattern());
		fNodeNamePatterns.setText(options.getNodeNamePattern());
		fCaseSensitiveButton.setSelection(options.isCaseSensitive());
		fRadioSelected.setSelection(options.getScope() == ZipSearchOptions.SCOPE_SELECTED);
		fRadioWorkspace.setSelection(options.getScope() == ZipSearchOptions.SCOPE_WORKSPACE);
		fRadioFileSystem.setSelection(options.getScope() == ZipSearchOptions.SCOPE_FILESYSTEM);
	}

	private boolean isScopeSelectedPossible() {
		IStructuredSelection selection = fContainer.getSelection() instanceof IStructuredSelection ? (IStructuredSelection) fContainer.getSelection() : null;
		Object firstElement = selection != null && selection.getFirstElement() != null ? selection.getFirstElement() : null;
		return firstElement instanceof IResource || firstElement instanceof File || getActiveEditor() != null;
	}

	protected void saveDialogSettings() {
		IDialogSettings section = ZipEditorPlugin.getDefault().getDialogSettings().addNewSection("searchPage"); //$NON-NLS-1$
		section.put("historySize", fPreviousSearches.size()); //$NON-NLS-1$
		for (int i = 0; i < fPreviousSearches.size(); i++) {
			IDialogSettings h = section.addNewSection("history" + i); //$NON-NLS-1$
			ZipSearchOptions options = (ZipSearchOptions) fPreviousSearches.get(i);
			h.put("nodename", options.getNodeNamePattern()); //$NON-NLS-1$
			h.put("pattern", options.getPattern()); //$NON-NLS-1$
			h.put("case", options.isCaseSensitive()); //$NON-NLS-1$
			h.put("scope", options.getScope()); //$NON-NLS-1$
			List files = options.getPath();
			if (files != null) {
				String[] path = new String[files.size()];
				for (int j = 0; j < path.length; j++) {
					path[j] = ((File) files.get(j)).getAbsolutePath();
				}
				h.put("path", path); //$NON-NLS-1$
			}
		}
    }

	public void setVisible(boolean visible) {
		fSearchText.setFocus();
		super.setVisible(visible);

		if (!fPreviousSearches.isEmpty()) {
			ZipSearchOptions options = (ZipSearchOptions) fPreviousSearches.get(0);
			if (options.getPath() != null)
				fFileSystemChooser.setFileSelection(options.getPath());
		}
	}
}
