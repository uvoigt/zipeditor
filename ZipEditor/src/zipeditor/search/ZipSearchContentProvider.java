/*
 * (c) Copyright 2002, 2016 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.search;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
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

	public ZipSearchContentProvider(int viewMode) {
		super(viewMode);
	}

	public void elementsChanged(Object[] elements) {
		for (int i = 0; i < elements.length; i++) {
			if (fSearchResult.getMatchCount(elements[i]) > 0) {
				if (fViewer.testFindItem(elements[i]) != null) {
					fViewer.refresh(elements[i]);
				} else {
					if (fViewer instanceof TableViewer) {
						((TableViewer) fViewer).add(elements[i]);
					} else if (fViewer instanceof TreeViewer) {
						Node node = (Node) elements[i];
						Element element = createTreeElement(node);
						((TreeViewer) fViewer).add(fViewer.getInput(), element);
					}
				}
			} else {
				if (fViewer instanceof TableViewer)
					((TableViewer) fViewer).remove(elements[i]);
				else if (fViewer instanceof TreeViewer)
					((TreeViewer) fViewer).remove(elements[i]);
			}
		}
	}

	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof ZipSearchResult) {
			ZipSearchResult result = (ZipSearchResult) parentElement;
			if (fViewer instanceof TreeViewer) {
				Object[] elements = result.getElements();
				LinkedHashSet resultElements = new LinkedHashSet();
				for (int i = 0; i < elements.length; i++) {
					Node node = (Node) elements[i];
					Element element = createTreeElement(node);
					resultElements.add(element);
				}
				return resultElements.toArray();
			} else {
				Object[] nodes = result.getElements();
				for (int i = 0; i < nodes.length; i++) {
					addModel(((Node) nodes[i]).getModel());
				}
				return nodes;
			}
		} else if (parentElement instanceof Element) {
			Element element = (Element) parentElement;
			List result = new ArrayList();
			if (element.getChildren() != null)
				result.addAll(element.getChildren());
			if (element.getNodes() != null)
				result.addAll(element.getNodes());
			return result.toArray();
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
				Element child = new Element(parentNode.getPath(), parentNode.getName());
				int childIndex = element.getChildren() != null ? element.getChildren().indexOf(child) : -1;
				if (childIndex != -1)
					child = (Element) element.getChildren().get(childIndex);
				else
					element.addChild(child);
				element = child;
			}
			element.addNode(node);
		}
		if (root == null) {
			File file = node.getModel().getZipPath();
			root = (Element) fElementsToFiles.get(file);
			if (root == null) {
				root = createRootElement(file);
				fElementsToFiles.put(file, root);
				root.addNode(node);
			} else {
				root.addNode(node);
			}
		}
		return root;
	}

	private Element createRootElement(File file) {
		Element root;
		IFile[] workspaceFiles = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(file.toURI());
		if (workspaceFiles.length == 1)
			root = new Element(workspaceFiles[0].getFullPath().toString(), workspaceFiles[0].getName());
		else
			root = new Element(file.getAbsolutePath(), file.getName());
		return root;
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		fViewer = (StructuredViewer) viewer;
		fSearchResult = (ZipSearchResult) newInput;
		fElementsToFiles.clear();

		super.inputChanged(viewer, oldInput, newInput);
	}

	public void clear() {
		fElementsToFiles.clear();
		fViewer.refresh();
	}
}
