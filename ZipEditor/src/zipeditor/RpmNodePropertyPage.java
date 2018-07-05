/*
 * (c) Copyright 2002, 2017 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPropertyPage;

public class RpmNodePropertyPage extends NodePropertyPage implements IWorkbenchPropertyPage {
	private Text TODO;

	protected Control createContents(Composite parent) {
		Composite control = (Composite) createPropertiesSection(parent);
		// TODO
		applyDialogFont(control);
		return control;
	}

}
