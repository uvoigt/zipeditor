/*
 * (c) Copyright 2010 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.actions;

import org.eclipse.ui.IActionFilter;

public class ZipFileActionFilter implements IActionFilter {

	public boolean testAttribute(Object target, String name, String value) {
		return false;
	}
}
