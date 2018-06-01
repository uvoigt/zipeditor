/*
 * (c) Copyright 2002, 2017 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.model;

import java.io.IOException;

import org.apache.tools.tar.TarInputStream;

public class TarRootNode extends RootNode {

	private TarInputStream in;

	public TarRootNode(ZipModel model) {
		super(model);
	}

	TarInputStream getInputStream() {
		return in;
	}

	public Object accept(NodeVisitor visitor, Object argument) throws IOException {
		in = TarNode.getTarFile(model);
		try {
			return super.accept(visitor, argument);
		} finally {
			if (in != null)
				in.close();
			in = null;
		}
	}

	public Node create(ZipModel model, String name, boolean isFolder) {
		return new TarNode(model, name, isFolder);
	}
}
