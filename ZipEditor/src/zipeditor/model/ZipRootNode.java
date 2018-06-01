/*
 * (c) Copyright 2002, 2017 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.model;

import java.io.IOException;
import java.util.zip.ZipFile;

public class ZipRootNode extends RootNode {

	private ZipFile zipFile;

	public ZipRootNode(ZipModel model) {
		super(model);
	}

	ZipFile getZipFile() {
		return zipFile;
	}

	public Object accept(NodeVisitor visitor, Object argument) throws IOException {
		// this is relevant when saving to a previously empty file
		if (model.getZipPath().length() > 0)
			zipFile = new ZipFile(model.getZipPath());
		try {
			return super.accept(visitor, argument);
		} finally {
			if (zipFile != null)
				zipFile.close();
			zipFile = null;
		}
	}

	public Node create(ZipModel model, String name, boolean isFolder) {
		return new ZipNode(model, name, isFolder);
	}
}
