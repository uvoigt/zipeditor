/*
 * (c) Copyright 2002, 2016 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.search;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;

import zipeditor.ZipContentProvider;
import zipeditor.model.Node;
import zipeditor.model.ZipModel;

public class ZipSearchContentProvider extends ZipContentProvider {

	private ZipSearchResult fSearchResult;
	private StructuredViewer fViewer;
	private Map fElementsToFiles = new HashMap();
	private Map fTreeChildren = new HashMap();

	public ZipSearchContentProvider(int viewMode) {
		super(viewMode);
	}

	public void elementsChanged(Object[] elements) {
		for (int i = 0; i < elements.length; i++) {
			Object element = elements[i];
			if (fSearchResult.getMatchCount(element) > 0) {
				if (fViewer.testFindItem(element) != null) {
					fViewer.refresh(element);
				} else {
					if (fViewer instanceof TableViewer) {
						((TableViewer) fViewer).add(element);
					} else if (fViewer instanceof TreeViewer) {
						Node node = (Node) element;
						element = createTreeElement(node);
						((TreeViewer) fViewer).add(fViewer.getInput(), element);
					}
				}
			} else {
				if (fViewer instanceof TableViewer) {
					((TableViewer) fViewer).remove(element);
				} else if (fViewer instanceof TreeViewer) {
					TreeViewer treeViewer = (TreeViewer) fViewer;
					treeViewer.remove(element);
					removeChild(element);
				}
			}
		}
	}

	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof ZipSearchResult) {
			ZipSearchResult result = (ZipSearchResult) parentElement;
			if (fViewer instanceof TreeViewer) {
				List children = (List) fTreeChildren.get(parentElement);
				return children != null ? children.toArray() : new Object[0];
			} else {
				return result.getElements();
			}
		} else if (parentElement instanceof Element) {
			List children = (List) fTreeChildren.get(parentElement);
			return children != null ? children.toArray() : new Object[0];
		} else {
			return super.getChildren(parentElement);
		}
	}

	private Element createTreeElement(Node node) {
		Element root = null;
		List parentNodes = node.getParentNodes();
		if (parentNodes.size() > 0) {
			ZipModel model = ((Node) parentNodes.get(0)).getModel();
			File file = model.getZipPath();
			root = (Element) fElementsToFiles.get(file);
			if (root == null) {
				root = createRootElement(file);
				fElementsToFiles.put(file, root);
			}
			Element element = root;
			for (int j = 0; j < parentNodes.size(); j++) {
				Node parentNode = (Node) parentNodes.get(j);
				Element child = new Element(element, parentNode.getPath(), parentNode.getName(),
						Long.valueOf(parentNode.getSize()), Long.valueOf(parentNode.getTime()));
				List children = (List) fTreeChildren.get(element);
				int childIndex = children != null ? children.indexOf(child) : -1;
				if (childIndex != -1)
					child = (Element) children.get(childIndex);
				else
					addChild(element, child);
				element = child;
			}
			addChild(element, node);
		}
		if (root == null) {
			File file = node.getModel().getZipPath();
			root = (Element) fElementsToFiles.get(file);
			if (root == null) {
				root = createRootElement(file);
				fElementsToFiles.put(file, root);
			}
			addChild(root, node);
		}
		return root;
	}

	private Element createRootElement(File file) {
		Element root = null;
		if (((ZipSearchQuery) fSearchResult.getQuery()).getOptions().getScope() != ZipSearchOptions.SCOPE_FILESYSTEM) {
			URI fileLocation = file.toURI();
			IFile[] workspaceFiles = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(fileLocation);
			if (workspaceFiles.length == 1) {
				Long size = null;
				Long lastModified = null;
				IFile wsFile = workspaceFiles[0];
				try {
					IFileInfo info = EFS.getStore(fileLocation).fetchInfo();
					size = Long.valueOf(info.getLength());
					lastModified = Long.valueOf(info.getLastModified());
				} catch (CoreException e) {
				}
				root = new Element(fSearchResult, wsFile.getFullPath().toString(), wsFile.getName(), size, lastModified);
			}
		}
		if (root == null)
			root = new Element(fSearchResult, file.getAbsolutePath(), file.getName(), Long.valueOf(file.length()), Long.valueOf(file.lastModified()));
		addChild(fSearchResult, root);
		return root;
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		fViewer = (StructuredViewer) viewer;
		fSearchResult = (ZipSearchResult) newInput;
		fElementsToFiles.clear();
		fTreeChildren.clear();
		if (newInput instanceof ZipSearchResult && fViewer instanceof TreeViewer) {
			initTree();
		}

		super.inputChanged(viewer, oldInput, newInput);
	}

	public void clear() {
		fElementsToFiles.clear();
		fTreeChildren.clear();
		fViewer.refresh();
	}

	private void initTree() {
		Object[] elements = fSearchResult.getElements();
		for (int i = 0; i < elements.length; i++) {
			Node node = (Node) elements[i];
			createTreeElement(node);
		}
	}

	private void addChild(Object parent, Object child) {
		List children = (List) fTreeChildren.get(parent);
		if (children == null) {
			children = new ArrayList();
			fTreeChildren.put(parent, children);
		}
		if (child instanceof Node)
			((Node) child).setProperty("parent", parent); //$NON-NLS-1$
		children.add(child);
	}

	private void removeChild(Object child) {
		Object parent = null;
		if (child instanceof Node) {
			parent = ((Node) child).getProperty("parent"); //$NON-NLS-1$
		} else if (child instanceof Element) {
			parent = ((Element) child).getParent(null);
		}
		List siblings = (List) fTreeChildren.get(parent);
		if (siblings != null) {
			siblings.remove(child);
			if (siblings.isEmpty()) {
				removeChild(parent);
				fViewer.refresh();
			}
		}
	}
}
