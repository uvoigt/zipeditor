/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.model;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

public class GzipNode extends Node {
	public GzipNode(ZipModel model, String name, boolean isFolder) {
		super(model, name, isFolder);
	}

	public Node create(ZipModel model, String name, boolean isFolder) {
		return new GzipNode(model, name, isFolder);
	}

	protected InputStream doGetContent() throws IOException {
		InputStream in = super.doGetContent();
		return in != null ? in : new GZIPInputStream(new FileInputStream(model.getZipPath()));
	}
}
