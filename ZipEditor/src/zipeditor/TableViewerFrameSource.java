/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.ui.views.framelist.Frame;
import org.eclipse.ui.views.framelist.FrameList;
import org.eclipse.ui.views.framelist.IFrameSource;

public class TableViewerFrameSource implements IFrameSource {
	private TableViewer fViewer;

	public TableViewerFrameSource(TableViewer viewer) {
		fViewer = viewer;
	}

	public void connectTo(FrameList frameList) {
		frameList.addPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				TableViewerFrameSource.this.handlePropertyChange(event);
			}
		});
	}

	protected TableFrame createFrame(Object input) {
		return new TableFrame(fViewer, input);
	}

	protected void frameChanged(TableFrame frame) {
		fViewer.getControl().setRedraw(false);
		fViewer.setInput(frame.getInput());
		fViewer.setSelection(frame.getSelection(), true);
		fViewer.getControl().setRedraw(true);
	}

	protected Frame getCurrentFrame(int flags) {
		Object input = fViewer.getInput();
		TableFrame frame = createFrame(input);
		if ((flags & IFrameSource.FULL_CONTEXT) != 0) {
			frame.setSelection(fViewer.getSelection());
		}
		return frame;
	}

	public Frame getFrame(int whichFrame, int flags) {
		switch (whichFrame) {
		case IFrameSource.CURRENT_FRAME:
			return getCurrentFrame(flags);
		case IFrameSource.PARENT_FRAME:
			return getParentFrame(flags);
		case IFrameSource.SELECTION_FRAME:
			return getSelectionFrame(flags);
		default:
			return null;
		}
	}

	protected Frame getParentFrame(int flags) {
		Object input = fViewer.getInput();
		ITreeContentProvider provider = (ITreeContentProvider) fViewer
				.getContentProvider();
		Object parent = provider.getParent(input);
		if (parent == null) {
			return null;
		} else {
			TableFrame frame = createFrame(parent);
			if ((flags & IFrameSource.FULL_CONTEXT) != 0) {
				frame.setSelection(fViewer.getSelection());
			}
			return frame;
		}
	}

	protected Frame getSelectionFrame(int flags) {
		IStructuredSelection sel = (IStructuredSelection) fViewer
				.getSelection();
		if (sel.size() == 1) {
			Object o = sel.getFirstElement();
			TableFrame frame = createFrame(o);
			if ((flags & IFrameSource.FULL_CONTEXT) != 0) {
				frame.setSelection(fViewer.getSelection());
			}
			return frame;
		}
		return null;
	}

	public TableViewer getViewer() {
		return fViewer;
	}

	protected void handlePropertyChange(PropertyChangeEvent event) {
		if (FrameList.P_CURRENT_FRAME.equals(event.getProperty())) {
			frameChanged((TableFrame) event.getNewValue());
		}
	}
}
