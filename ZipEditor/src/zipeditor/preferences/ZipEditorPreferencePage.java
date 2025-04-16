package zipeditor.preferences;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.*;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbench;
import zipeditor.PreferenceConstants;
import zipeditor.ZipEditorPlugin;
import zipeditor.model.zstd.ZstdUtilities;

/**
 * This is the Preference Page for changing the zstd library for reading / writing a zstd compressed stream.
 * The zstd libraries are optional so the zstd handling can also be disabled, which will lead to loose of the zstd compressed data, after saving.
 */
public class ZipEditorPreferencePage
	extends FieldEditorPreferencePage
	implements IWorkbenchPreferencePage, IPropertyChangeListener {

	private ComboFieldEditor zstdLibComboFieldEditor;
	private Label fLibValidationLabel;
	private Composite fZstdLibSelectionComposite;
	private BooleanFieldEditor zstdStateFieldEditor;

	public ZipEditorPreferencePage() {
		super(GRID);
		setPreferenceStore(ZipEditorPlugin.getDefault().getPreferenceStore());
		setDescription("Generic Zip Editor Settings"); //$NON-NLS-1$
	}
	
	public void createFieldEditors() {
		zstdStateFieldEditor = new BooleanFieldEditor(
				PreferenceConstants.PREFIX_EDITOR + PreferenceConstants.ACTIVATE_ZSTD_LIB,
				"Activate Zstd Support", //$NON-NLS-1$
				getFieldEditorParent());
		addField(zstdStateFieldEditor);
		zstdStateFieldEditor.load();
		
		fZstdLibSelectionComposite = new Composite(getFieldEditorParent(), SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(fZstdLibSelectionComposite);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(fZstdLibSelectionComposite);
		
		zstdLibComboFieldEditor = new ComboFieldEditor(PreferenceConstants.PREFIX_EDITOR + PreferenceConstants.SELECTED_ZSTD_LIB, "Zstd Library", PreferenceUtils.libs, fZstdLibSelectionComposite); //$NON-NLS-1$
		addField(zstdLibComboFieldEditor);
		zstdLibComboFieldEditor.setEnabled(PreferenceUtils.isZstdActive(), fZstdLibSelectionComposite);
		
		
		GridDataFactory.fillDefaults().grab(true, false).applyTo(new Label(fZstdLibSelectionComposite, SWT.WRAP));

		fLibValidationLabel = new Label(fZstdLibSelectionComposite, SWT.WRAP);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(fLibValidationLabel);

		String selectedLib = getPreferenceStore().getString(PreferenceConstants.PREFIX_EDITOR + PreferenceConstants.SELECTED_ZSTD_LIB);
		updateValidationLabel(selectedLib);
		getPreferenceStore().addPropertyChangeListener(this);
		
	}

	private void updateValidationLabel(String selectedLib) {
		if (zstdStateFieldEditor.getBooleanValue()) {
			if (PreferenceUtils.JNI_LIBRARY.equals(selectedLib)) {
				setValidationLabel(ZstdUtilities.isZstdJniCompressionAvailable());
			} else {
				setValidationLabel(ZstdUtilities.isAircompressorAvailable());
			}
		}
	}

	private void setValidationLabel(boolean compressionLibAvailable) {
		String lMessage = compressionLibAvailable ? Messages.ZipEditorPreferencePage_LibAvailable : Messages.ZipEditorPreferencePage_LibNotAvailable;
		Color lColor = compressionLibAvailable ? Display.getDefault().getSystemColor(SWT.COLOR_BLACK) : Display.getDefault().getSystemColor(SWT.COLOR_RED);

		fLibValidationLabel.setText(lMessage);
		fLibValidationLabel.setForeground(lColor);
	}
	
	@Override
	public void propertyChange(PropertyChangeEvent event) {
		super.propertyChange(event);

		if (event.getSource() instanceof BooleanFieldEditor booleanFieldEditor) {
			if ((PreferenceConstants.PREFIX_EDITOR + PreferenceConstants.ACTIVATE_ZSTD_LIB).equals(booleanFieldEditor.getPreferenceName())) {
				zstdLibComboFieldEditor.setEnabled((boolean) event.getNewValue(), fZstdLibSelectionComposite);
				fLibValidationLabel.setVisible((boolean) event.getNewValue());
			}
		} else if (event.getSource() instanceof ComboFieldEditor comboFieldEditor) {
			if ((PreferenceConstants.PREFIX_EDITOR + PreferenceConstants.SELECTED_ZSTD_LIB).equals(comboFieldEditor.getPreferenceName())) {
				updateValidationLabel((String) event.getNewValue());
			}
		}
	}
	
	@Override
	public void dispose() {
		super.dispose();
		
		getPreferenceStore().removePropertyChangeListener(this);
	}

	@Override
	public void init(IWorkbench workbench) {
	}
	
}