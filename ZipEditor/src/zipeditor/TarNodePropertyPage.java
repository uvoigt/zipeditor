/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPropertyPage;

import zipeditor.model.Node;
import zipeditor.model.TarNode;
import zipeditor.model.TarNodeProperty;

public class TarNodePropertyPage extends NodePropertyPage implements IWorkbenchPropertyPage {
	private Text fGroupId;
	private Text fGroupName;
	private Text fUserId;
	private Text fUserName;
	
	protected Control createContents(Composite parent) {
		Composite control = (Composite) createPropertiesSection(parent);

		MultiplePropertyAccessor accessor = new MultiplePropertyAccessor(TarNode.class);
		createLabel(control, TarNodeProperty.PGROUP_ID.toString(), 1);
		fGroupId = createText(control, 30, 1, true);
		setFieldText(fGroupId, accessor.getAccessor("groupId")); //$NON-NLS-1$
		createLabel(control, TarNodeProperty.PGROUP_NAME.toString(), 1);
		fGroupName = createText(control, 30, 1, true);
		setFieldText(fGroupName, accessor.getAccessor("groupName")); //$NON-NLS-1$
		createLabel(control, TarNodeProperty.PUSER_ID.toString(), 1);
		fUserId = createText(control, 30, 1, true);
		setFieldText(fUserId, accessor.getAccessor("userId")); //$NON-NLS-1$
		createLabel(control, TarNodeProperty.PUSER_NAME.toString(), 1);
		fUserName = createText(control, 30, 1, true);
		setFieldText(fUserName, accessor.getAccessor("userName")); //$NON-NLS-1$
		
		applyDialogFont(control);
		return control;
	}
	
	public boolean performOk() {
		boolean ok = super.performOk();
		if (!ok)
			return false;
		Node[] nodes = getNodes();
		Integer groupId = null;
		Integer userId = null;
		if (!nonEqualStringLabel.equals(fGroupId)) {
			try {
				groupId = new Integer(fGroupId.getText());
				setErrorMessage(null);
			} catch (Exception e) {
				setErrorMessage(Messages.getString("TarNodePropertyPage.0")); //$NON-NLS-1$
				return false;
			}
		}
		try {
			userId = new Integer(fUserId.getText());
			setErrorMessage(null);
		} catch (Exception e) {
			setErrorMessage(Messages.getString("TarNodePropertyPage.1")); //$NON-NLS-1$
			return false;
		}
		for (int i = 0; i < nodes.length; i++) {
			if (groupId != null)
				((TarNode) nodes[i]).setGroupId(groupId.intValue());
			if (!nonEqualStringLabel.equals(fGroupName.getText()))
				((TarNode) nodes[i]).setGroupName(fGroupName.getText());
			if (userId != null)
				((TarNode) nodes[i]).setUserId(userId.intValue());
			if (!nonEqualStringLabel.equals(fUserName.getText()))
				((TarNode) nodes[i]).setUserName(fUserName.getText());
		}
		return true;
	}
}
