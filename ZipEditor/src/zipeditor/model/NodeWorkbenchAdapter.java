/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.model;

import org.eclipse.ui.model.WorkbenchAdapter;

public class NodeWorkbenchAdapter extends WorkbenchAdapter {
	private Node node;

	public NodeWorkbenchAdapter(Node node) {
		this.node = node;
	}
	
	public String getLabel(Object object) {
		return node.getName();
	}
}
