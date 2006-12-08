/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor;

import java.text.DateFormat;
import java.text.NumberFormat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.PropertyPage;

import zipeditor.model.NodeProperty;
import zipeditor.model.TarNode;
import zipeditor.model.TarNodeProperty;

public class TarNodePropertyPage extends PropertyPage implements IWorkbenchPropertyPage {
	private Text fName;
	private Text fPath;
	private Text fType;
	private Text fDate;
	private Text fSize;
	private Text fGroupId;
	private Text fGroupName;
	private Text fUserId;
	private Text fUserName;
	
	protected Control createContents(Composite parent) {
		Composite control = new Composite(parent, SWT.NONE);
		control.setLayout(new GridLayout(2, false));

		TarNode node = getTarNode();
		createLabel(control, NodeProperty.PNAME.toString(), 1);
		fName = createText(control, 30, 1, true);
		fName.setText(node.getName());
		createLabel(control, NodeProperty.PPATH.toString(), 1);
		fPath = createText(control, 30, 1, false);
		fPath.setText(node.getPath());
		createLabel(control, NodeProperty.PTYPE.toString(), 1);
		fType = createText(control, 30, 1, false);
		Program program = Program.findProgram(node.getType());
		fType.setText(program != null ? program.getName() : Messages.getFormattedString("ZipNodePropertyPage.0", node.getType())); //$NON-NLS-1$
		createLabel(control, NodeProperty.PDATE.toString(), 1);
		fDate = createText(control, 30, 1, false);
		fDate.setText(formatDate(node.getTime()));
		createLabel(control, NodeProperty.PSIZE.toString(), 1);
		fSize = createText(control, 30, 1, false);
		fSize.setText(formatSize(node.getSize()));
		createLabel(control, TarNodeProperty.PGROUP_ID.toString(), 1);
		fGroupId = createText(control, 30, 1, false);
		fGroupId.setText(Integer.toString(node.getGroupId()));
		createLabel(control, TarNodeProperty.PGROUP_NAME.toString(), 1);
		fGroupName = createText(control, 30, 1, false);
		fGroupName.setText(saveText(node.getGroupName()));
		createLabel(control, TarNodeProperty.PUSER_ID.toString(), 1);
		fUserId = createText(control, 30, 1, false);
		fUserId.setText(Integer.toString(node.getUserId()));
		createLabel(control, TarNodeProperty.PUSER_NAME.toString(), 1);
		fUserName = createText(control, 30, 1, false);
		fUserName.setText(saveText(node.getUserName()));
		
		return control;
	}
	
	private String saveText(String text) {
		return text != null ? text : new String();
	}

	private String formatDate(long time) {
		return DateFormat.getDateTimeInstance().format(new Long(time));
	}

	private String formatSize(long size) {
		return NumberFormat.getNumberInstance().format(size);
	}

	private Label createLabel(Composite parent, String text, int hspan) {
		Label label = new Label(parent, SWT.LEFT);
		label.setText(text);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = hspan;
		label.setLayoutData(data);
		return label;
	}

	private Text createText(Composite parent, int width, int hspan, boolean editable) {
		Text text = new Text(parent, SWT.BORDER);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = hspan;
		data.widthHint = convertWidthInCharsToPixels(width);
		text.setLayoutData(data);
		text.setEditable(editable);
		return text;
	}

	private TarNode getTarNode() {
		return (TarNode) getElement().getAdapter(TarNode.class);
	}

	public boolean performOk() {
		TarNode node = getTarNode();
		String name = fName.getText();
		node.setName(name.length() > 0 ? name : null);
		return super.performOk();
	}
}
