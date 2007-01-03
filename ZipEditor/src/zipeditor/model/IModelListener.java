/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.model;

import java.util.EventListener;

public interface IModelListener extends EventListener {
	public class ModelChangeEvent {
		private int fModelState;
		private Node fModelRoot;
		
		public ModelChangeEvent(ZipModel model) {
			fModelState = model.getState();
			fModelRoot = model.getRoot();
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
		
		public Node getModelRoot() {
			return fModelRoot;
		}
	};

	public void modelChanged(ModelChangeEvent event);
}
