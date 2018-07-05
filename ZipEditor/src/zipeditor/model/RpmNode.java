/*
 * (c) Copyright 2002, 2017 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.model;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import zipeditor.rpm.RpmEntry;
import zipeditor.rpm.RpmInputStream;

public class RpmNode extends Node {

	private class EntryStream extends InputStream {
		private InputStream in;
		private EntryStream(RpmEntry entry) throws IOException {
			RpmInputStream in = new RpmInputStream(new FileInputStream(model.getZipPath()));
			this.in = in.getInputStream(entry);
		}
		public int read() throws IOException {
			return in != null ? in.read() : -1;
		}
		public void close() throws IOException {
			if (in != null)
				in.close();
		}
	}

	private RpmEntry rpmEntry;

	public RpmNode(ZipModel model, RpmEntry entry, String name, boolean isFolder) {
		super(model, name, isFolder);
		rpmEntry = entry;
		if (entry != null) {
			time = entry.getTime();
			size = entry.getSize();
		}
	}

	public RpmNode(ZipModel model, String name, boolean isFolder) {
		super(model, name, isFolder);
	}

    protected InputStream doGetContent() throws IOException {
		InputStream in = super.doGetContent();
		if (in != null)
			return in;
		if (rpmEntry != null && model.getZipPath() != null)
			return new EntryStream(rpmEntry);
		return null;
	}

    public Node create(ZipModel model, String name, boolean isFolder) {
    	return new RpmNode(model, name, isFolder);
    }
}
