/*
 * (c) Copyright 2010 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.model;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import net.sf.sevenzipjbinding.ISequentialOutStream;
import net.sf.sevenzipjbinding.SevenZipException;
import net.sf.sevenzipjbinding.simple.ISimpleInArchiveItem;
import zipeditor.ZipEditorPlugin;
import zipeditor.model.ZipModel.SevenZipCreator;

public class SevenZipNode extends Node {
	private class ItemStream extends InputStream implements ISequentialOutStream {
		private ByteBuffer buf;
		public int read() throws IOException {
			if (buf == null) {
				buf = ByteBuffer.allocate((int) size);
				if (!creator.isOpen())
					item = creator.openArchive(item.getItemIndex());
				try {
					item.extractSlow(this);
				} catch (SevenZipException e) {
					ZipEditorPlugin.log(e);
				}
				buf.position(0);
			}
			return buf.position() < buf.capacity() ? buf.get() : -1;
		}
		public int write(byte[] data) throws SevenZipException {
			buf.put(data);
			return data.length;
		}
		
		public void close() throws IOException {
			creator.close();
		}
	}

	private SevenZipCreator creator;
	private ISimpleInArchiveItem item;
	private String comment;

	public SevenZipNode(ZipModel model, ISimpleInArchiveItem item, SevenZipCreator creator, String name,
			boolean isFolder) {
		this(model, name, isFolder);
		this.item = item;
		this.creator = creator;
		if (item != null) {
			try {
				time = item.getLastWriteTime() != null ? item.getLastWriteTime().getTime() : 0L;
				size = item.getSize() != null ? item.getSize().longValue() : 0L;
				comment = item.getComment();
			} catch (SevenZipException e) {
				ZipEditorPlugin.log(e);
			}
		}
	}
	
	public SevenZipNode(ZipModel model, String name, boolean isFolder) {
		super(model, name, isFolder);
	}
	
	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		if (comment == this.comment || comment != null && comment.equals(this.comment))
			return;
		this.comment = comment;
		model.setDirty(true);
		model.notifyListeners();
	}
	
	public long getCompressedSize() {
		try {
			return item != null && item.getPackedSize() != null ? item.getPackedSize().longValue() : 0;
		} catch (SevenZipException e) {
			ZipEditorPlugin.log(e);
			return 0;
		}
	}

	protected InputStream doGetContent() throws IOException {
		InputStream in = super.doGetContent();
		if (in != null)
			return in;
		if (item != null)
			return new ItemStream();
		return null;
	}
	
	public void update(Object entry) {
		if (!(entry instanceof ISimpleInArchiveItem))
			return;
		ISimpleInArchiveItem item = (ISimpleInArchiveItem) entry;
		try {
			time = item.getLastWriteTime() != null ? item.getLastWriteTime().getTime() : 0;
		} catch (SevenZipException e) {
			ZipEditorPlugin.log(e);
		}
	}

	public Node create(ZipModel model, String name, boolean isFolder) {
		return new SevenZipNode(model, name, isFolder);
	}
}
