/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor;

import java.text.DateFormat;
import java.text.NumberFormat;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PropertyPage;

import zipeditor.model.Node;
import zipeditor.model.NodeProperty;

public abstract class NodePropertyPage extends PropertyPage {
	private Text fName;
	private Text fPath;
	private Text fType;
	private Text fDate;
	private Text fSize;
	
	protected Control createPropertiesSection(Composite parent) {
		Group control = new Group(parent, SWT.NONE);
		control.setLayout(new GridLayout(2, false));
		
		Node node = getNode();
	
		createLabel(control, NodeProperty.PNAME.toString(), 1);
		fName = createText(control, 30, 1, true);
		fName.setText(node.getName());
		createLabel(control, NodeProperty.PTYPE.toString(), 1);
		fType = createText(control, 30, 1, false);
		Program program = Program.findProgram(node.getType());
		IContentType contentType = Platform.getContentTypeManager().findContentTypeFor(node.getName());
		fType.setText(contentType != null ? contentType.getName() : program != null ? program.getName() : Messages.getFormattedString("ZipNodePropertyPage.0", node.getType())); //$NON-NLS-1$
		createLabel(control, NodeProperty.PPATH.toString(), 1);
		fPath = createText(control, 30, 1, false);
		fPath.setText(node.getPath());
		
		createLabel(control, NodeProperty.PDATE.toString(), 1);
		fDate = createText(control, 30, 1, false);
		fDate.setText(formatDate(node.getTime()));
		createLabel(control, NodeProperty.PSIZE.toString(), 1);
		fSize = createText(control, 30, 1, false);
		fSize.setText(formatSize(node.getSize()));

		return control;
	}
	
	protected Node getNode() {
		return (Node) getElement().getAdapter(Node.class);
	}

	protected String formatDate(long time) {
		return DateFormat.getDateTimeInstance().format(new Long(time));
	}

	protected String formatSize(long size) {
		return NumberFormat.getNumberInstance().format(size);
	}

	protected Label createLabel(Composite parent, String text, int hspan) {
		Label label = new Label(parent, SWT.LEFT);
		label.setText(text);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = hspan;
		label.setLayoutData(data);
		return label;
	}

	protected Text createText(Composite parent, int width, int hspan, boolean editable) {
		Text text = new Text(parent, SWT.BORDER);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = hspan;
		data.widthHint = convertWidthInCharsToPixels(width);
		text.setLayoutData(data);
		text.setEditable(editable);
		return text;
	}
	
	public boolean performOk() {
		Node node = getNode();
		String name = fName.getText();
		node.setName(name.length() > 0 ? name : null);
		return super.performOk();
	}

}