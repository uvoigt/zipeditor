/*
 * (c) Copyright 2002, 2016 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.search;

import java.util.List;

import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.AbstractTextSearchViewPage;
import org.eclipse.search.ui.text.Match;
import org.eclipse.swt.graphics.Image;

import zipeditor.ZipEditorPlugin;
import zipeditor.ZipLabelProvider;
import zipeditor.model.Node;

public class ZipSearchLabelProvider extends ZipLabelProvider {

	static String getNodeText(Node node, List parentNodes, int matchCount) {
		StringBuilder sb = new StringBuilder();
		sb.append(node.toString());
		if (parentNodes != null) {
			sb.append(" ["); //$NON-NLS-1$
			if (parentNodes.size() > 0) {
				for (int i = 0; i < parentNodes.size(); i++) {
					Node parent = (Node) parentNodes.get(i);
					if (i == 0)
						sb.append(parent.getModel().getZipPath().getName());
					sb.append(">"); //$NON-NLS-1$
					sb.append(parent);
				}
			} else {
				sb.append(node.getModel().getZipPath().getName());
			}
			sb.append("]"); //$NON-NLS-1$
		}
		if (matchCount > 0)
			sb.append(" - ").append(matchCount); //$NON-NLS-1$
		return sb.toString();
	}

	private ZipSearchResultPage fPage;

	public ZipSearchLabelProvider(ZipSearchResultPage page) {
		fPage = page;
	}

	public String getColumnText(Object element, int columnIndex) {
		return getText(element);
	}

	public String getText(Object element) {
		if (element instanceof Node) {
			AbstractTextSearchResult searchResult = fPage.getInput();
			Match[] matches = searchResult.getMatches(element);
			Node node = (Node) element;
			List parentNodes = node.getParentNodes();
			if (fPage.getLayout() == AbstractTextSearchViewPage.FLAG_LAYOUT_TREE)
				parentNodes = null;
			return getNodeText(node, parentNodes, matches.length);
		} else if (element instanceof Element) {
			return ((Element) element).getFileName();
		}
		return null;
	}

	public Image getImage(Object element) {
		if (element instanceof Element) {
			return ZipEditorPlugin.getImage("icons/zipicon.gif"); //$NON-NLS-1$
		} else {
			return super.getImage(element);
		}
	}
}
