/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.actions;

import java.text.MessageFormat;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.actions.TextActionHandler;

import zipeditor.ZipEditorPlugin;
import zipeditor.model.Node;

public class RenameNodeAction extends ViewerAction {
	private TreeEditor fTreeEditor;
	private TableEditor fTableEditor;
	private Control fControl;
	private Text fTextEditor;
	private Composite fTextEditorParent;
	private TextActionHandler fTextActionHandler;

	private Node inlinedNode;
	private boolean saving = false;
	private String newName;
	private String[] modelProviderIds;

	public RenameNodeAction(StructuredViewer viewer) {
		super(ActionMessages.getString("RenameNodeAction.0"), viewer); //$NON-NLS-1$
		setToolTipText(ActionMessages.getString("RenameNodeAction.1")); //$NON-NLS-1$
		setId(ZipEditorPlugin.PLUGIN_ID + ".RenameAction"); //$NON-NLS-1$

		fControl = viewer.getControl();
		if (fControl instanceof Tree) {
			fTreeEditor = new TreeEditor((Tree) fControl);
		} else if (fControl instanceof Table) {
			fTableEditor = new TableEditor((Table) fControl);
		} else {
			throw new IllegalArgumentException();
		}
	}

	private boolean checkOverwrite(final Shell shell, final Node destination) {

		final boolean[] result = new boolean[1];

		Runnable query = new Runnable() {
			public void run() {
				String message = ActionMessages.getString("RenameNodeAction.2"); //$NON-NLS-1$
				String title = ActionMessages.getString("RenameNodeAction.3"); //$NON-NLS-1$
				result[0] = MessageDialog.openQuestion(shell,
						title, MessageFormat.format(message,
								new Object[] { destination.getName() }));
			}

		};

		shell.getDisplay().syncExec(query);
		return result[0];
	}

	Composite createParent() {
		Composite result = new Composite((Composite) fControl, SWT.NONE);
		if (fControl instanceof Tree) {
			TreeItem[] selectedItems = ((Tree) fControl).getSelection();
			fTreeEditor.horizontalAlignment = SWT.LEFT;
			fTreeEditor.grabHorizontal = true;
			fTreeEditor.setEditor(result, selectedItems[0]);
		} else {
			TableItem[] selectedItems = ((Table) fControl).getSelection();
			fTableEditor.horizontalAlignment = SWT.LEFT;
			fTableEditor.grabHorizontal = true;
			fTableEditor.setEditor(result, selectedItems[0], 0);
		}
		return result;
	}

	private void createTextEditor(final Node node) {
		// Create text editor parent. This draws a nice bounding rect.
		fTextEditorParent = createParent();
		fTextEditorParent.setVisible(false);
		final int inset = 1;
		if (inset > 0) {
			fTextEditorParent.addListener(SWT.Paint, new Listener() {
				public void handleEvent(Event e) {
					Point textSize = fTextEditor.getSize();
					Point parentSize = fTextEditorParent.getSize();
					e.gc.drawRectangle(0, 0, Math.min(textSize.x + 4,
							parentSize.x - 1), parentSize.y - 1);
				}
			});
		}
		// Create inner text editor.
		fTextEditor = new Text(fTextEditorParent, SWT.NONE);
		fTextEditor.setFont(fControl.getFont());
		fTextEditorParent.setBackground(fTextEditor.getBackground());
		fTextEditor.addListener(SWT.Modify, new Listener() {
			public void handleEvent(Event e) {
				Point textSize = fTextEditor.computeSize(SWT.DEFAULT,
						SWT.DEFAULT);
				textSize.x += textSize.y; // Add extra space for new
				// characters.
				Point parentSize = fTextEditorParent.getSize();
				fTextEditor.setBounds(2, inset, Math.min(textSize.x,
						parentSize.x - 4), parentSize.y - 2 * inset);
				fTextEditorParent.redraw();
			}
		});
		fTextEditor.addListener(SWT.Traverse, new Listener() {
			public void handleEvent(Event event) {

				switch (event.detail) {
				case SWT.TRAVERSE_ESCAPE:
					// Do nothing in this case
					disposeTextWidget();
					event.doit = true;
					event.detail = SWT.TRAVERSE_NONE;
					break;
				case SWT.TRAVERSE_RETURN:
					saveChangesAndDispose(node);
					event.doit = true;
					event.detail = SWT.TRAVERSE_NONE;
					break;
				}
			}
		});
		fTextEditor.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent fe) {
				saveChangesAndDispose(node);
			}
		});

		if (fTextActionHandler != null) {
			fTextActionHandler.addText(fTextEditor);
		}
	}

	private void disposeTextWidget() {
		if (fTextActionHandler != null) {
			fTextActionHandler.removeText(fTextEditor);
		}

		if (fTextEditorParent != null) {
			fTextEditorParent.dispose();
			fTextEditorParent = null;
			fTextEditor = null;
			if (fTreeEditor != null)
				fTreeEditor.setEditor(null, null);
			else
				fTableEditor.setEditor(null, null, 0);
		}
	}

	private void queryNewNodeNameInline(final Node node) {
		if (node == null)
			return;
		if (fTextEditorParent == null) {
			createTextEditor(node);
		}
		fTextEditor.setText(node.getName());

		// Open text editor with initial size.
		fTextEditorParent.setVisible(true);
		Point textSize = fTextEditor.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		textSize.x += textSize.y; // Add extra space for new characters.
		Point parentSize = fTextEditorParent.getSize();
		int inset = 1;
		fTextEditor.setBounds(2, inset, Math.min(textSize.x, parentSize.x - 4),
				parentSize.y - 2 * inset);
		fTextEditorParent.redraw();
		fTextEditor.selectAll();
		fTextEditor.setFocus();
	}

	public void run() {
		Node[] nodes = getSelectedNodes();
		if (nodes.length == 1)
			queryNewNodeNameInline(nodes[0]);
	}

	private Node getSelectedNode() {
		Node[] nodes = getSelectedNodes();
		return nodes.length > 0 ? nodes[0] : null;
	}

	protected void runWithNewName(String name) {
		this.newName = name;
		doOperation();
	}

	private void saveChangesAndDispose(Node node) {
		if (saving == true) {
			return;
		}

		saving = true;
		inlinedNode = node;
		final String newName = fTextEditor.getText();
		Runnable query = new Runnable() {
			public void run() {
				try {
					if (!newName.equals(inlinedNode.getName())) {
						runWithNewName(newName);
					}
					inlinedNode = null;
					disposeTextWidget();
					if (fControl != null && !fControl.isDisposed()) {
						fControl.setFocus();
					}
				} finally {
					saving = false;
				}
			}
		};
		fControl.getDisplay().asyncExec(query);
	}

	public void setTextActionHandler(TextActionHandler actionHandler) {
		fTextActionHandler = actionHandler;
	}

	public String[] getModelProviderIds() {
		return modelProviderIds;
	}

	public void setModelProviderIds(String[] modelProviderIds) {
		this.modelProviderIds = modelProviderIds;
	}

	protected void doOperation() {
		Node node = getSelectedNode();
		if (node == null)
			return;
		Node newNode = newName != null ? node.getParent().getChildByName(newName, false) : null;
		boolean go = true;
		if (newNode != null) {
			go = checkOverwrite(fControl.getShell(), newNode);
		}
		if (go) {
			node.setName(newName);
		}
	}
}
