/*
 * (c) Copyright 2010 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.actions;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.ui.IActionFilter;

public class ZipAdapterFactory implements IAdapterFactory {

	public Object getAdapter(Object adaptableObject, Class adapterType) {
		if (IActionFilter.class.equals(adapterType))
			return new ZipFileActionFilter();
		return adaptableObject;
	}

	public Class[] getAdapterList() {
		return new Class[] { IActionFilter.class, IFile.class };
	}

}
