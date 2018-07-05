/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor;

import java.text.ParseException;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import zipeditor.model.Node;
import zipeditor.model.NodeProperty;

public abstract class NodePropertyPage extends MultiElementPropertyPage {
	protected abstract class NodePropertyAccessor implements PropertyAccessor {
		public Object getPropertyValue(Object object) {
			Node node = (Node) object;
			if (node.isFolder()) {
				Node[] children = node.getChildren();
				Object aggregate = getSinglePropertyValue(node);
				for (int i = 0; i < children.length; i++) {
					aggregate = aggregatePropertyValues(getPropertyValue(children[i]), aggregate);
				}
				return aggregate;
			} else {
				return getSinglePropertyValue(node);
			}
		}
		
		protected Object aggregatePropertyValues(Object value1, Object value2) {
			if (value1 instanceof Number && value2 instanceof Number)
				return new Long(((Number) value1).longValue() + ((Number) value2).longValue());
			return value1;
		}

		protected abstract Object getSinglePropertyValue(Node node);
	}

	private Text fName;
	private Text fPath;
	private Text fType;
	private TriStateCheckbox fPersisted;
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
		Composite group = new Composite(control, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		layout.marginWidth = layout.marginHeight = 0;
		group.setLayout(layout);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = convertWidthInCharsToPixels(30);
		group.setLayoutData(data);
		fType = createText(group, 10, 1, false);
		setFieldText(fType, new PropertyAccessor() {
			public Object getPropertyValue(Object object) {
				return ZipLabelProvider.getTypeLabel((Node) object);
			}
		});
		Button button = createCheckbox(group);
		fPersisted = new TriStateCheckbox(button);
		fPersisted.setText(NodeProperty.PPERSISTED.toString());
		select(fPersisted, accessor.getAccessor("persistedFolder")); //$NON-NLS-1$
		Node[] nodes = getNodes();
		boolean allFolders = true;
		for (int i = 0; i < nodes.length; i++) {
			if (nodes[i].isFolder())
				continue;
			allFolders = false;
			break;
		}
		fPersisted.setEnabled(allFolders);

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
		setFieldText(fSize, new NodePropertyAccessor() {
			public Object getSinglePropertyValue(Node node) {
				return new Long(node.getSize());
			}
		}, true);

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
		return ZipLabelProvider.formatDate(time);
	}

	protected long parseDate(String string) throws ParseException {
		return ZipLabelProvider.DATE_FORMAT.parse(string).getTime();
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

	protected Combo createCombo(Composite parent, int width, int hspan) {
		Combo combo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = hspan;
		data.widthHint = convertWidthInCharsToPixels(width);
		combo.setLayoutData(data);
		combo.add(nonEqualStringLabel);
		return combo;
	}

	protected Button createCheckbox(Composite parent) {
		Button box = new Button(parent, SWT.CHECK);
		return box;
	}

	public boolean performOk() {
		Node[] nodes = getNodes();
		String name = fName.getText();
		Long time = null;
		String dateText = fDate.getText();
		if (dateText.trim().length() > 0 && !nonEqualStringLabel.equals(dateText)) {
			try {
				time = new Long(parseDate(dateText));
				setErrorMessage(null);
			} catch (ParseException e) {
				String pattern = ZipLabelProvider.DATE_FORMAT.format(new Long(System.currentTimeMillis()));
				setErrorMessage(Messages.getFormattedString("NodePropertyPage.0", new Object[] { dateText, pattern })); //$NON-NLS-1$
				return false;
			}
		}
		int persisted = fPersisted.getState();
		for (int i = 0; i < nodes.length; i++) {
			Node node = nodes[i];
			if (!nonEqualStringLabel.equals(name))
				node.setName(name);
			if (time != null)
				node.setTime(time.longValue());
			if (node.isFolder() && (persisted == TriStateCheckbox.SELECTED || persisted == TriStateCheckbox.UNSELECTED))
				node.setPersistedFolder(persisted == TriStateCheckbox.SELECTED);
		}
		return true;
	}

}