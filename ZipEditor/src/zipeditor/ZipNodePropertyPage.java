package zipeditor;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPropertyPage;

import zipeditor.model.ZipNode;
import zipeditor.model.ZipNodeProperty;

public class ZipNodePropertyPage extends NodePropertyPage implements IWorkbenchPropertyPage {
	private Text fAttributes;
	private Text fPackedSize;
	private Text fRatio;
	private Text fCrc;
	private Text fComment;
	
	protected Control createContents(Composite parent) {
		
		Composite control = (Composite) createPropertiesSection(parent);

		ZipNode node = (ZipNode) getNode();

		createLabel(control, ZipNodeProperty.PPACKED_SIZE.toString(), 1);
		fPackedSize = createText(control, 30, 1, false);
		fPackedSize.setText(formatSize(node.getCompressedSize()));
		createLabel(control, ZipNodeProperty.PRATIO.toString(), 1);
		fRatio = createText(control, 30, 1, false);
		fRatio.setText(Long.toString(Math.max(Math.round(node.getRatio()), 0)) + "%"); //$NON-NLS-1$
		createLabel(control, ZipNodeProperty.PCRC.toString(), 1);
		fCrc = createText(control, 30, 1, false);
		fCrc.setText(Long.toHexString(node.getCrc()).toUpperCase());
		createLabel(control, ZipNodeProperty.PATTR.toString(), 1);
		fAttributes = createText(control, 30, 1, false);
		fAttributes.setText(new String(node.getExtra()));
		createLabel(control, ZipNodeProperty.PCOMMENT.toString(), 1);
		fComment = createText(control, 30, 1, true);
		if (node.getComment() != null)
			fComment.setText(node.getComment());
		
		applyDialogFont(control);
		return control;
	}

	public boolean performOk() {
		ZipNode node = (ZipNode) getNode();
		String comment = fComment.getText();
		node.setComment(comment.length() > 0 ? comment : null);
		return super.performOk();
	}
}
