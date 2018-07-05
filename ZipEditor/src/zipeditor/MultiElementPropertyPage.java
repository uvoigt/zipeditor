package zipeditor;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PropertyPage;

public abstract class MultiElementPropertyPage extends PropertyPage {
	protected interface PropertyAccessor {
		Object getPropertyValue(Object object);
	};

	protected class MultiplePropertyAccessor implements PropertyAccessor {
		private PropertyDescriptor[] fDescriptors;
		private String fPropertyName;

		public MultiplePropertyAccessor(Class clazz) {
			try {
				fDescriptors = Introspector.getBeanInfo(clazz).getPropertyDescriptors();
			} catch (Exception e) {
				ZipEditorPlugin.log(e);
			}
		}

		public Object getPropertyValue(Object object) {
			for (int i = 0; i < fDescriptors.length; i++) {
				if (!fDescriptors[i].getName().equals(fPropertyName))
					continue;
				try {
					return fDescriptors[i].getReadMethod().invoke(object, null);
				} catch (Exception e) {
					ZipEditorPlugin.log(e);
				}
			}
			return null;
		}

		public PropertyAccessor getAccessor(String property) {
			fPropertyName = property;
			return this;
		}
		
	};

	protected static class TriStateCheckbox {
		static final int NONE = -1;
		static final int UNSELECTED = 0;
		static final int SELECTED = 1;
		static final int MULTIPLE = 2;

		private Button btn;
		private int state;
		protected TriStateCheckbox(Button button) {
			btn = button;
			btn.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent event) {
					state++;
					if (state > MULTIPLE) {
						state = UNSELECTED;
					}
					update();
				}
			});
		}
		private void update() {
			switch (state) {
			case UNSELECTED:
				btn.setSelection(false);
				btn.setGrayed(false);
				break;
			case SELECTED:
				btn.setSelection(true);
				btn.setGrayed(false);
				break;
			case MULTIPLE:
				btn.setSelection(true);
				btn.setGrayed(true);
				break;
			}
		}

		public void setState(int state) {
			this.state = state;
			update();
		}
		public int getState() {
			return btn.getEnabled() ? btn.getGrayed() ? MULTIPLE : btn.getSelection() ? SELECTED : UNSELECTED : NONE;
		}
		public void setText(String string) {
			btn.setText(string);
		}
		public void setEnabled(boolean enabled) {
			btn.setEnabled(enabled);
		}
	}

	private IAdaptable[] fElements;

	protected String nonEqualStringLabel = Messages.getString("MultiElementPropertyPage.0"); //$NON-NLS-1$

	public void setElements(IAdaptable[] elements) {
		fElements = elements;
	}
	
	public void setElement(IAdaptable element) {
		if (element != null)
			fElements = new IAdaptable[] { element };
	}
	
	public IAdaptable[] getElements() {
		return fElements;
	}

	protected void setFieldText(Text field, PropertyAccessor accessor) {
		setFieldText(field, accessor, false);
	}

	protected void setFieldText(Text field, PropertyAccessor accessor, boolean summate) {
		Object[] values = new Object[fElements.length];
		Number sum = null;
		for (int i = 0; i < fElements.length; i++) {
			values[i] = accessor.getPropertyValue(fElements[i]);
			if (summate && values[i] instanceof Number) {
				sum = new Long(((Number) values[i]).longValue() + (sum != null ? sum.longValue() : 0));
			}
		}
		final Object unequalValue = new Object();
		Object singularValue = values.length > 0 ? values[0] : null;
		for (int i = 1; i < values.length; i++) {
			singularValue = values[i];
			if (values[i] == values[i - 1] || values[i] != null && values[i].equals(values[i - 1]))
				continue;
			singularValue = unequalValue;
			break;
		}
		if (sum != null) {
			field.setText(formatLong(sum.longValue()));
		} else if (singularValue == unequalValue) {
			field.setText(nonEqualStringLabel);
		} else if (singularValue != null) {
			field.setText(singularValue.toString());
		}
	}

	protected void select(TriStateCheckbox checkbox, PropertyAccessor accessor) {
		boolean isUnequal = false;
		Object previousValue = fElements.length > 0 ? accessor.getPropertyValue(fElements[0]) : null;
		Object singularValue = null;
		for (int i = 0; i < fElements.length; i++) {
			singularValue = accessor.getPropertyValue(fElements[i]);
			if (singularValue == previousValue || singularValue != null && singularValue.equals(previousValue))
				continue;
			previousValue = singularValue;
			isUnequal = true;
		}
		if (isUnequal) {
			checkbox.setState(2);
		} else if (singularValue != null) {
			checkbox.setState(singularValue instanceof Boolean && ((Boolean) singularValue).booleanValue() ? 1 : 0);
		}
	}

	protected void select(Combo combo, PropertyAccessor accessor) {
		boolean isUnequal = false;
		Object previousValue = fElements.length > 0 ? accessor.getPropertyValue(fElements[0]) : null;
		Object singularValue = null;
		for (int i = 0; i < fElements.length; i++) {
			singularValue = accessor.getPropertyValue(fElements[i]);
			if (singularValue == previousValue || singularValue != null && singularValue.equals(previousValue))
				continue;
			previousValue = singularValue;
			isUnequal = true;
		}
		if (isUnequal) {
			combo.select(0);
		} else if (singularValue != null) {
			int index = combo.indexOf(singularValue.toString());
			combo.select(index);
		}
	}

	protected String formatLong(long value) {
		return ZipLabelProvider.formatLong(value);
	}
}
