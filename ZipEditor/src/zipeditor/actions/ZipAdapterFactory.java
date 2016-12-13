/*
 * (c) Copyright 2010 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.actions;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.search.ui.ISearchPageScoreComputer;
import org.eclipse.ui.IActionFilter;

import zipeditor.search.ZipSearchScoreComputer;

public class ZipAdapterFactory implements IAdapterFactory {

	private ZipSearchScoreComputer fScoreComputer;

	public Object getAdapter(Object adaptableObject, Class adapterType) {
		if(adapterType == ISearchPageScoreComputer.class)
			return getSearchPageScoreComputer();

		return null;
	}

	public Class[] getAdapterList() {
		return new Class[] { IActionFilter.class, IFile.class, ISearchPageScoreComputer.class };
	}

	private Object getSearchPageScoreComputer() {
		if(fScoreComputer == null)
			fScoreComputer = new ZipSearchScoreComputer();
		return fScoreComputer;
	}
}
