/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPropertyPage;

import zipeditor.model.TarNode;
import zipeditor.model.TarNodeProperty;

public class TarNodePropertyPage extends NodePropertyPage implements IWorkbenchPropertyPage {
	private Text fGroupId;
	private Text fGroupName;
	private Text fUserId;
	private Text fUserName;
	
	protected Control createContents(Composite parent) {
		Composite control = (Composite) createPropertiesSection(parent);

		TarNode node = (TarNode) getNode();
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
		
		applyDialogFont(control);
		return control;
	}
	
	private String saveText(String text) {
		return text != null ? text : new String();
	}

}
