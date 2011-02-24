/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.actions;

import java.io.File;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
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
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.ui.dialogs.SelectionStatusDialog;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.fieldassist.ContentAssistCommandAdapter;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;

import zipeditor.PreferenceConstants;
import zipeditor.PreferenceInitializer;
import zipeditor.Utils;
import zipeditor.ZipEditorPlugin;
import zipeditor.actions.FileOpener.Editor;
import zipeditor.model.Node;

public class OpenWithMenu extends ContributionItem {
	private class ExecutableSelectionDialog extends SelectionStatusDialog {
		private class EditDialog extends SelectionDialog {
			// be compatible with release before 3.6
			private class ContentProposal implements IContentProposal {
				private String content;
				private String label;
				private String description;
				private int cursorPosition;
				public ContentProposal(String content, String label, String description) {
					this.content = content;
					this.label = label;
					this.description = description;
					cursorPosition = content.length();
				}
				public String getContent() {
					return content;
				}
				public int getCursorPosition() {
					return cursorPosition;
				}
				public String getLabel() {
					return label;
				}
				public String getDescription() {
					return description;
				}
			}

			private Editor fEditor;
			private Text fLabel;
			private Text fPath;
			private Button fBrowse;

			private EditDialog(Editor editor) {
				super(ExecutableSelectionDialog.this.getShell());
				setShellStyle(getShellStyle() | SWT.RESIZE);
				fEditor = editor;
				setTitle(ActionMessages.getString("OpenWithMenu.16")); //$NON-NLS-1$
			}

