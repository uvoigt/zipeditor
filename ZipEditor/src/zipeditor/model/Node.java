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

import org.eclipse.core.runtime.IProgressMonitor;

import zipeditor.ZipEditorPlugin;

public abstract class Node {
	protected Node parent;
	protected List children;
	protected boolean isFolder;
	protected String name;
	protected long time;
	protected long size;
	protected File file;
	protected ZipModel model;

	public Node(ZipModel model, String name, boolean isFolder) {
		if (model == null)
			throw new NullPointerException();
		if (name == null)
			throw new NullPointerException();
		this.model = model;
		this.name = name;
		this.isFolder = isFolder;
		this.time = System.currentTimeMillis();			
	}

	public Node getParent() {
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
		model.setDirty(true);
		model.notifyListeners();
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

	public String getType() {
		int index = name.lastIndexOf('.');
		return index != -1 ? name.substring(index + 1) : ""; //$NON-NLS-1$
	}

	public boolean isFolder() {
		return isFolder;
	}

	public long getTime() {
		return time;
	}

	public long getSize() {
		return size;
	}

	protected void setSize(long size) {
		this.size = size;
	}

	public Node[] getChildren() {
		return children != null ? (Node[]) children.toArray(new Node[children.size()]) : new Node[0];
	}

	public Node getChildByName(String name, boolean deep) {
		if (children == null)
			return null;
		for (int i = 0, n = children.size(); i < n; i++) {
			Node child = (Node) children.get(i);
			if (child.name.equals(name))
				return child;
		}
		if (!deep)
			return null;
		for (int i = 0, n = children.size(); i < n; i++) {
			Node child = (Node) children.get(i);
			Node result = child.getChildByName(name, deep);
			if (result != null)
				return result;
		}
		return null;
	}

	public InputStream getContent() {
		try {
			return doGetContent();
		} catch (Exception e) {
			ZipEditorPlugin.log(e);
			return null;
		}
	}
	
	protected InputStream doGetContent() throws IOException {
		return file != null ? new FileInputStream(file) : null;
	}

	public void add(Node node) throws IllegalArgumentException {
		node.parent = this;
		if (children == null)
			children = new ArrayList();
		children.add(node);
		model.setDirty(true);
		model.notifyListeners();
	}
	
	public void add(File file, IProgressMonitor monitor) throws IllegalArgumentException {
		Node node = create(model, file.getName(), file.isDirectory());
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
		this.file = file;
		time = System.currentTimeMillis();
		size = file.length();
		model.setDirty(true);
	}

	public void remove(Node node) {
		if (children == null)
			return;
		for (Iterator it = children.iterator(); it.hasNext(); ) {
			if (it.next().equals(node))
				it.remove();
		}
		model.setDirty(true);
		model.notifyListeners();
	}

	public abstract Node create(ZipModel model, String name, boolean isFolder);

	public String toString() {
		return getFullPath();
	}

}
