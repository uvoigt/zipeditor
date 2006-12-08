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
import zipeditor.model.ZipNode;
import zipeditor.model.ZipNodeProperty;

public class ZipNodePropertyPage extends PropertyPage implements IWorkbenchPropertyPage {
	private Text fName;
	private Text fPath;
	private Text fType;
	private Text fAttributes;
	private Text fDate;
	private Text fSize;
	private Text fPackedSize;
	private Text fRatio;
	private Text fCrc;
	private Text fComment;
	
	protected Control createContents(Composite parent) {
		Composite control = new Composite(parent, SWT.NONE);
		control.setLayout(new GridLayout(2, false));

		ZipNode node = getZipNode();
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
		createLabel(control, ZipNodeProperty.PATTR.toString(), 1);
		fAttributes = createText(control, 30, 1, false);
		fAttributes.setText(new String(node.getExtra()));
		createLabel(control, NodeProperty.PDATE.toString(), 1);
		fDate = createText(control, 30, 1, false);
		fDate.setText(formatDate(node.getTime()));
		createLabel(control, NodeProperty.PSIZE.toString(), 1);
		fSize = createText(control, 30, 1, false);
		fSize.setText(formatSize(node.getSize()));
		createLabel(control, ZipNodeProperty.PPACKED_SIZE.toString(), 1);
		fPackedSize = createText(control, 30, 1, false);
		fPackedSize.setText(formatSize(node.getCompressedSize()));
		createLabel(control, ZipNodeProperty.PRATIO.toString(), 1);
		fRatio = createText(control, 30, 1, false);
		fRatio.setText(Long.toString(Math.max(Math.round(node.getRatio()), 0)) + "%"); //$NON-NLS-1$
		createLabel(control, ZipNodeProperty.PCRC.toString(), 1);
		fCrc = createText(control, 30, 1, false);
		fCrc.setText(Long.toHexString(node.getCrc()).toUpperCase());
		createLabel(control, ZipNodeProperty.PCOMMENT.toString(), 1);
		fComment = createText(control, 30, 1, true);
		if (node.getComment() != null)
			fComment.setText(node.getComment());
		
		return control;
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

	private ZipNode getZipNode() {
		return (ZipNode) getElement().getAdapter(ZipNode.class);
	}

	public boolean performOk() {
		ZipNode node = getZipNode();
		String comment = fComment.getText();
		node.setComment(comment.length() > 0 ? comment : null);
		String name = fName.getText();
		node.setName(name.length() > 0 ? name : null);
		return super.performOk();
	}
}
