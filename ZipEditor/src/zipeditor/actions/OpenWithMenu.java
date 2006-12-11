/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.actions;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SelectionStatusDialog;
import org.eclipse.ui.editors.text.EditorsUI;

import zipeditor.PreferenceConstants;
import zipeditor.PreferenceInitializer;
import zipeditor.Utils;
import zipeditor.ZipEditorPlugin;

public class OpenWithMenu extends ContributionItem {
	private class ExecutableSelectionDialog extends SelectionStatusDialog {
		private TableViewer fTableViewer;
		private Object fSelection;
		private String fLastFilterPath;
		private Set fExternalEditors;
		private Set fInternalEditors;
		private Button fBrowseButton;

		public ExecutableSelectionDialog(Shell parentShell, Object initialSelection) {
			super(parentShell);
			setShellStyle(getShellStyle() | SWT.RESIZE);
			fSelection = initialSelection;
			setTitle(ActionMessages.getString("OpenWithMenu.5")); //$NON-NLS-1$
		}

		protected Control createDialogArea(Composite parent) {
			Composite composite = (Composite) super.createDialogArea(parent);
			Label label = new Label(composite, SWT.LEFT);
			label.setText(ActionMessages.getString("OpenWithMenu.6")); //$NON-NLS-1$
			label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			label = new Label(composite, SWT.LEFT);
			label.setText(ActionMessages.getString("OpenWithMenu.7") + getFileResource().getName()); //$NON-NLS-1$
			label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
			createExternalEditorGroup(composite);
			return composite;
		}
		
