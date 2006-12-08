/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.model;

import java.util.EventListener;

public interface IModelListener extends EventListener {
	public class ModelChangeEvent {
		private ZipModel fModel;
		
		public ModelChangeEvent(ZipModel model) {
			super();
			fModel = model;
		}

		public ZipModel getModel() {
			return fModel;
		}
	};

	public void modelChanged(ModelChangeEvent event);
}
