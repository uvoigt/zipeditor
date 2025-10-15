/*
 * (c) Copyright 2002, 2017 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.model;

public class ZipRootNode extends RootNode {

	public ZipRootNode(ZipModel model) {
		super(model);
	}

	public Node create(ZipModel model, String name, boolean isFolder) {
		return new ZipNode(model, name, isFolder);
	}
}
