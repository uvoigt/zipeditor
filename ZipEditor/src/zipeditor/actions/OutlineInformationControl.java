/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.actions;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlExtension2;
import org.eclipse.jface.text.IInformationControlExtension3;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import zipeditor.PreferenceConstants;
import zipeditor.ZipContentProvider;
import zipeditor.ZipEditorPlugin;
import zipeditor.ZipLabelProvider;
import zipeditor.ZipSorter;
import zipeditor.model.Node;

public class OutlineInformationControl implements IInformationControl,
		IInformationControlExtension2, IInformationControlExtension3 {

	private class PatternFilter extends ViewerFilter {
		private Map fCache = new HashMap();

		public Object[] filter(Viewer viewer, Object parent, Object[] elements) {
			if (fMatcher == null)
				return elements;

			Object[] filtered = (Object[]) fCache.get(parent);
			if (filtered == null) {
				filtered = super.filter(viewer, parent, elements);
				fCache.put(parent, filtered);
			}
			return filtered;
		}

		public boolean select(Viewer viewer, Object parentElement, Object element) {

			String labelText = ((ILabelProvider) ((StructuredViewer) viewer)
					.getLabelProvider()).getText(element);
			if (labelText == null)
				return false;
			return match(labelText);
		}

		public void setPattern(String patternString) {
			fCache.clear();
			if (patternString == null || patternString.equals("")) //$NON-NLS-1$
				fMatcher = null;
			else
				fMatcher = new StringMatcher(patternString + "*", true, false); //$NON-NLS-1$
		}

		protected boolean match(String string) {
			return fMatcher.match(string);
		}
	};

	private Shell fShell;
	private TableViewer fTableViewer;
	private PatternFilter fPatternFilter = new PatternFilter();
	private Text fText;
	private ToolBar fToolBar;
	private StringMatcher fMatcher;
	private StructuredViewer fViewer;
	private MenuManager fMenuManager;
	
	private final static String PREFERENCE_SORTER = "quick_outline"; //$NON-NLS-1$
	
	public OutlineInformationControl(StructuredViewer viewer, Shell parent) {
		fViewer = viewer;
		fShell = createShell(parent);
		fText = createTextControl(fShell);
		new Label(fShell, SWT.SEPARATOR | SWT.HORIZONTAL).setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fTableViewer = createViewer(fShell);
		new Label(fShell, SWT.SEPARATOR | SWT.HORIZONTAL).setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		setForegroundColor(parent.getDisplay().getSystemColor(SWT.COLOR_INFO_FOREGROUND));
		setBackgroundColor(parent.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));

		fillMenuManager(fMenuManager);
	}

	private Shell createShell(Shell parent) {
		Shell shell = new Shell(parent, SWT.RESIZE | SWT.BORDER | SWT.NO_FOCUS);
		shell.setLayout(createLayout(1));
		shell.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				deactivate();
			}
		});
		return shell;
	}

	private GridLayout createLayout(int numColumns) {
		GridLayout layout = new GridLayout(numColumns, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		return layout;
	}

	private Text createTextControl(Shell parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(createLayout(2));
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		Text text = new Text(composite, SWT.NONE);
		text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		text.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				handleFilterFieldKeyReleased(e);
			}
		});
		text.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				handleFilterFieldModified();
			}
		});
		text.addSelectionListener(new SelectionAdapter() {
			public void widgetDefaultSelected(SelectionEvent e) {
				IStructuredSelection selection = (IStructuredSelection) fTableViewer.getSelection();
				if (!selection.isEmpty()) {
					entrySelected((Node) selection.getFirstElement());
					deactivate();
				}
			}
		});

		createMenuButton(composite, fMenuManager = new MenuManager());
		composite.setTabList(new Control[] { text });

		return text;
	}

	protected void fillMenuManager(MenuManager menuManager) {
		menuManager.add(new Separator());
		menuManager.add(new SortAction(fTableViewer, PREFERENCE_SORTER));
	}

	private void createMenuButton(Composite parent, final MenuManager menuManager) {
		if (menuManager == null)
			return;
		fToolBar = new ToolBar(parent, SWT.FLAT);
		fToolBar.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
		ToolItem item = new ToolItem(fToolBar, SWT.PUSH, 0);
		GridData data = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_END);
		fToolBar.setLayoutData(data);
		item.setImage(ZipEditorPlugin.getImage("icons/arrow_down.gif")); //$NON-NLS-1$
		item.setToolTipText(ActionMessages.getString("OutlineInformationControl.0")); //$NON-NLS-1$
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				showViewMenu(fToolBar, menuManager);
			}
		});
	}

	protected void showViewMenu(ToolBar toolBar, MenuManager menuManager) {
		Menu menu = menuManager.createContextMenu(fShell);
		Rectangle bounds = toolBar.getBounds();
		Point topLeft = new Point(bounds.x, bounds.y + bounds.height);
		topLeft = fShell.toDisplay(topLeft);
		menu.setLocation(topLeft.x, topLeft.y);
		menu.setVisible(true);
	}

	private TableViewer createViewer(Shell shell) {
		TableViewer viewer = new TableViewer(shell, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.VIRTUAL);
		viewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
		viewer.setContentProvider(new ZipContentProvider(PreferenceConstants.VIEW_MODE_FOLDERS_ONE_LAYER));
		viewer.setLabelProvider(new ZipLabelProvider());
		viewer.setSorter(new ZipSorter(PREFERENCE_SORTER));
		
		viewer.getTable().addSelectionListener(new SelectionAdapter() {
			public void widgetDefaultSelected(SelectionEvent e) {
				entrySelected((Node) e.item.getData());
				deactivate();
			}
		});
		viewer.getControl().addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				handleKeyReleased(e);
			}
		});
		viewer.addFilter(fPatternFilter);
		return viewer;
	}

	protected void handleKeyReleased(KeyEvent e) {
		if (e.keyCode == SWT.ARROW_UP) {
			TableItem[] selection = fTableViewer.getTable().getSelection();
			int itemCount = fTableViewer.getTable().getItemCount();
			if (selection.length > 0 && itemCount > 0 && fTableViewer.getTable().getItems()[0].equals(
					selection[0]))
				fText.setFocus();
		}
	}

	protected void handleFilterFieldKeyReleased(KeyEvent e) {
		if (e.keyCode == SWT.ARROW_DOWN)
			fTableViewer.getControl().setFocus();
	}
	
	protected void handleFilterFieldModified() {
		fPatternFilter.setPattern(fText.getText());
		fTableViewer.getTable().setRedraw(false);
		fTableViewer.refresh();
		if (fText.getText().length() > 0)
			selectFirstMatch();
		fTableViewer.getTable().setRedraw(true);
	}
	
	protected void selectFirstMatch() {
		Table table = fTableViewer.getTable();
		Object element = findElement(table.getItems());
		if (element != null)
			fTableViewer.setSelection(new StructuredSelection(element), true);
		else
			fTableViewer.setSelection(StructuredSelection.EMPTY);
	}

	private Node findElement(TableItem[] items) {
		ILabelProvider labelProvider = (ILabelProvider) fTableViewer.getLabelProvider();
		for (int i = 0; i < items.length; i++) {
			Node element = (Node) items[i].getData();
			if (fMatcher == null)
				return element;

			if (element != null) {
				String label = labelProvider.getText(element);
				if (fMatcher.match(label))
					return element;
			}
		}
		return null;
	}

	protected void entrySelected(Node node) {
		fViewer.setSelection(new StructuredSelection(node), true);
	}

	public void setInformation(String information) {
	}

	public void setSizeConstraints(int maxWidth, int maxHeight) {
	}

	public Point computeSizeHint() {
		return new Point(10, 10);
	}

	public void setVisible(boolean visible) {
		IStructuredSelection selection = (IStructuredSelection) fViewer.getSelection();
		Node element = (Node) selection.getFirstElement();

		fShell.setVisible(visible);
		fText.setText(""); //$NON-NLS-1$
		if (visible && element != null)
			fTableViewer.setSelection(new StructuredSelection(element), true);
	}

	public void setSize(int width, int height) {
		fShell.setSize(width, height);
	}

	public void setLocation(Point location) {
		fShell.setLocation(location);
	}
	
	protected void deactivate() {
		setVisible(false);
	}

	public void dispose() {
		fShell.dispose();
		fShell = null;
		fTableViewer = null;
	}

	public void addDisposeListener(DisposeListener listener) {
		fTableViewer.getControl().addDisposeListener(listener);
	}

	public void removeDisposeListener(DisposeListener listener) {
		fTableViewer.getControl().removeDisposeListener(listener);
	}

	public void setForegroundColor(Color foreground) {
		fShell.setForeground(foreground);
		fText.setForeground(foreground);
		fText.getParent().setForeground(foreground);
		fTableViewer.getControl().setForeground(foreground);
	}

	public void setBackgroundColor(Color background) {
		fShell.setBackground(background);
		fText.setBackground(background);
		fText.getParent().setBackground(background);
		fTableViewer.getControl().setBackground(background);
	}

	public boolean isFocusControl() {
		return fTableViewer.getControl().isFocusControl() ||
				fText.isFocusControl();
	}

	public void setFocus() {
		fText.setFocus();
	}

	public void addFocusListener(FocusListener listener) {
		fTableViewer.getControl().addFocusListener(listener);
	}

	public void removeFocusListener(FocusListener listener) {
		fTableViewer.getControl().removeFocusListener(listener);
	}

	public Rectangle getBounds() {
		return fShell.getBounds();
	}

	public Rectangle computeTrim() {
		return fShell.computeTrim(-1, -1, 20, 20);
	}

	public boolean restoresSize() {
		return true;
	}

	public boolean restoresLocation() {
		return true;
	}

	public void setInput(Object input) {
		fTableViewer.setInput(input);
	}
}
