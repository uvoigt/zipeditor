/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.views.framelist.Frame;

import zipeditor.model.Node;
import zipeditor.model.ZipModel;

public class TableFrame extends Frame {
	private static final String TAG_SELECTION = "selection"; //$NON-NLS-1$
	private static final String TAG_ELEMENT = "element"; //$NON-NLS-1$
	private static final String TAG_FRAME_INPUT = "frameInput"; //$NON-NLS-1$
	private static final String TAG_PATH = "nodePath"; //$NON-NLS-1$

	private TableViewer fViewer;
	private Object fInput;
	private ISelection fSelection;

	public TableFrame(TableViewer viewer) {
		fViewer = viewer;
	}

	public TableFrame(TableViewer viewer, Object input) {
		this(viewer);
		setInput(input);
		ILabelProvider provider = (ILabelProvider) viewer.getLabelProvider();
		String name = provider.getText(input);
		if (name == null) {
			name = "";//$NON-NLS-1$
		}
		setName(name);
		setToolTipText(name);
	}

	public Object getInput() {
		return fInput;
	}

	public ISelection getSelection() {
		return fSelection;
	}

	public TableViewer getViewer() {
		return fViewer;
	}

	public void restoreState(IMemento memento, ZipModel model) {
		IMemento inputMem = memento.getChild(TAG_FRAME_INPUT);
		String inputPath = inputMem != null ? inputMem.getString(TAG_PATH) : null;
		if (inputPath != null)
			fInput = model.findNode(inputPath);

		IMemento selectionMem = memento.getChild(TAG_SELECTION);
		if (selectionMem != null) {
			IMemento[] elements = selectionMem.getChildren(TAG_ELEMENT);
			List selection = new ArrayList();
			for (int i = 0; i < elements.length; i++) {
				String nodePath = elements[i].getString(TAG_PATH);
				if (nodePath == null)
					continue;
				Node node = model.findNode(nodePath);
				if (node != null)
					selection.add(node);
			}
			fSelection = new StructuredSelection(selection);
		} else {
			fSelection = StructuredSelection.EMPTY;
		}
	}

	private void saveNode(Node node, IMemento memento, String childTag) {
		IMemento childMem = memento.createChild(childTag);
		childMem.putString(TAG_PATH, node.getFullPath());
	}

	public void saveState(IMemento memento) {
		if (fInput instanceof Node)
			saveNode((Node) fInput, memento, TAG_FRAME_INPUT);

		if (fSelection instanceof IStructuredSelection) {
			Object[] elements = ((IStructuredSelection) fSelection).toArray();
			if (elements.length > 0) {
				IMemento selectionMem = memento.createChild(TAG_SELECTION);
				for (int i = 0; i < elements.length; i++) {
					if (elements[i] instanceof Node)
						saveNode((Node) elements[i], selectionMem, TAG_ELEMENT);
				}
			}
		}
	}

	public void setInput(Object input) {
		fInput = input;
	}

	public void setSelection(ISelection selection) {
		fSelection = selection;
	}
}
