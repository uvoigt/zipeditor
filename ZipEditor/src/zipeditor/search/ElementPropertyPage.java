/*
 * (c) Copyright 2002, 20016 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.search;

import java.text.DateFormat;
import java.text.NumberFormat;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.dialogs.PropertyPage;

public class ElementPropertyPage extends PropertyPage {

	private StyledText fEntry;
	private StyledText fPath;
	private StyledText fType;
	private StyledText fSize;
	private StyledText fLastModified;

	protected Control createContents(Composite parent) {
		Composite control = new Composite(parent, SWT.NONE);
		control.setLayout(new GridLayout(2, false));

		Element element = (Element) getElement().getAdapter(Element.class);

		Label label = new Label(control, SWT.LEFT);
		label.setText(SearchMessages.getString("ElementPropertyPage.0")); //$NON-NLS-1$
		label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		fEntry = new StyledText(control, SWT.LEFT | SWT.READ_ONLY);
		fEntry.setText(element.getFileName());
		fEntry.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		fEntry.setBackground(control.getBackground());

		label = new Label(control, SWT.LEFT);
		label.setText(SearchMessages.getString("ElementPropertyPage.1")); //$NON-NLS-1$
		label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		fPath = new StyledText(control, SWT.LEFT | SWT.READ_ONLY | SWT.WRAP);
		fPath.setText(element.getPath());
		GridData data = new GridData(SWT.FILL, SWT.TOP, true, false);
		data.widthHint = convertWidthInCharsToPixels(60);
		fPath.setLayoutData(data);
		fPath.setBackground(control.getBackground());

		label = new Label(control, SWT.LEFT);
		label.setText(SearchMessages.getString("ElementPropertyPage.2")); //$NON-NLS-1$
		label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		fType = new StyledText(control, SWT.LEFT | SWT.READ_ONLY);
		IContentType contentType = Platform.getContentTypeManager().findContentTypeFor(element.getFileName());
		fType.setText(contentType != null ? contentType.getName() :
			element.getType() == Element.FOLDER ? SearchMessages.getString("ElementPropertyPage.7") //$NON-NLS-1$
			: SearchMessages.getString("ElementPropertyPage.6")); //$NON-NLS-1$
		fType.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		fType.setBackground(control.getBackground());

		label = new Label(control, SWT.LEFT);
		label.setText(SearchMessages.getString("ElementPropertyPage.3")); //$NON-NLS-1$
		label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		fSize = new StyledText(control, SWT.LEFT | SWT.READ_ONLY);
		String size = element.getSize() != null ? NumberFormat.getNumberInstance().format(element.getSize()) + " " //$NON-NLS-1$
				+ SearchMessages.getString("ElementPropertyPage.5") : SearchMessages.getString("ElementPropertyPage.6"); //$NON-NLS-1$ //$NON-NLS-2$
		fSize.setText(size);
		fSize.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		fSize.setBackground(control.getBackground());

		label = new Label(control, SWT.LEFT);
		label.setText(SearchMessages.getString("ElementPropertyPage.4")); //$NON-NLS-1$
		label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		fLastModified = new StyledText(control, SWT.LEFT | SWT.READ_ONLY);
		String lastModified = element.getLastModified() != null ? DateFormat.getDateTimeInstance().format(element.getLastModified())
				: SearchMessages.getString("ElementPropertyPage.6"); //$NON-NLS-1$
		fLastModified.setText(lastModified);
		fLastModified.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		fLastModified.setBackground(control.getBackground());

		applyDialogFont(control);
		return control;
	}
}
