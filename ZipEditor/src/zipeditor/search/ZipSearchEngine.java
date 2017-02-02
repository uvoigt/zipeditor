/*
 * (c) Copyright 2002, 2016 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.search;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.content.IContentType;

import zipeditor.actions.StringMatcher;
import zipeditor.model.IModelInitParticipant;
import zipeditor.model.Node;
import zipeditor.model.ZipModel;

public class ZipSearchEngine implements IModelInitParticipant {
	private final IContentType fArchiveContentType = Platform.getContentTypeManager().getContentType("ZipEditor.archive"); //$NON-NLS-1$
	private char[] fPattern;
	private String fEncoding;
	private StringMatcher[] fNodeNameMatchers;
	private boolean fCaseSensitive;
	private ZipSearchResultCollector fCollector;
	private IProgressMonitor fMonitor;
	private final List fParentNodes = new ArrayList();

	public IStatus search(ZipSearchOptions options, ZipModel[] models, ZipSearchResultCollector collector) {
		IProgressMonitor monitor = collector.getfMonitor();
		monitor.beginTask("", models.length); //$NON-NLS-1$

		if (options.getNodeNamePattern() != null && options.getNodeNamePattern().length() > 0) {
			String[] patterns = options.getNodeNamePattern().split(","); //$NON-NLS-1$
			fNodeNameMatchers = new StringMatcher[patterns.length];
			for (int i = 0; i < patterns.length; i++) {
				fNodeNameMatchers[i] = new StringMatcher(patterns[i].trim(), true, false);
			}
		}
		fCaseSensitive = options.isCaseSensitive();
		String pattern = fCaseSensitive ? options.getPattern() : options.getPattern().toLowerCase();
		fPattern = pattern.toCharArray();
		fEncoding = options.getEncoding();
		fCollector = collector;
		fMonitor = monitor;

		for (int i = 0; i < models.length; i++) {
			Object[] args = { models[i].getZipPath().getName() };
			String taskName = MessageFormat.format(SearchMessages.getString("ZipSearchEngine.0"), args); //$NON-NLS-1$
			monitor.setTaskName(taskName);
			searchZipModel(models[i]);
			if (monitor.isCanceled())
				break;
			monitor.worked(1);
		}
		monitor.done();
		return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
	}

	private void searchZipModel(ZipModel model) {
		model.init(this);
	}

	private void searchNode(List parentNodes, Node node, InputStream inputStream, StringMatcher[] nodeNameMatchers, char[] pattern,
			String encoding, boolean caseSensitive, ZipSearchResultCollector collector, IProgressMonitor monitor) {
		if (!monitor.isCanceled() && !node.isFolder()) {
			try {
				searchNodeContent(parentNodes, node, inputStream, nodeNameMatchers, pattern, encoding, caseSensitive, collector);
			} catch (IOException e) {
				node.getModel().logError(e);
			}
		}
	}

	private void searchNodeContent(List parentNodes, Node node, InputStream in, StringMatcher[] nodeNameMatchers,
			char pattern[], String encoding, boolean caseSensitive, ZipSearchResultCollector collector) throws IOException {

		IContentType contentType = Platform.getContentTypeManager().findContentTypeFor(node.getName());
		if (contentType != null && contentType.isKindOf(fArchiveContentType)) {
			ZipModel model = new ZipModel(null, in);
			parentNodes.add(node);
			searchZipModel(model);
			parentNodes.remove(node);
		} else {
			boolean matches = false;
			if (nodeNameMatchers != null) {
				for (int i = 0; i < nodeNameMatchers.length; i++) {
					if (nodeNameMatchers[i].match(node.getName())) {
						matches = true;
						break;
					}
				}
			} else {
				matches = true;
			}
			if (!matches)
				return;
			if (pattern.length > 0)
				searchPlainNodeContent(parentNodes, node, in, pattern, encoding, caseSensitive, collector);
			else
				collector.accept(parentNodes, node, true, 0, 0);
		}
	}

	private void searchPlainNodeContent(List parentNodes, Node node, InputStream in, char[] pattern,
			String encoding, boolean caseSensitive, ZipSearchResultCollector collector) throws IOException {

		int patternOffset = 0;
		int hitOffset = 0;
		int realOffset = 0;

		char buffer[] = new char[8192];
		int count;
		int completeCount = 0;
		InputStreamReader reader = new InputStreamReader(in, encoding);
		do {
			for (;(count = reader.read(buffer, completeCount, buffer.length - completeCount)) != -1
							&& (completeCount += count) != buffer.length;)
				;

			char findChar = pattern[patternOffset];
			int bufOffset = Math.max(patternOffset, hitOffset); // k2
			while (bufOffset < completeCount) {
				char bufChar = caseSensitive ? buffer[bufOffset] : Character.toLowerCase(buffer[bufOffset]);
				if (bufChar == findChar) {
					if (++patternOffset < pattern.length) {
						findChar = pattern[patternOffset];
					} else {
						findChar = pattern[patternOffset = 0];
						hitOffset = bufOffset;
						collector.accept(parentNodes, node, false, realOffset + bufOffset - pattern.length + 1, pattern.length);
					}
				} else {
					if (patternOffset > 0) {
						findChar = pattern[patternOffset = 0];
						bufOffset--;
					}
				}
				bufOffset++;
			}
			realOffset += completeCount;

			if (patternOffset != 0 && completeCount == buffer.length && count != -1) {
				int shiftOffset = buffer.length - patternOffset;
				System.arraycopy(buffer, shiftOffset, buffer, 0, patternOffset);
				completeCount = patternOffset;
				hitOffset = hitOffset >= shiftOffset ? (hitOffset - shiftOffset) + 1 : 0;
				realOffset -= patternOffset;
			} else {
				completeCount = 0;
				hitOffset = 0;
			}
		} while (count != -1);
	}

	public void streamAvailable(InputStream inputStream, Node node) {
		searchNode(fParentNodes, node, inputStream, fNodeNameMatchers, fPattern, fEncoding, fCaseSensitive, fCollector, fMonitor);
	}

	public List getParentNodes() {
		return fParentNodes;
	}
}