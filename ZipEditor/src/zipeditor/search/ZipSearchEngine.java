/*
 * (c) Copyright 2002, 2016 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.search;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.IContentType;

import zipeditor.ZipEditorPlugin;
import zipeditor.actions.StringMatcher;
import zipeditor.model.IModelInitParticipant;
import zipeditor.model.Node;
import zipeditor.model.ZipContentDescriber;
import zipeditor.model.ZipModel;

public class ZipSearchEngine implements IModelInitParticipant {
	private final List fFileNames = ZipContentDescriber.getFileNamesAssociatedWithArchives();
	private final List fFileExtensions = ZipContentDescriber.getFileExtensionsAssociatedWithArchives();
	private char[] fPattern;
	private String fEncoding;
	private StringMatcher[] fNodeNameMatchers;
	private boolean fCaseSensitive;
	private boolean fNonArchives;
	private ZipSearchResultCollector fCollector;
	private IProgressMonitor fMonitor;
	private final List fParentNodes = new ArrayList();

	public IStatus search(ZipSearchOptions options, List elements, ZipSearchResultCollector collector) {
		IProgressMonitor monitor = collector.getfMonitor();
		monitor.beginTask("", IProgressMonitor.UNKNOWN); //$NON-NLS-1$

		if (options.getNodeNamePattern() != null && options.getNodeNamePattern().length() > 0) {
			String[] patterns = options.getNodeNamePattern().split(","); //$NON-NLS-1$
			fNodeNameMatchers = new StringMatcher[patterns.length];
			for (int i = 0; i < patterns.length; i++) {
				fNodeNameMatchers[i] = new StringMatcher(patterns[i].trim(), true, false);
			}
		}
		fCaseSensitive = options.isCaseSensitive();
		fNonArchives = options.isNonArchives();
		String pattern = fCaseSensitive ? options.getPattern() : options.getPattern().toLowerCase();
		fPattern = pattern.toCharArray();
		fEncoding = options.getEncoding();
		fCollector = collector;
		fMonitor = monitor;

		final IContentType archiveContentType = ZipContentDescriber.getArchiveContentType();

		for (Iterator it = elements.iterator(); it.hasNext();) {
			Object element = it.next();
			ZipModel model = null;

			try {
				if (element instanceof IFile) {
					model = createModelForFile((IFile) element, options.isNonArchives(), archiveContentType);
				} else if (element instanceof File) {
					searchPlainFile((File) element, options.isNonArchives(), monitor);
				} else if (element instanceof IResource) {
					searchResource((IResource) element, options.isNonArchives(), archiveContentType, monitor);
				} else if (element instanceof IAdaptable) {
					element = ((IAdaptable) element).getAdapter(IResource.class);
					if (element instanceof IResource)
						searchResource((IResource) element, options.isNonArchives(), archiveContentType, monitor);
				} else if (element instanceof Node) {
					Node node = (Node) element;
					if (node.getModel().getZipPath() != null)
						model = node.getModel();
				}
			} catch (CoreException e) {
				ZipEditorPlugin.log(e);
			}
			internalSearch(model, monitor);
		}
		monitor.done();
		return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
	}

	private ZipModel createModelForFile(IFile file, boolean nonArchives, IContentType archiveContentType) throws CoreException {
		IContentDescription description = file.getContentDescription();
		IContentType contentType = description != null ? description.getContentType() : null;
		IContentType baseType = contentType != null ? contentType.getBaseType() : null;
		if (baseType != null && baseType.equals(archiveContentType)) {
			return new ZipModel(file.getLocation().toFile(), null);
		} else if (nonArchives) {
			return new PlainModel(file.getLocation().toFile(), null);
		}
		return null;
	}

	private void searchPlainFile(File file, boolean nonArchives, IProgressMonitor monitor) {
		if (file.isDirectory()) {
			File[] children = file.listFiles();
			if (children != null) {
				String taskName = MessageFormat.format(SearchMessages.getString("ZipSearchEngine.1"), new Object[] {file.getName()}); //$NON-NLS-1$
				monitor.setTaskName(taskName);
				for (int i = 0; i < children.length; i++) {
					if (monitor.isCanceled())
						break;
					searchPlainFile(children[i], nonArchives, monitor);
				}
			}
		} else if (file.isFile() && ZipContentDescriber.matchesFileSpec(file.getName(), fFileNames, fFileExtensions)) {
			internalSearch(new ZipModel(file, null), monitor);
		} else if (file.isFile() && nonArchives) {
			internalSearch(new PlainModel(file, null), monitor);
		}
	}

	private void searchResource(IResource resource, final boolean nonArchives, final IContentType archiveContentType, final IProgressMonitor monitor) throws CoreException {
		resource.accept(new IResourceVisitor() {
			public boolean visit(IResource resource) throws CoreException {
				if (monitor.isCanceled())
					throw new OperationCanceledException();

				if (resource instanceof IContainer) {
					monitor.subTask(resource.getFullPath().toString());
				} else if (resource instanceof IFile) {
					internalSearch(createModelForFile((IFile) resource, nonArchives, archiveContentType), monitor);
				}
				return true;
			}
		}, IResource.DEPTH_INFINITE, false);
	}

	private void internalSearch(ZipModel zipModel, IProgressMonitor monitor) {
		if (zipModel != null) {
			if (!(zipModel instanceof PlainModel)) {
				String taskName = MessageFormat.format(SearchMessages.getString("ZipSearchEngine.0"), new Object[] {zipModel.getZipPath().getName()}); //$NON-NLS-1$
				monitor.setTaskName(taskName);
			}
			searchZipModel(zipModel);
			monitor.worked(1);
		}
	}

	private void searchZipModel(ZipModel model) {
		model.init(this);
	}

	private void searchNode(List parentNodes, Node node, InputStream inputStream, StringMatcher[] nodeNameMatchers, char[] pattern,
			String encoding, boolean caseSensitive, boolean nonArchives, ZipSearchResultCollector collector, IProgressMonitor monitor) {
		if (!monitor.isCanceled() && !node.isFolder()) {
			try {
				searchNodeContent(parentNodes, node, inputStream, nodeNameMatchers, pattern, encoding, caseSensitive, nonArchives, collector);
			} catch (IOException e) {
				node.getModel().logError(e);
			}
		}
	}

	private void searchNodeContent(List parentNodes, Node node, InputStream in, StringMatcher[] nodeNameMatchers,
			char pattern[], String encoding, boolean caseSensitive, boolean nonArchives, ZipSearchResultCollector collector) throws IOException {

		if (!nonArchives && ZipContentDescriber.matchesFileSpec(node.getName(), fFileNames, fFileExtensions)) {
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
		searchNode(fParentNodes, node, inputStream, fNodeNameMatchers, fPattern, fEncoding, fCaseSensitive, fNonArchives, fCollector, fMonitor);
	}

	public List getParentNodes() {
		return fParentNodes;
	}
}