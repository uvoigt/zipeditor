/*
 * (c) Copyright 2002, 2016 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.search;

import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
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
	private Set fModels = new HashSet();

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
						if (element instanceof Node && !(element instanceof PlainNode))
							fModels.add(((Node) element).getModel());
					} else if (fViewer instanceof TreeViewer) {
						Node node = (Node) element;
						element = createTreeElement(node);
						if (element != null)
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
				Map children = (Map) fTreeChildren.get(parentElement);
				return children != null ? children.keySet().toArray() : new Object[0];
			} else {
				return result.getElements();
			}
		} else if (parentElement instanceof Element) {
			Map children = (Map) fTreeChildren.get(parentElement);
			return children != null ? children.keySet().toArray() : new Object[0];
		} else {
			return super.getChildren(parentElement);
		}
	}

	protected Object[] getNodeChildren(Node node) {
		if (!(node instanceof PlainNode))
			fModels.add(node.getModel());
		return super.getNodeChildren(node);
	}

	private Object createTreeElement(Node node) {
		Element root = null;
		int elementType = node instanceof PlainNode ? Element.FOLDER : Element.ZIP;
		List parentNodes = node.getParentNodes();
		if (parentNodes != null && parentNodes.size() > 0) {
			ZipModel model = ((Node) parentNodes.get(0)).getModel();
			File file = model.getZipPath();
			root = (Element) fElementsToFiles.get(file);
			if (root == null) {
				root = createRootElement(file, elementType);
				fElementsToFiles.put(file, root);
			}
			Element element = root;
			for (int j = 0; j < parentNodes.size(); j++) {
				Node parentNode = (Node) parentNodes.get(j);
				Element child = new Element(element, parentNode.getPath(), parentNode.getName(),
						Long.valueOf(parentNode.getSize()), Long.valueOf(parentNode.getTime()), Element.ZIP);
				Map children = (Map) fTreeChildren.get(element);
				if (children != null && children.containsKey(child))
					child = (Element) children.get(child);
				else
					addChild(element, child);
				element = child;
			}
			addChild(element, node);
		}
		if (root == null) {
			File file = node.getModel().getZipPath();
			root = (Element) fElementsToFiles.get(file);
			boolean rootWasNull = root == null;
			if (rootWasNull) {
				root = createRootElement(file, elementType);
				fElementsToFiles.put(file, root);
			}
			addChild(root, node);
			if (!rootWasNull)
				return null;
		}
		return root;
	}

	private Element createRootElement(File file, int type) {
		Element root = null;
		List path = ((ZipSearchQuery) fSearchResult.getQuery()).getOptions().getPath();
		IPath searchPath = path != null && !path.isEmpty() ? Path.fromPortableString(((File) path.get(0)).getAbsolutePath()) : null;
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
				root = new Element(fSearchResult, wsFile.getFullPath().toString(), wsFile.getName(), size, lastModified, type);
			}
		}
		if (root == null) {
			if (type == Element.FOLDER) {
				File folder = file.getParentFile();
				IPath relativeTo = Path.fromPortableString(folder.getAbsolutePath()).makeRelativeTo(searchPath);
				root = new Element(fSearchResult, folder.getAbsolutePath(), relativeTo.toString(), null, Long.valueOf(folder.lastModified()), type);
			} else {
				root = new Element(fSearchResult, file.getAbsolutePath(), file.getName(), Long.valueOf(file.length()), Long.valueOf(file.lastModified()), type);
			}
		}
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

	protected void disposeModels() {
		for (Iterator it = fModels.iterator(); it.hasNext();) {
			ZipModel model = (ZipModel) it.next();
			model.dispose();
		}
		fModels.clear();
	}

	private void initTree() {
		Object[] elements = fSearchResult.getElements();
		for (int i = 0; i < elements.length; i++) {
			Node node = (Node) elements[i];
			createTreeElement(node);
		}
	}

	private void addChild(Object parent, Object child) {
		Map children = (Map) fTreeChildren.get(parent);
		if (children == null) {
			children = new LinkedHashMap();
			fTreeChildren.put(parent, children);
		}
		if (child instanceof Node)
			((Node) child).setProperty("parent", parent); //$NON-NLS-1$
		children.put(child, child);
	}

	private void removeChild(Object child) {
		Object parent = null;
		if (child instanceof Node) {
			parent = ((Node) child).getProperty("parent"); //$NON-NLS-1$
		} else if (child instanceof Element) {
			parent = ((Element) child).getParent(null);
		}
		Map siblings = (Map) fTreeChildren.get(parent);
		if (siblings != null) {
			siblings.remove(child);
			if (siblings.isEmpty()) {
				removeChild(parent);
				fViewer.refresh();
			}
		}
	}
}
