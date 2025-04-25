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
	
	/**
	 * This method can be used to check if new compression methods can be used without doubts.
	 *  
	 * @param zipMethod the compression method to check
	 * @return true if the given zipMethod was already used in this node model, else false.
	 */
	public boolean hasContentWithCompression(int zipMethod) {
		return hasContentWithCompression(this, zipMethod);
	}

	private boolean hasContentWithCompression(Node parentNode, int zipMethod) {
		for (Node node : parentNode.getChildren()) {
			if (!(node instanceof ZipNode zipNode)) {
				continue;
			}
			if (zipNode.getMethod() == zipMethod) {
				return true;
			}
			if (hasContentWithCompression(zipNode, zipMethod)) {
				return true;
			}
		}
		return false;
	}
}
