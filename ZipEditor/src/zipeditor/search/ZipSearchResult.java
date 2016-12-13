/*
 * (c) Copyright 2002, 2016 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.search;

import java.io.File;
import java.text.MessageFormat;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.IEditorMatchAdapter;
import org.eclipse.search.ui.text.IFileMatchAdapter;
import org.eclipse.ui.model.IWorkbenchAdapter;

import zipeditor.ZipEditorPlugin;

public class ZipSearchResult extends AbstractTextSearchResult {

	private ZipSearchQuery fQuery;

	public ZipSearchResult(ZipSearchQuery query) {
		fQuery = query;
	}

	public ImageDescriptor getImageDescriptor() {
		return ZipEditorPlugin.getImageDescriptor("icons/zipicon.gif"); //$NON-NLS-1$
	}

	public String getLabel() {
		ZipSearchOptions options = fQuery.getOptions();
		Object[] args = {fQuery.getOptions().getPattern(), Integer.valueOf(getMatchCount()), getElementsText(options.getElements())};
		String key;
		if (options.getPattern().length() == 0) {
			key = "ZipSearchResult.1"; //$NON-NLS-1$
			args[0] = fQuery.getOptions().getNodeNamePattern();
		} else {
			key = "ZipSearchResult.0"; //$NON-NLS-1$
		}
		return MessageFormat.format(SearchMessages.getString(key), args);
	}

	private String getElementsText(List elements) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0, n = Math.min(elements.size(), 2); i < n; i++) {
			if (sb.length() > 0)
				sb.append(", "); //$NON-NLS-1$
			Object element = elements.get(i);
			if (element instanceof IAdaptable) {
				IAdaptable adaptable = (IAdaptable) element;
				IWorkbenchAdapter adapter = (IWorkbenchAdapter) adaptable.getAdapter(IWorkbenchAdapter.class);
				if (adapter != null)
					sb.append('\'').append(adapter.getLabel(element)).append('\'');
			} else if (element instanceof File) {
				sb.append('\'').append(((File) element).getName()).append('\'');
			}
		}
		if (elements.size() > 2)
			sb.append(", ..."); //$NON-NLS-1$
		if (sb.length() == 0)
			sb.append(SearchMessages.getString("ZipSearchResult.2")); //$NON-NLS-1$
		return sb.toString();
	}

	public ISearchQuery getQuery() {
		return fQuery;
	}

	public String getTooltip() {
		return null;
	}

	public IEditorMatchAdapter getEditorMatchAdapter() {
		return null;
	}

	public IFileMatchAdapter getFileMatchAdapter() {
		return null;
	}
}
