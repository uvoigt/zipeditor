/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.model;

import java.util.EventListener;

public interface IModelListener extends EventListener {
	public class ModelChangeEvent {
		private final int fModelState;
		private final Node fModelRoot;
		private Node fNode;
		
		public ModelChangeEvent(ZipModel model) {
			fModelState = model.getState();
			fModelRoot = model.getRoot();
		}

		public ModelChangeEvent(ZipModel model, Node node) {
			this(model);
			fNode = node;
		}

		public boolean isInitStarted() {
			return (fModelState & ZipModel.INIT_STARTED) > 0;
		}
		
		public boolean isInitFinished() {
			return (fModelState & ZipModel.INIT_FINISHED) > 0;
		}
		
		public boolean isInitializing() {
			return (fModelState & ZipModel.INITIALIZING) > 0;
		}
		
		public boolean isDispose() {
			return (fModelState & ZipModel.DISPOSE) > 0;
		}

		public Node getNode() {
			return fNode;
		}

		public Node getModelRoot() {
			return fModelRoot;
		}
	};

	public void modelChanged(ModelChangeEvent event);
}
