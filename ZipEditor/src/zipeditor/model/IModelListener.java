/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.model;

import java.util.EventListener;

public interface IModelListener extends EventListener {

	public void modelChanged();
}
