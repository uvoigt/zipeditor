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
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.ui.editors.text.EditorsUI;

import zipeditor.Utils;
import zipeditor.ZipEditorPlugin;

public class OpenWithMenu extends ContributionItem {
	private class ProgramSelectionDialog extends SelectionDialog {
		private Table fTable;
		private Program fSelection;
		private List fImages;
		private int topIndex;
		
		public ProgramSelectionDialog(Shell parentShell, Program initialSelection) {
			super(parentShell);
			fImages = new ArrayList();
			fSelection = lastSelection;
			setTitle(ActionMessages.getString("OpenWithMenu.3")); //$NON-NLS-1$
		}
		
		protected Control createDialogArea(Composite parent) {
			Composite composite = (Composite) super.createDialogArea(parent);
			composite.setLayout(new GridLayout());
			composite.setLayoutData(new GridData(GridData.FILL_BOTH));
			fTable = new Table(composite, SWT.SINGLE | SWT.BORDER);
			GridData data = new GridData(GridData.FILL_BOTH);
			data.heightHint = fTable.getItemHeight() * 14;
			data.widthHint = 250;
			fTable.setLayoutData(data);
			fTable.addSelectionListener(new SelectionAdapter() {
				public void widgetDefaultSelected(SelectionEvent e) {
					okPressed();
				}
			});
			BusyIndicator.showWhile(parent.getShell().getDisplay(), new Runnable() {
				public void run() {
					fillTable();
				}
			});
			fTable.setTopIndex(topIndex);
			selectValue(fSelection);
			return composite;
		}
		
		private void selectValue(Program value) {
			if (value == null)
				return;
			TableItem[] items = fTable.getItems();
			for (int i = 0; i < items.length; i++) {
				if (value.equals(items[i].getData())) {
					fTable.select(i);
					fTable.showSelection();
					break;
				}
			}
		}
		
		private void fillTable() {
			fTable.setRedraw(false);
			Program[] programs = Program.getPrograms();
			for (int i = 0; i < programs.length; i++) {
				TableItem item = new TableItem(fTable, SWT.NULL);
				Program program = programs[i];
				item.setData(program);
				item.setText(program.getName());
				ImageData data = program.getImageData();
				if (data == null)
					continue;
				Image image = new Image(getShell().getDisplay(), data);
				fImages.add(image);
				item.setImage(image);
			}
			fTable.setRedraw(true);
		}
		
		protected void okPressed() {
			fSelection = fTable.getSelectionCount() > 0 ?
				(Program) fTable.getSelection()[0].getData() : null;
			topIndex = fTable.getTopIndex();
			super.okPressed();
		}
		
		public Program getSelection() {
			return fSelection;
		}
		
		public boolean close() {
			for (Iterator iter = fImages.iterator(); iter.hasNext();) {
				Image image = (Image) iter.next();
				image.dispose();
			}
			return super.close();
		}
	};
	
	private IWorkbenchPage page;
	private IAdaptable file;
	private IEditorRegistry registry = PlatformUI.getWorkbench().getEditorRegistry();
	private static Hashtable imageCache = new Hashtable(11);
	private static Program lastSelection;

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
		ProgramSelectionDialog dialog = new ProgramSelectionDialog(
				page.getWorkbenchWindow().getShell(), lastSelection);
		if (dialog.open() != Window.OK)
			return;
		lastSelection = dialog.getSelection();
		if (lastSelection != null)
			lastSelection.execute(file.toString());
	}
}