			protected Control createDialogArea(Composite parent) {
				Composite control = (Composite) super.createDialogArea(parent);
				((GridLayout) control.getLayout()).numColumns = 2;
				Label label = new Label(control, SWT.LEFT);
				label.setText(ActionMessages.getString("OpenWithMenu.14")); //$NON-NLS-1$
				fLabel = createText(control, fEditor.getLabel());
				label = new Label(control, SWT.LEFT);
				label.setText(ActionMessages.getString("OpenWithMenu.15")); //$NON-NLS-1$
				fPath = createText(control, fEditor.getPath());
				new ContentAssistCommandAdapter(fPath, new TextContentAdapter(),
						new IContentProposalProvider() {
							public IContentProposal[] getProposals(String contents,
									int position) {
								return doGetProposals(contents, position);
							}
						}, ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS,
						new char[0], true);
				fBrowse = new Button(control, SWT.PUSH);
				fBrowse.setText(ActionMessages.getString("OpenWithMenu.19")); //$NON-NLS-1$
				fBrowse.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						handleBrowseSelected();
					}
				});
				applyDialogFont(control);
				return control;
			}
			
			private Text createText(Composite parent, String string) {
				Text text = new Text(parent, SWT.BORDER);
				if (string != null)
					text.setText(string);
				GridData data = new GridData(GridData.FILL_HORIZONTAL);
				data.widthHint = convertWidthInCharsToPixels(20);
				text.setLayoutData(data);
				return text;
			}
			
			protected void okPressed() {
				fEditor.setLabel(fLabel.getText());
				fEditor.setPath(fPath.getText());
				super.okPressed();
			}
			
			public Editor getEditor() {
				return fEditor;
			}
			
			private void handleBrowseSelected() {
				FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
				if (fLastFilterPath != null)
					dialog.setFilterPath(fLastFilterPath);
				String selectedFile = dialog.open();
				if (selectedFile == null)
					return;
				int lastPoint = selectedFile.lastIndexOf('.');
				int lastSlash = selectedFile.lastIndexOf('/');
				if (lastSlash == -1)
					lastSlash = selectedFile.lastIndexOf('\\');
				String label = selectedFile.substring(lastSlash + 1, lastPoint != -1 ? lastPoint : selectedFile.length());
				if (fLabel.getText().length() == 0)
					fLabel.setText(label);
				fPath.setText(selectedFile);
			}

			private IContentProposal[] doGetProposals(String contents, int position) {
				String all = ActionMessages.getString("OpenWithMenu.17"); //$NON-NLS-1$
				StringTokenizer st = new StringTokenizer(all, "\n"); //$NON-NLS-1$
				List proposals = new ArrayList();
				while (st.hasMoreElements()) {
					String entry = st.nextToken();
					StringTokenizer tst = new StringTokenizer(entry, " "); //$NON-NLS-1$
					String value = tst.nextToken();
					int wordoffset = contents.lastIndexOf(' ', position - 1) + 1;
					if (wordoffset > position)
						wordoffset = position;
					String word = contents.substring(wordoffset, position);
					if (position >= 3 && contents.substring(position - 3).startsWith("$e")) { //$NON-NLS-1$
						Set set = new TreeSet(new Comparator() {
							public int compare(Object arg0, Object arg1) {
								return ((IEditorDescriptor) arg0).getLabel().compareTo(((IEditorDescriptor) arg1).getLabel());
							}
						});
						set.addAll(Arrays.asList(getEditorsFromRegistry()));
						IEditorDescriptor[] editors = (IEditorDescriptor[]) set.toArray(new IEditorDescriptor[set.size()]);
						for (int i = 0; i < editors.length; i++) {
							proposals.add(new ContentProposal(editors[i].getId(), editors[i].getLabel(), null));
						}
						break;
					} else if (value.startsWith(word))
						proposals.add(new ContentProposal(value.substring(word.length()), value, entry));
				}
				return (IContentProposal[]) proposals.toArray(new IContentProposal[proposals.size()]);
			}
		};
		
		private class EditorDescriptorLabelProvider extends LabelProvider {
			public String getText(Object element) {
				if (!(element instanceof Editor))
					return super.getText(element);
				Editor editor = (Editor) element;
				return editor.getDescriptor() != null ? editor.getDescriptor().getLabel()
						: editor.getLabel();
			}
			
			public Image getImage(Object element) {
				if (!(element instanceof Editor))
					return super.getImage(element);
				Editor editor = (Editor) element;
				return editor.getDescriptor() != null ? ZipEditorPlugin
						.getImage(editor.getDescriptor().getImageDescriptor())
						: PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FILE);
			}
		};

		private class RemoveAction extends Action {
			private RemoveAction() {
				super(ActionMessages.getString("OpenWithMenu.13")); //$NON-NLS-1$
			}
			
			public void run() {
				Object selectedEditor = ((IStructuredSelection) fTableViewer.getSelection()).getFirstElement();
				getExternalEditors().remove(selectedEditor);
				IPreferenceStore store = ZipEditorPlugin.getDefault().getPreferenceStore();
				ArrayList editors = new ArrayList(Arrays.asList((Editor[]) PreferenceInitializer.split(store.getString(PreferenceConstants.RECENTLY_USED_EDITORS), PreferenceConstants.RECENTLY_USED_SEPARATOR, Editor.class)));
				editors.remove(selectedEditor);
				store.setValue(PreferenceConstants.RECENTLY_USED_EDITORS, PreferenceInitializer.join(editors.toArray(), PreferenceConstants.RECENTLY_USED_SEPARATOR));
				fTableViewer.refresh();
			}
		};

		private class AddAction extends Action {
			private AddAction() {
				super(ActionMessages.getString("OpenWithMenu.18")); //$NON-NLS-1$
			}
			
			public void run() {
				addNewEditor();
			}
		};

		private class EditAction extends Action {
			private EditAction() {
				super(ActionMessages.getString("OpenWithMenu.12")); //$NON-NLS-1$
			}
			
			public void run() {
				EditDialog dialog = new EditDialog((Editor) ((IStructuredSelection) fTableViewer.getSelection()).getFirstElement());
				if (dialog.open() == Window.OK)
					fTableViewer.refresh();
			}
		};

		private TableViewer fTableViewer;
		private Object fSelection;
		private String fLastFilterPath;
		private Set fExternalEditors;
		private Set fInternalEditors;
		private Button fAddButton;
		private IAction fAddAction;
		private IAction fEditAction;
		private IAction fRemoveAction;

		public ExecutableSelectionDialog(Shell parentShell, Object initialSelection) {
			super(parentShell);
			setShellStyle(getShellStyle() | SWT.RESIZE);
			fSelection = initialSelection;
			setTitle(ActionMessages.getString("OpenWithMenu.5")); //$NON-NLS-1$
			fAddAction = new AddAction();
			fEditAction = new EditAction();
			fRemoveAction = new RemoveAction();
		}
		
		protected Control createContents(Composite parent) {
			Control control = super.createContents(parent);
			if (fSelection != null)
				fTableViewer.setSelection(new StructuredSelection(fSelection));
			return control;
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
			applyDialogFont(composite);
			return composite;
		}
		
		private Control createExternalEditorGroup(Composite parent) {
			Group group = new Group(parent, SWT.NONE);
			group.setText(ActionMessages.getString("OpenWithMenu.8")); //$NON-NLS-1$
			group.setLayout(new GridLayout());
			group.setLayoutData(new GridData(GridData.FILL_BOTH));
			Button[] buttons = createRadioButtons(group);
			fTableViewer = createTableViewer(group);

			Composite btnComposite = new Composite(group, SWT.NONE);
			btnComposite.setLayout(new GridLayout(2, false));
			btnComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			fAddButton = new Button(btnComposite, SWT.PUSH);
			fAddButton.setText(ActionMessages.getString("OpenWithMenu.9")); //$NON-NLS-1$
			setButtonLayoutData(fAddButton);
			((GridData) fAddButton.getLayoutData()).horizontalAlignment |= GridData.HORIZONTAL_ALIGN_BEGINNING;
			fAddButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					handleAddButtonSelected();
				}
			});

			if (buttons[0].getText().equals(previouslySelectedRadio)) {
				handleExternalButtonSelected(buttons[0]);
				buttons[0].setSelection(true);
			}
			if (buttons[1].getText().equals(previouslySelectedRadio)) {
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
				fExternalEditors.addAll(Arrays.asList(createExternalEditors(loadExecutables())));
			}
			return fExternalEditors;
		}

		private Object getInternalEditors() {
			if (fInternalEditors == null) {
				fInternalEditors = new HashSet(Arrays.asList(createInternalEditors(getEditorsFromRegistry())));
			}
			return fInternalEditors;
		}

		private IEditorDescriptor[] getEditorsFromRegistry() {
			try {
				IEditorRegistry editorRegistry = PlatformUI.getWorkbench().getEditorRegistry();
				return (IEditorDescriptor[]) editorRegistry.getClass().getMethod(
						"getSortedEditorsFromPlugins", null).invoke(editorRegistry, null); //$NON-NLS-1$
			} catch (Exception e) {
				ZipEditorPlugin.log(e);
				return new IEditorDescriptor[0];
			}
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
			viewer.addSelectionChangedListener(new ISelectionChangedListener() {
				public void selectionChanged(SelectionChangedEvent event) {
					updateButtons();
				}
			});
			MenuManager manager = new MenuManager();
			manager.setRemoveAllWhenShown(true);
			manager.addMenuListener(new IMenuListener() {
				public void menuAboutToShow(IMenuManager manager) {
					manager.add(fAddAction);
					manager.add(fEditAction);
					manager.add(fRemoveAction);
				}
			});
			Menu contextMenu = manager.createContextMenu(viewer.getControl());
			viewer.getControl().setMenu(contextMenu);
			return viewer;
		}

		protected Control createButtonBar(Composite parent) {
			Control control = super.createButtonBar(parent);
			updateButtons();
			return control;
		}
		
		private Editor[] createExternalEditors(String[] executablePaths) {
			Editor[] editors = new Editor[executablePaths.length];
			for (int i = 0; i < editors.length; i++) {
				editors[i] = new Editor(executablePaths[i]);
			}
			return editors;
		}
		
		private Editor[] createInternalEditors(IEditorDescriptor[] descriptors) {
			Editor[] editors = new Editor[descriptors.length];
			for (int i = 0; i < editors.length; i++) {
				editors[i] = new Editor(descriptors[i]);
			}
			return editors;
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
			boolean empty = fTableViewer.getSelection().isEmpty();
			if (getButton(IDialogConstants.OK_ID) != null)
				getButton(IDialogConstants.OK_ID).setEnabled(!empty);
			fAddAction.setEnabled(fAddButton.isEnabled());
			fEditAction.setEnabled(fAddButton.isEnabled() && !empty);
			fRemoveAction.setEnabled(fAddButton.isEnabled() && !empty);
		}

		private void handleExternalButtonSelected(Button button) {
			fTableViewer.setInput(getExternalEditors());
			fAddAction.setEnabled(true);
			fEditAction.setEnabled(true);
			fRemoveAction.setEnabled(true);
			fAddButton.setEnabled(true);
			previouslySelectedRadio = button.getText();
			updateButtons();
		}

		private void handleInternalButtonSelected(Button button) {
			fTableViewer.setInput(getInternalEditors());
			fAddAction.setEnabled(false);
			fEditAction.setEnabled(false);
			fRemoveAction.setEnabled(false);
			fAddButton.setEnabled(false);
			previouslySelectedRadio = button.getText();
			updateButtons();
		}

		private void handleAddButtonSelected() {
			addNewEditor();
		}
		
		private void addNewEditor() {
			EditDialog dialog = new EditDialog(new Editor(null, null));
			if (dialog.open() == Window.OK)
				addExternalEditor(dialog.getEditor());
		}

		private void addExternalEditor(Editor editor) {
			getExternalEditors().add(editor);
			fTableViewer.refresh();
			fTableViewer.setSelection(new StructuredSelection(editor));
		}

		public Object getSelection() {
			return fSelection;
		}

		protected void computeResult() {
			fSelection = ((IStructuredSelection) fTableViewer.getSelection()).getFirstElement();
			saveExecutables();
		}
	};

	private IWorkbenchPage fPage;
	private IAdaptable fFile;
	private IEditorRegistry registry = PlatformUI.getWorkbench().getEditorRegistry();
	private FileOpener fFileOpener;

	private static Hashtable imageCache = new Hashtable(11);
	private static Editor previouslySelectedEditor;
	private static String previouslySelectedRadio;
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

	public OpenWithMenu(IWorkbenchPage page, FileOpener fileOpener, IAdaptable file) {
		super(ID);
		fPage = page;
		fFileOpener = fileOpener;
		fFile = file;
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
		if (fFile instanceof IFileStore) {
			return (IFileStore) fFile;
		}
		return (IFileStore) fFile.getAdapter(IFileStore.class);
	}
	
	private Node getNode() {
		return (Node) (fFile instanceof Node ? fFile : fFile.getAdapter(Node.class));
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
			fPage.openEditor(Utils.createEditorInput(file), editorId, true, MATCH_BOTH);
			ZipEditorPlugin.getDefault().addFileMonitor(new File(file.toURI()), getNode());
		} catch (PartInitException e) {
			ErrorDialog.openError(fPage.getWorkbenchWindow().getShell(),
					ActionMessages.getString("OpenWithMenu.0"), e.getMessage(), ZipEditorPlugin.createErrorStatus(e.getMessage(), e)); //$NON-NLS-1$
		}
	}

	private void openWithProgram(IFileStore file) {
		ExecutableSelectionDialog dialog = new ExecutableSelectionDialog(
				fPage.getWorkbenchWindow().getShell(), previouslySelectedEditor);
		if (dialog.open() != Window.OK)
			return;
		fFileOpener.openFromOther(file, previouslySelectedEditor = (Editor) dialog.getSelection());
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
							fPage.openEditor(Utils.createEditorInput(file), Utils.getEditorId(file),
									true, MATCH_BOTH);
							ZipEditorPlugin.getDefault().addFileMonitor(new File(file.toURI()), getNode());
						} catch (PartInitException e) {
							ErrorDialog.openError(fPage.getWorkbenchWindow().getShell(),
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

}
