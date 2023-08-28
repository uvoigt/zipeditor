/*
 * (c) Copyright 2002, 2023 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.search;

import java.io.File;

import zipeditor.model.Node;
import zipeditor.model.ZipModel;

public class PlainNode extends Node {

	public PlainNode(ZipModel model, String name) {
		super(model, name, false);
		file = model.getZipPath();
		
	}

	public Node create(ZipModel model, String name, boolean isFolder) {
		return new PlainNode(model, name);
	}

	public Object getAdapter(Class adapter) {
		if (adapter == Element.class) {
			File path = getModel().getZipPath();
			return new Element(null, path.getAbsolutePath(), getName(),
					Long.valueOf(path.length()), Long.valueOf(path.lastModified()), Element.UNKNOWN);
		}
		return super.getAdapter(adapter);
	}
}
