/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.model;

import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.archivers.zip.ZipMethod;
import zipeditor.model.zstd.ZstdUtilities;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;

public class ZipNode extends Node {
	private class EntryStream extends InputStream {
		private InputStream in;
		private ZipFile zipFile;
		private EntryStream(ZipArchiveEntry entry, ZipFile zipFile) throws IOException {
			ZipArchiveEntry zipArcEntry = zipFile.getEntry(entry.getName());
			in = zipFile.getInputStream(zipArcEntry);
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
	private ZipArchiveEntry zipEntry;
	private int method = ZipEntry.DEFLATED;

	public ZipNode(ZipModel model, ZipArchiveEntry entry, String name, boolean isFolder) {
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
		if (in != null) {
			return in;
		}
		if (zipEntry != null && model.getZipPath() != null) {
			ZipFile zipFile;
			if (isZstdEncoded(zipEntry)) {
				zipFile = ZstdUtilities.getZipFileBuilder().setFile(model.getZipPath()).get();
			} else {
				zipFile = ZipFile.builder().setFile(model.getZipPath()).get();
			}
			return new EntryStream(zipEntry, zipFile);
		}
		return null;
	}
	
	private static boolean isZstdEncoded(ZipArchiveEntry zipEntry) {
		return zipEntry.getMethod() == ZipMethod.ZSTD.getCode() || zipEntry.getMethod() == ZipMethod.ZSTD_DEPRECATED.getCode();
	}
	
	public void reset() {
		super.reset();
		if (zipEntry != null) {
			time = zipEntry.getTime();
			size = zipEntry.getSize();
		}
	}
	
	public void update(Object entry) {
		if (entry instanceof ZipArchiveEntry) {
			zipEntry = (ZipArchiveEntry) entry;
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
