/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.runtime.IProgressMonitor;

import zipeditor.ZipEditorPlugin;

public class ZipNode {
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
			zipFile.close();
		}
	};

	private ZipNode parent;
	private List children;
	private boolean isFolder;
	private String name;
	private long time;
	private long size;
	private String comment;
	private ZipEntry entry;
	private File file;
	private ZipModel model;
	
	public ZipNode(ZipModel model, ZipEntry entry, String name, boolean isFolder) {
		if (model == null)
			throw new NullPointerException();
		if (name == null)
			throw new NullPointerException();
		this.model = model;
		this.entry = entry;
		this.name = name;
		this.isFolder = isFolder;
		if (entry != null) {
			this.time = entry.getTime();
			this.size = entry.getSize();
			this.comment = entry.getComment();
		} else {
			this.time = System.currentTimeMillis();			
		}
	}
	
	public void add(ZipNode node) throws IllegalArgumentException {
		node.parent = this;
		if (children == null)
			children = new ArrayList();
		children.add(node);
		model.notifyListeners();
	}
	
	public void add(File file, IProgressMonitor monitor) throws IllegalArgumentException {
		ZipNode node = new ZipNode(model, null, file.getName(), file.isDirectory());
		add(node);
		if (node.isFolder) {
			File[] files = file.listFiles();
			if (files != null) {
				for (int i = 0; i < files.length; i++) {
					if (monitor.isCanceled())
						break;
					monitor.subTask(files[i].getName());
					node.add(files[i], monitor);
				}
			}
		} else {
			node.updateContent(file);
			monitor.worked(1);
		}
	}
	
	public void updateContent(File file) {
		try {
			//content = readFile(file);
			this.file = file;
			entry = null;
			time = System.currentTimeMillis();
			size = file.length();
		} catch (Exception e) {
			ZipEditorPlugin.log(e);
		}
	}

	public void remove(ZipNode node) {
		if (children == null)
			return;
		for (Iterator it = children.iterator(); it.hasNext(); ) {
			if (it.next().equals(node))
				it.remove();
		}
	}

	public ZipNode getParent() {
		return parent;
	}
	
	public ZipModel getModel() {
		return model;
	}

	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
		model.notifyListeners();
	}
	
	public String getType() {
		int index = name.lastIndexOf('.');
		return index != -1 ? name.substring(index + 1) : ""; //$NON-NLS-1$
	}

	public String getPath() {
		if (parent == null)
			return ""; //$NON-NLS-1$
		StringBuffer sb = new StringBuffer(parent.getPath());
		if (children != null)
			sb.append(name);
		if (isFolder)
			sb.append('/');
		return sb.toString();
	}
	
	public String getFullPath() {
		StringBuffer sb = new StringBuffer(getPath());
		if (!isFolder)
			sb.append(name);
		return sb.toString();
	}

	protected ZipEntry getEntry() {
		return entry;
	}
	
	public String getComment() {
		return comment;
	}
	
	public void setComment(String comment) {
		this.comment = comment;
		model.notifyListeners();
	}
	
	public boolean isFolder() {
		return isFolder;
	}
	
	public long getTime() {
		return time;
	}
	
	public byte[] getExtra() {
		return entry != null && entry.getExtra() != null ? entry.getExtra() : new byte[0];
	}
	
	public long getCrc() {
		return entry != null ? entry.getCrc() : 0;
	}
	
	public long getCompressedSize() {
		return entry != null ? entry.getCompressedSize() : 0;
	}
	
	public long getSize() {
		return size;
	}
	
	void setSize(long size) {
		this.size = size;
	}
	
	public double getRatio() {
		return entry != null ? (entry.getSize() - entry.getCompressedSize()) / (double) entry.getSize() * 100 : 0;
	}
	
	public ZipNode[] getChildren() {
		return children != null ? (ZipNode[]) children.toArray(new ZipNode[children.size()]) : new ZipNode[0];
	}
	
	public ZipNode getChildByName(String name, boolean deep) {
		if (children == null)
			return null;
		for (int i = 0, n = children.size(); i < n; i++) {
			ZipNode child = (ZipNode) children.get(i);
			if (child.name.equals(name))
				return child;
		}
		if (!deep)
			return null;
		for (int i = 0, n = children.size(); i < n; i++) {
			ZipNode child = (ZipNode) children.get(i);
			ZipNode result = child.getChildByName(name, deep);
			if (result != null)
				return result;
		}
		return null;
	}
	
	public InputStream getContent() {
		try {
			if (file != null)
				return new FileInputStream(file);
			return internalGetContent();
		} catch (Exception e) {
			ZipEditorPlugin.log(e);
			return null;
		}
	}
	
	private InputStream internalGetContent() throws IOException {
		if (entry == null)
			return null;
		return new EntryStream(entry, model.getZipFile());
	}

	public String toString() {
		return getFullPath();
	}
}
