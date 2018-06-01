/*
 * (c) Copyright 2002, 2017 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.model;

public abstract class RootNode extends Node {

	public RootNode(ZipModel model) {
		super(model, "", true); //$NON-NLS-1$
	}
}