		private Control createExternalEditorGroup(Composite parent) {
			Group group = new Group(parent, SWT.NONE);
			group.setText(ActionMessages.getString("OpenWithMenu.8")); //$NON-NLS-1$
			group.setLayout(new GridLayout());
			group.setLayoutData(new GridData(GridData.FILL_BOTH));
			Button[] buttons = createRadioButtons(group);
			fTableViewer = createTableViewer(group);
			
			fBrowseButton = new Button(group, SWT.PUSH);
			fBrowseButton.setText(ActionMessages.getString("OpenWithMenu.9")); //$NON-NLS-1$
			setButtonLayoutData(fBrowseButton);
			((GridData) fBrowseButton.getLayoutData()).horizontalAlignment |= GridData.HORIZONTAL_ALIGN_BEGINNING;
			fBrowseButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					handleBrowseButtonSelected();
				}
			});

			if (buttons[0].getText().equals(previousSelectedRadio)) {
				handleExternalButtonSelected(buttons[0]);
				buttons[0].setSelection(true);
			}
			if (buttons[1].getText().equals(previousSelectedRadio)) {
				handleInternalButtonSelected(buttons[1]);
				buttons[1].setSelection(true);
			}
			return group;
		}

		private Button[] createRadioButtons(Composite parent) {
			Button[] buttons = { null, null };
			Composite composite = new Composite(parent, SWT.NONE);
			composite.setLayout(new GridLayout(2, true));
			composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			Button button = buttons[0] = new Button(composite, SWT.RADIO);
			button.setText(ActionMessages.getString("OpenWithMenu.10")); //$NON-NLS-1$
			setButtonLayoutData(button);
			button.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					handleExternalButtonSelected((Button) e.widget);
				}
			});
			button = buttons[1] = new Button(composite, SWT.RADIO);
			button.setText(ActionMessages.getString("OpenWithMenu.11")); //$NON-NLS-1$
			setButtonLayoutData(button);
			button.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					handleInternalButtonSelected((Button) e.widget);
				}
			});
			return buttons;
		}

		private Set getExternalEditors() {
			if (fExternalEditors == null) {
				fExternalEditors = new HashSet();
				fExternalEditors.addAll(Arrays.asList(loadExecutables()));
			}
			return fExternalEditors;
		}

		private Object getInternalEditors() {
			if (fInternalEditors == null) {
				fInternalEditors = new HashSet();
				IEditorRegistry editorRegistry = PlatformUI.getWorkbench().getEditorRegistry();
				try {
					fInternalEditors.addAll(Arrays.asList((Object[]) editorRegistry.getClass().getMethod("getSortedEditorsFromPlugins", null).invoke(editorRegistry, null))); //$NON-NLS-1$
				} catch (Exception ignore) {
				}
			}
			return fInternalEditors;
		}

		private TableViewer createTableViewer(Composite parent) {
			TableViewer viewer = new TableViewer(parent, SWT.SINGLE | SWT.BORDER);
			viewer.setContentProvider(new ArrayContentProvider());
			viewer.setLabelProvider(new EditorDescriptorLabelProvider());
			viewer.setSorter(new ViewerSorter());
			GridData data = new GridData(GridData.FILL_BOTH);
			data.heightHint = convertHeightInCharsToPixels(14);
			data.widthHint = convertWidthInCharsToPixels(45);
			viewer.getTable().setLayoutData(data);
			viewer.getTable().addSelectionListener(new SelectionAdapter() {
				public void widgetDefaultSelected(SelectionEvent e) {
					okPressed();
				}
			});
			if (fSelection != null)
				viewer.setSelection(new StructuredSelection(fSelection));
			viewer.addSelectionChangedListener(new ISelectionChangedListener() {
				public void selectionChanged(SelectionChangedEvent event) {
					updateButtons();
				}
			});
			return viewer;
		}

		protected Control createButtonBar(Composite parent) {
			Control control = super.createButtonBar(parent);
			updateButtons();
			return control;
		}
		
		private String[] loadExecutables() {
			IPreferenceStore store = ZipEditorPlugin.getDefault().getPreferenceStore();
			return (String[]) PreferenceInitializer.split(store.getString(PreferenceConstants.EXTERNAL_EDITORS), ",", String.class); //$NON-NLS-1$
		}
		
		private void saveExecutables() {
			IPreferenceStore store = ZipEditorPlugin.getDefault().getPreferenceStore();
			store.setValue(PreferenceConstants.EXTERNAL_EDITORS, PreferenceInitializer.join(getExternalEditors().toArray(), ",")); //$NON-NLS-1$
		}

		private void updateButtons() {
			getButton(IDialogConstants.OK_ID).setEnabled(!fTableViewer.getSelection().isEmpty());
		}

		private void handleExternalButtonSelected(Button button) {
			fTableViewer.setInput(getExternalEditors());
			fBrowseButton.setEnabled(true);
			previousSelectedRadio = button.getText();
		}

		private void handleInternalButtonSelected(Button button) {
			fTableViewer.setInput(getInternalEditors());
			fBrowseButton.setEnabled(false);
			previousSelectedRadio = button.getText();
		}

		private void handleBrowseButtonSelected() {
			FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
			if (fLastFilterPath != null)
				dialog.setFilterPath(fLastFilterPath);
			String selectedFile = dialog.open();
			if (selectedFile == null)
				return;
			getExternalEditors().add(selectedFile);
			fTableViewer.refresh();
		}

		public Object getSelection() {
			return fSelection;
		}

		protected void computeResult() {
			fSelection = ((IStructuredSelection) fTableViewer.getSelection()).getFirstElement();
			saveExecutables();
		}
	};
	
	
	private class EditorDescriptorLabelProvider extends LabelProvider {
		public String getText(Object element) {
			return element instanceof IEditorDescriptor ?
					((IEditorDescriptor) element).getLabel() : super.getText(element);
		}
		
		public Image getImage(Object element) {
			return element instanceof IEditorDescriptor ? ZipEditorPlugin
					.getImage(((IEditorDescriptor) element)
							.getImageDescriptor()) : PlatformUI.getWorkbench()
					.getSharedImages().getImage(ISharedImages.IMG_OBJ_FILE);
		}
	};

	private IWorkbenchPage page;
	private IAdaptable file;
	private IEditorRegistry registry = PlatformUI.getWorkbench().getEditorRegistry();

	private static Hashtable imageCache = new Hashtable(11);
	private static Object previousExecutableSelection;
	private static String previousSelectedRadio;
	public static final String ID = PlatformUI.PLUGIN_ID + ".OpenWithMenu";//$NON-NLS-1$
	private static final int MATCH_BOTH = IWorkbenchPage.MATCH_INPUT | IWorkbenchPage.MATCH_ID;

	private static final Comparator comparer = new Comparator() {
		private Collator collator = Collator.getInstance();

		public int compare(Object arg0, Object arg1) {
			String s1 = ((IEditorDescriptor) arg0).getLabel();
			String s2 = ((IEditorDescriptor) arg1).getLabel();
			return collator.compare(s1, s2);
		}
	};

	public OpenWithMenu(IWorkbenchPage page) {
		this(page, null);
	}

	public OpenWithMenu(IWorkbenchPage page, IAdaptable file) {
		super(ID);
		this.page = page;
		this.file = file;
		getFileResource();
	}

	private Image getImage(IEditorDescriptor editorDesc) {
		ImageDescriptor imageDesc = getImageDescriptor(editorDesc);
		if (imageDesc == null) {
			return null;
		}
		Image image = (Image) imageCache.get(imageDesc);
		if (image == null) {
			image = imageDesc.createImage();
			imageCache.put(imageDesc, image);
		}
		return image;
	}

	private ImageDescriptor getImageDescriptor(IEditorDescriptor editorDesc) {
		ImageDescriptor imageDesc = null;
		if (editorDesc == null) {
			imageDesc = registry.getImageDescriptor(getFileResource().getName());
		} else {
			imageDesc = editorDesc.getImageDescriptor();
		}
		if (imageDesc == null) {
			if (editorDesc.getId().equals(IEditorRegistry.SYSTEM_EXTERNAL_EDITOR_ID)) {
				imageDesc = registry.getSystemExternalEditorImageDescriptor(getFileResource().getName());
			}
		}
		return imageDesc;
	}

	private void createMenuItem(Menu menu, final IEditorDescriptor descriptor, final IEditorDescriptor preferredEditor) {
		final MenuItem menuItem = new MenuItem(menu, SWT.RADIO);
		boolean isPreferred = preferredEditor != null && descriptor.getId().equals(preferredEditor.getId());
		menuItem.setSelection(isPreferred);
		menuItem.setText(descriptor.getLabel());
		Image image = getImage(descriptor);
		if (image != null) {
			menuItem.setImage(image);
		}
		Listener listener = new Listener() {
			public void handleEvent(Event event) {
				switch (event.type) {
				case SWT.Selection:
					if (menuItem.getSelection()) {
						openEditor(descriptor);
					}
					break;
				}
			}
		};
		menuItem.addListener(SWT.Selection, listener);
	}

    public void fill(Menu menu, int index) {
		IFileStore file = getFileResource();
		if (file == null) {
			return;
		}

		IEditorDescriptor defaultEditor = registry.findEditor(EditorsUI.DEFAULT_TEXT_EDITOR_ID);

		Object[] editors = registry.getEditors(file.getName(), Utils.getContentType(file));
		Collections.sort(Arrays.asList(editors), comparer);

		boolean defaultFound = false;

		ArrayList alreadyMapped = new ArrayList();

		for (int i = 0; i < editors.length; i++) {
			IEditorDescriptor editor = (IEditorDescriptor) editors[i];
			if (!alreadyMapped.contains(editor)) {
				createMenuItem(menu, editor, null);
				if (defaultEditor != null
						&& editor.getId().equals(defaultEditor.getId())) {
					defaultFound = true;
				}
				alreadyMapped.add(editor);
			}
		}

		if (editors.length > 0) {
			new MenuItem(menu, SWT.SEPARATOR);
		}

		if (!defaultFound && defaultEditor != null) {
			createMenuItem(menu, defaultEditor, null);
		}

		IEditorDescriptor descriptor = registry.findEditor(IEditorRegistry.SYSTEM_EXTERNAL_EDITOR_ID);
		createMenuItem(menu, descriptor, null);

		descriptor = registry.findEditor(IEditorRegistry.SYSTEM_INPLACE_EDITOR_ID);
		if (descriptor != null) {
			createMenuItem(menu, descriptor, null);
		}
		createDefaultMenuItem(menu, file);
		new MenuItem(menu, SWT.SEPARATOR);
		createChooseItem(menu, file);
	}
	
	private IFileStore getFileResource() {
		if (this.file instanceof IFileStore) {
			return (IFileStore) this.file;
		}
		return (IFileStore) this.file.getAdapter(IFileStore.class);
	}

    public boolean isDynamic() {
		return true;
	}

    private void openEditor(IEditorDescriptor editor) {
		IFileStore file = getFileResource();
		if (file == null) {
			return;
		}
		try {
			String editorId = editor == null ? IEditorRegistry.SYSTEM_EXTERNAL_EDITOR_ID : editor.getId();
			page.openEditor(Utils.createEditorInput(file), editorId, true, MATCH_BOTH);
		} catch (PartInitException e) {
			ErrorDialog.openError(page.getWorkbenchWindow().getShell(),
					ActionMessages.getString("OpenWithMenu.0"), e.getMessage(), ZipEditorPlugin.createErrorStatus(e.getMessage(), e)); //$NON-NLS-1$
		}
	}

    private void createDefaultMenuItem(Menu menu, final IFileStore file) {
		final MenuItem menuItem = new MenuItem(menu, SWT.RADIO);
		menuItem.setText(ActionMessages.getString("OpenWithMenu.1")); //$NON-NLS-1$

		Listener listener = new Listener() {
			public void handleEvent(Event event) {
				switch (event.type) {
				case SWT.Selection:
					if (menuItem.getSelection()) {
						try {
							page.openEditor(Utils.createEditorInput(file), Utils.getEditorId(file),
									true, MATCH_BOTH);
						} catch (PartInitException e) {
							ErrorDialog.openError(page.getWorkbenchWindow().getShell(),
									ActionMessages.getString("OpenWithMenu.2"), e.getMessage(), ZipEditorPlugin.createErrorStatus(e.getMessage(), e)); //$NON-NLS-1$
						}
					}
					break;
				}
			}
		};

		menuItem.addListener(SWT.Selection, listener);
	}

	private void createChooseItem(Menu menu, final IFileStore file) {
		final MenuItem menuItem = new MenuItem(menu, SWT.RADIO);
		menuItem.setText(ActionMessages.getString("OpenWithMenu.4")); //$NON-NLS-1$

		Listener listener = new Listener() {
			public void handleEvent(Event event) {
				switch (event.type) {
				case SWT.Selection:
					if (menuItem.getSelection()) {
						openWithProgram(file);
					}
					break;
				}
			}
		};

		menuItem.addListener(SWT.Selection, listener);
	}

	private void openWithProgram(IFileStore file) {
		ExecutableSelectionDialog dialog = new ExecutableSelectionDialog(
				page.getWorkbenchWindow().getShell(), previousExecutableSelection);
		if (dialog.open() != Window.OK)
			return;
		previousExecutableSelection = dialog.getSelection();
		if (previousExecutableSelection instanceof String) {
			try {
				Runtime.getRuntime().exec(previousExecutableSelection + " " + file.toString()); //$NON-NLS-1$
			} catch (Exception e) {
				ZipEditorPlugin.log(e);
			}
		} else if (previousExecutableSelection instanceof IEditorDescriptor) {
			if (!file.fetchInfo().isDirectory() && file.fetchInfo().exists()) {
				IEditorInput input = Utils.createEditorInput(file);
				String editorId = ((IEditorDescriptor) previousExecutableSelection).getId();
				try {
					page.openEditor(input, editorId);
				} catch (PartInitException e) {
					ZipEditorPlugin.log(e);
				}
			}
		}
	}
}
