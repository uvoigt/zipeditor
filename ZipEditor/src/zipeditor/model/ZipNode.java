/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.model;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipNode extends Node {
	private class EntryStream extends InputStream {
		private InputStream in;
		private ZipFile zipFile;
		private EntryStream(ZipEntry entry, ZipFile zipFile) throws IOException {
			in = zipFile.getInputStream(entry);
			this.zipFile = zipFile;
		}
		public int read() throws IOException {
			return in.read();
		}
		public void close() throws IOException {
			in.close();
			if (zipFile != null)
				zipFile.close();
		}
	};

	private String comment;
	private ZipEntry zipEntry;
	private int method = ZipEntry.DEFLATED;

	public ZipNode(ZipModel model, ZipEntry entry, String name, boolean isFolder) {
		this(model, name, isFolder);
		update(entry);
	}
	
	public ZipNode(ZipModel model, String name, boolean isFolder) {
		super(model, name, isFolder);
	}
	
	public String getComment() {
		return comment;
	}
	
	public void setComment(String comment) {
		if (comment != this.comment && (comment == null || !comment.equals(this.comment))) {
			this.comment = comment;
			setModified(true);
		}
	}
	
	public byte[] getExtra() {
		return zipEntry != null && file == null && zipEntry.getExtra() != null ? zipEntry.getExtra() : new byte[0];
	}
	
	public long getCrc() {
		return zipEntry != null && file == null ? zipEntry.getCrc() : 0;
	}
	
	public long getCompressedSize() {
		return zipEntry != null && file == null ? zipEntry.getCompressedSize() : 0;
	}
	
	public double getRatio() {
		return zipEntry != null && file == null ? (zipEntry.getSize() - zipEntry.getCompressedSize()) / (double) zipEntry.getSize() * 100 : 0;
	}
	
	public int getMethod() {
		return method;
	}
	
	public void setMethod(int method) {
		if (method != this.method) {
			this.method = method;
			setModified(true);
		}
	}

	protected InputStream doGetContent() throws IOException {
		InputStream in = super.doGetContent();
		if (in != null)
			return in;
		if (zipEntry != null) {
			ZipRootNode root = (ZipRootNode) model.getRoot();
			if (root.getZipFile() != null)
				return root.getZipFile().getInputStream(zipEntry);
			return model.getZipPath() != null ? new EntryStream(zipEntry, new ZipFile(model.getZipPath())) : null;
		}
		return null;
	}
	
	public void reset() {
		super.reset();
		if (zipEntry != null) {
			time = zipEntry.getTime();
			size = zipEntry.getSize();
		}
	}
	
	public void update(Object entry) {
		if (entry instanceof ZipEntry) {
			zipEntry = (ZipEntry) entry;
			time = zipEntry.getTime();
			size = zipEntry.getSize();
			comment = zipEntry.getComment();
			method = zipEntry.getMethod();
		}
	}
	
	public Node create(ZipModel model, String name, boolean isFolder) {
		return new ZipNode(model, name, isFolder);
	}
	
}
