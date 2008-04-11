/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import zipeditor.model.Node;
import zipeditor.model.NodeProperty;

public abstract class NodePropertyPage extends MultiElementPropertyPage {
	private Text fName;
	private Text fPath;
	private Text fType;
	private Text fDate;
	private Text fSize;
	
	protected Control createPropertiesSection(Composite parent) {
		Group control = new Group(parent, SWT.NONE);
		control.setLayout(new GridLayout(2, false));

		MultiplePropertyAccessor accessor = new MultiplePropertyAccessor(Node.class);

		createLabel(control, NodeProperty.PNAME.toString(), 1);
		fName = createText(control, 30, 1, true);
		setFieldText(fName, accessor.getAccessor("name")); //$NON-NLS-1$
		createLabel(control, NodeProperty.PTYPE.toString(), 1);
		fType = createText(control, 30, 1, false);

		setFieldText(fType, new PropertyAccessor() {
			public Object getPropertyValue(Object object) {
				return ZipLabelProvider.getTypeLabel((Node) object);
			}
		});
		createLabel(control, NodeProperty.PPATH.toString(), 1);
		fPath = createText(control, 30, 1, false);
		setFieldText(fPath, accessor.getAccessor("path")); //$NON-NLS-1$
		
		createLabel(control, NodeProperty.PDATE.toString(), 1);
		fDate = createText(control, 30, 1, true);
		setFieldText(fDate, new PropertyAccessor() {
			public Object getPropertyValue(Object object) {
				return formatDate(((Node) object).getTime());
			}
		});
		createLabel(control, NodeProperty.PSIZE.toString(), 1);
		fSize = createText(control, 30, 1, false);
		setFieldText(fSize, new PropertyAccessor() {
			public Object getPropertyValue(Object object) {
				return formatSize(((Node) object).getSize());
			}
		});

		return control;
	}
	
	protected Node[] getNodes() {
		IAdaptable[] elements = getElements();
		Node[] nodes = new Node[elements.length];
		for (int i = 0; i < nodes.length; i++) {
			nodes[i] = (Node) elements[i].getAdapter(Node.class);
		}
		return nodes;
	}

	protected String formatDate(long time) {
		return DateFormat.getDateTimeInstance().format(new Long(time));
	}

	protected long parseDate(String string) throws ParseException {
		return DateFormat.getDateTimeInstance().parse(string).getTime();
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
		Node[] nodes = getNodes();
		String name = fName.getText();
		Long time = null;
		if (!nonEqualStringLabel.equals(fDate.getText())) {
			try {
				time = new Long(parseDate(fDate.getText()));
				setErrorMessage(null);
			} catch (ParseException e) {
				setErrorMessage(Messages.getFormattedString("NodePropertyPage.0", fDate.getText())); //$NON-NLS-1$
				return false;
			}
		}
		for (int i = 0; i < nodes.length; i++) {
			if (!nonEqualStringLabel.equals(name))
				nodes[i].setName(name.length() > 0 ? name : null);
			if (time != null)
				nodes[i].setTime(time.longValue());
		}
		return super.performOk();
	}

}