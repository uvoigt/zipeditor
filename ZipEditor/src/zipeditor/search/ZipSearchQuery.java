/*
 * (c) Copyright 2002, 2016 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.search;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.ISearchResult;

public class ZipSearchQuery implements ISearchQuery {

	private ZipSearchOptions fOptions;
	private ZipSearchResult fResult;
	private List fElements;

	public ZipSearchQuery(ZipSearchOptions options, List elements) {
		fOptions = options;
		fElements = elements;
    }

	public boolean canRerun() {
		return true;
	}

	public boolean canRunInBackground() {
		return true;
	}

	public String getLabel() {
		return SearchMessages.getString("ZipSearchQuery.0"); //$NON-NLS-1$
	}

	public ISearchResult getSearchResult() {
		if (fResult == null)
			fResult = new ZipSearchResult(this);
		return fResult;
	}

	public ZipSearchOptions getOptions() {
		return fOptions;
	}

	public IStatus run(IProgressMonitor monitor) throws OperationCanceledException {
		ZipSearchResult result = (ZipSearchResult) getSearchResult();
		result.removeAll();
		ZipSearchResultCollector collector = new ZipSearchResultCollector(result);
		collector.setProgressMonitor(monitor);
		ZipSearchEngine engine = new ZipSearchEngine();
		return engine.search(fOptions, fElements, collector);
	}
}
