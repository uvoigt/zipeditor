/*
 * (c) Copyright 2002, 2016 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.search;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.text.ITextSelection;
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
import org.eclipse.ui.WorkbenchEncoding;

import zipeditor.ZipEditor;
import zipeditor.ZipEditorPlugin;
import zipeditor.actions.FileSystemChooseControl;

public class ZipSearchPage extends DialogPage implements ISearchPage {

	public static final String ID = "zipeditor.search.ZipSearchPage"; //$NON-NLS-1$

	private ISearchPageContainer fContainer;
	private Combo fSearchText;
	private Combo fNodeNamePatterns;
	private Button fCaseSensitiveButton;
	private Combo fEncodingCombo;

	private Button fRadioWorkspace;
	private Button fRadioSelected;
	private Button fRadioFileSystem;
	private FileSystemChooseControl fFileSystemChooser;

	private final String fDefaultEncoding = WorkbenchEncoding.getWorkbenchDefaultEncoding();

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

		Composite left = new Composite(control, SWT.NONE);
		layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		left.setLayout(layout);
		left.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		Label label = new Label(left, SWT.LEFT);
		label.setText(SearchMessages.getString("ZipSearchPage.1")); //$NON-NLS-1$
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		fSearchText = new Combo(left, SWT.NONE);
		fSearchText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		fSearchText.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				for (int i = 0; i < fPreviousSearches.size(); i++) {
					ZipSearchOptions options = (ZipSearchOptions) fPreviousSearches.get(i);
					if (options.getPattern().equals(fSearchText.getText())) {
						fillWidgets(options);
						fFileSystemChooser.setFileSelection(options.getScope() == ZipSearchOptions.SCOPE_FILESYSTEM ? options.getPath() : null);
						fFileSystemChooser.setEnabled(fRadioFileSystem.getSelection());
						break;
					}
				}
			}
		});

		Composite middle = new Composite(control, SWT.NONE);
		layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		middle.setLayout(layout);

		label = new Label(middle, SWT.LEFT);
		label.setText(SearchMessages.getString("ZipSearchPage.6")); //$NON-NLS-1$
		label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

		fEncodingCombo = new Combo(middle, SWT.NONE);
		fEncodingCombo.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));

		fCaseSensitiveButton = new Button(control, SWT.CHECK);
		fCaseSensitiveButton.setText(SearchMessages.getString("ZipSearchPage.0")); //$NON-NLS-1$
		fCaseSensitiveButton.setLayoutData(new GridData(SWT.END, SWT.END, false, false));

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
		if (fContainer.getSelection() instanceof ITextSelection) {
			String text = ((ITextSelection) fContainer.getSelection()).getText();
			if (text != null && text.length() > 0)
				fSearchText.setText(text);
		}
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

	private void fillWidgets(ZipSearchOptions options) {
		fNodeNamePatterns.setText(options.getNodeNamePattern() != null ? options.getNodeNamePattern() : ""); //$NON-NLS-1$
		fEncodingCombo.setText(options.getEncoding() != null ? options.getEncoding() : fDefaultEncoding);
		fCaseSensitiveButton.setSelection(options.isCaseSensitive());

		if (fRadioSelected.isEnabled())
			fRadioSelected.setSelection(options.getScope() == ZipSearchOptions.SCOPE_SELECTED);
		fRadioWorkspace.setSelection(options.getScope() == ZipSearchOptions.SCOPE_WORKSPACE ||
				!fRadioSelected.isEnabled() && options.getScope() == ZipSearchOptions.SCOPE_SELECTED);
		fRadioFileSystem.setSelection(options.getScope() == ZipSearchOptions.SCOPE_FILESYSTEM);
	}

	public boolean performAction() {
		int scope = fRadioWorkspace.getSelection() ? ZipSearchOptions.SCOPE_WORKSPACE : fRadioSelected.getSelection() ? ZipSearchOptions.SCOPE_SELECTED : ZipSearchOptions.SCOPE_FILESYSTEM;
		String encoding = fEncodingCombo.getText();
		if (encoding.length() == 0)
			encoding = fDefaultEncoding;
		ZipSearchOptions options = new ZipSearchOptions(fNodeNamePatterns.getText(), fSearchText.getText(), encoding, fCaseSensitiveButton.getSelection(), scope);

		List elements = new ArrayList();
		if (scope == ZipSearchOptions.SCOPE_WORKSPACE) {
			elements.add(ResourcesPlugin.getWorkspace().getRoot());
			options.setElements(elements);
		} else if (scope == ZipSearchOptions.SCOPE_SELECTED) {
			ZipEditor zipEditor = getActiveEditor();
			if (zipEditor != null) {
				File file = zipEditor.getModel().getZipPath();
				elements.add(file);
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
		ZipSearchQuery searchQuery = new ZipSearchQuery(options, elements);
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
		List encodings = WorkbenchEncoding.getDefinedEncodings();
		if (section != null) {
			String[] strings = section.getArray("nodenames"); //$NON-NLS-1$
			if (strings != null)
				namePatterns.addAll(Arrays.asList(strings));
			strings = section.getArray("encodings"); //$NON-NLS-1$
			if (strings != null)
				encodings.addAll(Arrays.asList(strings));

			int historySize = Math.min(section.getInt("historySize"), 14); //$NON-NLS-1$
			while (--historySize >= 0) {
				IDialogSettings h = section.getSection("history" + historySize); //$NON-NLS-1$
				boolean caseSensitive = h.getBoolean("case"); //$NON-NLS-1$
				String nodeNamePattern = h.get("nodename"); //$NON-NLS-1$
				String pattern = h.get("pattern"); //$NON-NLS-1$
				String encoding = h.get("encoding"); //$NON-NLS-1$
				int scope = h.getInt("scope"); //$NON-NLS-1$
				String[] path = h.getArray("path"); //$NON-NLS-1$
				options = new ZipSearchOptions(nodeNamePattern, pattern, encoding, caseSensitive, scope);
				if (path != null) {
					List files = new ArrayList();
					for (int i = 0; i < path.length; i++) {
						files.add(new File(path[i]));
					}
					options.setPath(files);
				}
				fPreviousSearches.add(0, options);
				fSearchText.add(pattern, 0);
				if (nodeNamePattern != null && nodeNamePattern.length() > 0 && !namePatterns.contains(nodeNamePattern))
					namePatterns.add(nodeNamePattern);
				if (encoding != null && encoding.length() > 0 && !encodings.contains(encoding))
					encodings.add(encoding);
			}
		}
		fNodeNamePatterns.setItems((String[]) namePatterns.toArray(new String[namePatterns.size()]));
		fEncodingCombo.setItems((String[]) encodings.toArray(new String[encodings.size()]));
		if (options == null)
			options = new ZipSearchOptions(null, "", null, false, ZipSearchOptions.SCOPE_WORKSPACE); //$NON-NLS-1$

		boolean isScopeSelectedPossible = isScopeSelectedPossible();
		if (options.getScope() == ZipSearchOptions.SCOPE_SELECTED && !isScopeSelectedPossible)
			options.setScope(ZipSearchOptions.SCOPE_WORKSPACE);
		fRadioSelected.setEnabled(isScopeSelectedPossible);
	
		fSearchText.setText(options.getPattern());
		fillWidgets(options);
	}

	private boolean isScopeSelectedPossible() {
		IStructuredSelection selection = fContainer.getSelection() instanceof IStructuredSelection ? (IStructuredSelection) fContainer.getSelection() : null;
		Object firstElement = selection != null && selection.getFirstElement() != null ? selection.getFirstElement() : null;
		return firstElement instanceof IResource || firstElement instanceof File || getActiveEditor() != null;
	}

	protected void saveDialogSettings() {
		IDialogSettings section = ZipEditorPlugin.getDefault().getDialogSettings().addNewSection("searchPage"); //$NON-NLS-1$

		section.put("nodenames", getItems(fNodeNamePatterns, null, null)); //$NON-NLS-1$
		String[] items = getItems(fEncodingCombo, WorkbenchEncoding.getDefinedEncodings(), fDefaultEncoding);
		if (items.length > 0)
			section.put("encodings", items); //$NON-NLS-1$

		section.put("historySize", fPreviousSearches.size()); //$NON-NLS-1$
		for (int i = 0; i < fPreviousSearches.size(); i++) {
			IDialogSettings h = section.addNewSection("history" + i); //$NON-NLS-1$
			ZipSearchOptions options = (ZipSearchOptions) fPreviousSearches.get(i);
			if (options.getNodeNamePattern() != null && options.getNodeNamePattern().length() > 0)
				h.put("nodename", options.getNodeNamePattern()); //$NON-NLS-1$
			h.put("pattern", options.getPattern()); //$NON-NLS-1$
			if (options.getEncoding() != null && options.getEncoding().length() > 0 && !fDefaultEncoding.equalsIgnoreCase(options.getEncoding()))
				h.put("encoding", options.getEncoding()); //$NON-NLS-1$
			if (options.isCaseSensitive())
				h.put("case", true); //$NON-NLS-1$
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

	private String[] getItems(Combo combo, List exceptList, String exceptItem) {
		List items = new ArrayList(Arrays.asList(combo.getItems()));
		String text = combo.getText();
		if (text.length() > 0 && !items.contains(text))
			items.add(0, text);
		if (exceptList != null)
			items.removeAll(exceptList);
		if (exceptItem != null)
			items.remove(exceptItem);
		return (String[]) items.toArray(new String[items.size()]);
	}

	public void setVisible(boolean visible) {
		fSearchText.setFocus();
		super.setVisible(visible);

		if (visible && !fPreviousSearches.isEmpty()) {
			ZipSearchOptions options = (ZipSearchOptions) fPreviousSearches.get(0);
			if (options.getPath() != null)
				fFileSystemChooser.setFileSelection(options.getPath());
		}
		if (!visible)
			fFileSystemChooser.setFileSelection(null);
	}
}
