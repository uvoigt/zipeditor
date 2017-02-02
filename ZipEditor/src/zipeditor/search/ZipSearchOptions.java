/*
 * (c) Copyright 2002, 2016 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.search;

import java.util.List;

public class ZipSearchOptions {
	public static final int SCOPE_SELECTED = 1;
	public static final int SCOPE_WORKSPACE = 2;
	public static final int SCOPE_FILESYSTEM = 3;

	private String fNodeNamePattern;
	private boolean fCaseSensitive;
	private String fPattern;
	private String fEncoding;
	private int fScope;
	private List fPath;
	private List fElements;

	public ZipSearchOptions(String nodeNamePattern, String pattern, String encoding, boolean caseSensitive, int scope) {
		fNodeNamePattern = nodeNamePattern;
		fPattern = pattern;
		fEncoding = encoding;
		fCaseSensitive = caseSensitive;
		fScope = scope;
	}

	public String getNodeNamePattern() {
		return fNodeNamePattern;
	}

	public void setNodeNamePattern(String nodeNamePattern) {
		fNodeNamePattern = nodeNamePattern;
	}

	public String getPattern() {
		return fPattern;
	}

	public void setPattern(String pattern) {
		fPattern = pattern;
	}

	public String getEncoding() {
		return fEncoding;
	}

	public void setEncoding(String encoding) {
		fEncoding = encoding;
	}

	public void setCaseSensitive(boolean caseSensitive) {
		fCaseSensitive = caseSensitive;
	}

	public boolean isCaseSensitive() {
		return fCaseSensitive;
	}

	public int getScope() {
		return fScope;
	}

	public void setScope(int scope) {
		fScope = scope;
	}

	public List getPath() {
		return fPath;
	}

	public void setPath(List path) {
		fPath = path;
	}

	public List getElements() {
		return fElements;
	}

	public void setElements(List elements) {
		fElements = elements;
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((fPattern == null) ? 0 : fPattern.hashCode());
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ZipSearchOptions other = (ZipSearchOptions) obj;
		if (fPattern == null) {
			if (other.fPattern != null)
				return false;
		} else if (!fPattern.equals(other.fPattern))
			return false;
		return true;
	}
}
