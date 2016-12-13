/*
 * (c) Copyright 2002, 2016 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.search;

import org.eclipse.search.ui.text.Match;

import zipeditor.model.Node;

public class ZipMatch extends Match {

	private boolean fOnNodeName;

	public ZipMatch(Node node, boolean onNodeName, int offset, int length) {
		super(node, offset, length);

		fOnNodeName = onNodeName;
	}

	public boolean isOnNodeName() {
		return fOnNodeName;
	}
}
