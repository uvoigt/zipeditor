/*
 * (c) Copyright 2002, 20016 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.search;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PropertyPage;

public class ElementPropertyPage extends PropertyPage {

	private Text fEntry;
	private Text fPath;
	private Text fType;

	protected Control createContents(Composite parent) {
		Composite control = new Composite(parent, SWT.NONE);
		control.setLayout(new GridLayout(2, false));

		Element element = (Element) getElement();

		Label label = new Label(control, SWT.LEFT);
		label.setText(SearchMessages.getString("ElementPropertyPage.0")); //$NON-NLS-1$
		fEntry = new Text(control, SWT.LEFT | SWT.READ_ONLY);
		fEntry.setText(element.getFileName());

		label = new Label(control, SWT.LEFT);
		label.setText(SearchMessages.getString("ElementPropertyPage.1")); //$NON-NLS-1$
		fPath = new Text(control, SWT.LEFT | SWT.READ_ONLY);
		fPath.setText(element.getPath());

		label = new Label(control, SWT.LEFT);
		label.setText(SearchMessages.getString("ElementPropertyPage.2")); //$NON-NLS-1$
		fType = new Text(control, SWT.LEFT | SWT.READ_ONLY);
		IContentType contentType = Platform.getContentTypeManager().findContentTypeFor(element.getFileName());
		fType.setText(contentType.getName());
	
		applyDialogFont(control);
		return control;
	}
}
