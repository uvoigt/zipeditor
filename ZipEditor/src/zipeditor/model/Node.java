/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.model;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.ui.model.IWorkbenchAdapter;

import zipeditor.ZipEditorPlugin;

public abstract class Node extends PlatformObject {
	protected Node parent;
	protected List children;
	protected int state;
	protected String name;
	protected long time;
	protected long size;
	protected File file;
	private byte[] content;
	protected ZipModel model;
	
	private String path;
	private String fullPath;
	
	private Hashtable property;
	// these refer to other models. this comes into play when searching recursively
	private List parentNodes;
	
	private final static int FOLDER = 0x01;
	private final static int MODIFIED = 0x02;
	private final static int ADDED = 0x04;
	final static int PERSISTED = 0x08;

	public Node(ZipModel model, String name, boolean isFolder) {
		if (model == null)
			throw new NullPointerException();
		if (name == null)
			throw new NullPointerException();
		this.model = model;
		this.name = name;
		state |= isFolder ? FOLDER : 0;
		this.time = System.currentTimeMillis();
	}

	public Object accept(NodeVisitor visitor, Object argument) throws IOException {
		Object result = visitor.visit(this, argument);
		if (!visitor.canceled)
			acceptChildren(visitor, visitor.propagateResult ? result : argument);
		return result;
	}

	private void acceptChildren(NodeVisitor visitor, Object argument) throws IOException {
		if (children != null) {
			for (int i = 0; i < children.size(); i++) {
				Node child = (Node) children.get(i);
				child.accept(visitor, argument);
				if (visitor.canceled)
					break;
			}
		}
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
		if (name == null)
			throw new NullPointerException();
		if (name.equals(this.name))
			return;
		this.name = name;
		resetPathCache();
		setModified(true);
	}
	
	public String getPath() {
		if (path == null) {
			if (parent == null) {
				path = new String();
			} else {
				StringBuffer sb = new StringBuffer(parent.getPath());
				if (isFolder()) {
					sb.append(name);
					sb.append('/');
				}
				path = sb.toString();
			}
		}
		return path;
	}

	public String getFullPath() {
		if (fullPath == null) {
			StringBuffer sb = new StringBuffer(getPath());
			if (!isFolder())
				sb.append(name);
			fullPath = sb.toString();
		}
		return fullPath;
	}

	public String getType() {
		int index = name.lastIndexOf('.');
		return index != -1 ? name.substring(index + 1) : ""; //$NON-NLS-1$
	}

	public boolean isFolder() {
		return (state & FOLDER) > 0;
	}

	public boolean isPersistedFolder() {
		return isFolder() && (state & PERSISTED) > 0;
	}

	public void setPersistedFolder(boolean persisted) {
		if (persisted == isPersistedFolder())
			return;
		state = (persisted ? state | PERSISTED : state & -1 ^ PERSISTED);
		setModified(true);
	}

	public boolean isModified() {
		return (state & MODIFIED) > 0;
	}

	public boolean isAdded() {
		return (state & ADDED) > 0;
	}

	public long getTime() {
		return time;
	}
	
	public void setTime(long time) {
		if (time == this.time)
			return;
		this.time = time;
		setModified(true);
	}

	public long getSize() {
		if (size == -1) {
			try {
				size = IOUtils.copyLarge(getContent(), NullOutputStream.INSTANCE);
			} catch (IOException e) {
				ZipEditorPlugin.log(e);
			}
		}
		return size;
	}

	protected void setSize(long size) {
		this.size = size;
	}

	protected void setModified(boolean notify) {
		state |= MODIFIED;
		model.setDirty(true);
		if (notify)
			model.notifyListeners();
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
	
	public Object getProperty(Object key) {
		if (property == null)
			return null;
		return property.get(key);
	}
	
	public void setProperty(Object key, Object value) {
		if (property == null)
			property = new Hashtable();
		property.put(key, value);
	}

	public List getParentNodes() {
		return parentNodes;
	}

	public void setParentNodes(List parentNodes) {
		this.parentNodes = parentNodes;
	}

	public InputStream getContent() {
		try {
			InputStream content = doGetContent();
			if (content == null && parentNodes != null)
				content = determineContent();
			return content;
		} catch (Exception e) {
			model.logError(e);
			return null;
		}
	}
	
	protected InputStream doGetContent() throws IOException {
		if (file != null)
			return new FileInputStream(file);
		if (content != null)
			return new ByteArrayInputStream(content);
		return null;
	}
	
	protected void setContent(byte[] buf) {
		this.content = buf;
	}

	private InputStream determineContent() {
		InputStream parentContent = null;
		for (int i = 0; i < parentNodes.size(); i++) {
			Node parentNode = (Node) parentNodes.get(i);
			if (parentContent != null) {
				parentNode.getModel().setNodeContent(parentNode, parentContent);
			}
			parentContent = parentNode.getContent();
		}
		if (parentContent != null) {
			getModel().setNodeContent(this, parentContent);
			return getContent();
		} else {
			return null;
		}
	}

	private void internalAdd(Node node, int atIndex) {
		node.parent = this;
		if (children == null)
			children = new ArrayList();
		if (atIndex < 0)
			atIndex = children.size();
		children.add(atIndex, node);
		model.setDirty(true);
		model.notifyListeners();
		node.resetPathCache();
	}

	public void add(Node node, Node beforeSibling) {
		internalAdd(node, children != null && beforeSibling != null ? children.indexOf(beforeSibling) : -1);
	}
	
	public void add(File file, Node beforeSibling, IProgressMonitor monitor) {
		Node node = create(model, file.getName(), file.isDirectory());
		add(node, beforeSibling);
		node.state |= ADDED;
		if (node.isFolder()) {
			node.time = file.lastModified();
			File[] files = file.listFiles();
			if (files != null) {
				for (int i = 0; i < files.length; i++) {
					if (monitor.isCanceled())
						break;
					monitor.subTask(files[i].getName());
					node.add(files[i], null, monitor);
				}
			}
			node.state |= MODIFIED;
		} else {
			node.updateContent(file);
			monitor.worked(1);
		}
	}
	
	public void updateContent(File file) {
		this.file = file;
		time = file.lastModified();
		size = file.length();
		setModified(true);
	}
	
	/**
	 * @param entry the entry that updates the node 
	 */
	public void update(Object entry) {
		// does nothing not knowing the type of entry
	}

	public void remove(Node node) {
		if (children == null)
			return;
		children.remove(node);
		model.setDirty(true);
		model.notifyListeners();
		node.clear();
	}
	
	private void clear() {
		if (children != null) {
			for (Iterator it = children.iterator(); it.hasNext();) {
				((Node) it.next()).clear();
				it.remove();
			}
			children = null;
		}
	}
	
	public void reset() {
		model.deleteFile(file);
		file = null;
		state &= -1 ^ MODIFIED;
		model.notifyListeners();
	}

	private void resetPathCache() {
		path = fullPath = null;
		if (children != null) {
			for (int i = 0; i < children.size(); i++)
				((Node) children.get(i)).resetPathCache();
		}
	}

	public abstract Node create(ZipModel model, String name, boolean isFolder);

	public String toString() {
		return getFullPath();
	}

	public Object getAdapter(Class adapter) {
		if (Node.class == adapter)
			return this;
		if (IWorkbenchAdapter.class == adapter)
			return new NodeWorkbenchAdapter(this);
		return super.getAdapter(adapter);
	}
}
