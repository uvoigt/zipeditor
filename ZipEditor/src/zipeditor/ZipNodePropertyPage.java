package zipeditor;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPropertyPage;

import zipeditor.model.Node;
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

		createLabel(control, ZipNodeProperty.PPACKED_SIZE.toString(), 1);
		fPackedSize = createText(control, 30, 1, false);
		setFieldText(fPackedSize, new NodePropertyAccessor() {
			public Object getSinglePropertyValue(Node node) {
				return new Long(((ZipNode) node).getCompressedSize());
			}
		}, true);
		createLabel(control, ZipNodeProperty.PRATIO.toString(), 1);
		fRatio = createText(control, 30, 1, false);
		setFieldText(fRatio, new PropertyAccessor() {
			public Object getPropertyValue(Object object) {
				return formatLong(Math.max(Math.round(((ZipNode) object).getRatio()), 0)) + "%"; //$NON-NLS-1$
			}
		});
		createLabel(control, ZipNodeProperty.PCRC.toString(), 1);
		fCrc = createText(control, 30, 1, false);
		setFieldText(fCrc, new PropertyAccessor() {
			public Object getPropertyValue(Object object) {
				return Long.toHexString(((ZipNode) object).getCrc()).toUpperCase();
			}
		});
		createLabel(control, ZipNodeProperty.PATTR.toString(), 1);
		fAttributes = createText(control, 30, 1, false);
		setFieldText(fAttributes, new PropertyAccessor() {
			public Object getPropertyValue(Object object) {
				return new String(((ZipNode) object).getExtra());
			}
		});
		createLabel(control, ZipNodeProperty.PCOMMENT.toString(), 1);
		fComment = createText(control, 30, 1, true);
		setFieldText(fComment, new MultiplePropertyAccessor(ZipNode.class).getAccessor("comment")); //$NON-NLS-1$
		
		applyDialogFont(control);
		return control;
	}

	public boolean performOk() {
		boolean ok = super.performOk();
		if (!ok)
			return false;
		Node[] nodes = getNodes();
		String comment = fComment.getText();
		for (int i = 0; i < nodes.length; i++) {
			if (!nonEqualStringLabel.equals(comment))
				((ZipNode) nodes[i]).setComment(comment.length() > 0 ? comment : null);
		}
		return true;
	}
}
