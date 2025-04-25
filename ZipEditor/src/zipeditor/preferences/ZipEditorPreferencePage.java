package zipeditor.preferences;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.*;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbench;
import zipeditor.PreferenceConstants;
import zipeditor.ZipEditorPlugin;

/**
 * This is the Preference Page for changing the zstd library for reading / writing a zstd compressed stream.
 * The zstd libraries are optional so the zstd handling can also be disabled, which will lead to loose of the zstd compressed data, after saving.
 */
public class ZipEditorPreferencePage
	extends FieldEditorPreferencePage
	implements IWorkbenchPreferencePage, IPropertyChangeListener {

	private ComboFieldEditor zstdLibComboFieldEditor;
	private BooleanFieldEditor zstdStateFieldEditor;
	private BooleanFieldEditor fBooleanFieldEditor;

	public ZipEditorPreferencePage() {
		super(GRID);
		setPreferenceStore(ZipEditorPlugin.getDefault().getPreferenceStore());
	}
	
	public void createFieldEditors() {
		String[][] availableLibraries = PreferenceUtils.getAvailableLibrariesForPreferenceUI();
		if (availableLibraries.length >= 1) {
			zstdStateFieldEditor = new BooleanFieldEditor(
					PreferenceConstants.PREFIX_EDITOR + PreferenceConstants.ACTIVATE_ZSTD_LIB,
					Messages.ZipEditorPreferencePage_ActivateZstdSupport,
					getFieldEditorParent());
			addField(zstdStateFieldEditor);
			
			if (availableLibraries.length == 1) {
				Label lLibraryName = new Label(getFieldEditorParent(), SWT.NONE);
				GridDataFactory.fillDefaults().applyTo(lLibraryName);
				lLibraryName.setText(Messages.ZipEditorPreferencePage_ZstdLibrary + ' ' + availableLibraries[0][0]);
			} else {
				String selectedZstdLibPrefName = PreferenceConstants.PREFIX_EDITOR + PreferenceConstants.SELECTED_ZSTD_LIB;
				zstdLibComboFieldEditor = new ComboFieldEditor(selectedZstdLibPrefName, Messages.ZipEditorPreferencePage_ZstdLibrary, availableLibraries, getFieldEditorParent());
				addField(zstdLibComboFieldEditor);
				zstdLibComboFieldEditor.setEnabled(PreferenceUtils.isZstdActive(), getFieldEditorParent());
				
				getPreferenceStore().addPropertyChangeListener(this);
			}
			
			String zstdDefaultPrefName = PreferenceConstants.PREFIX_EDITOR + PreferenceConstants.USE_ZSTD_AS_DEFAULT;
			fBooleanFieldEditor = new BooleanFieldEditor(zstdDefaultPrefName, Messages.ZipEditorPreferencePage_ZstdAsDefaultCompression, getFieldEditorParent());
			addField(fBooleanFieldEditor);
			fBooleanFieldEditor.setEnabled(PreferenceUtils.isZstdActive(), getFieldEditorParent());

		} else {
			Label lHint = new Label(getFieldEditorParent(), SWT.WRAP);
			lHint.setText(Messages.ZipEditorPreferencePage_HintNoLibraries);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(false, false).applyTo(lHint);
			lHint.pack(true);
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		super.propertyChange(event);

		if (event.getSource() instanceof BooleanFieldEditor booleanFieldEditor) {
			if ((PreferenceConstants.PREFIX_EDITOR + PreferenceConstants.ACTIVATE_ZSTD_LIB).equals(booleanFieldEditor.getPreferenceName())) {
				if (zstdLibComboFieldEditor != null) {
					zstdLibComboFieldEditor.setEnabled((boolean) event.getNewValue(), getFieldEditorParent());
				} 
				if (fBooleanFieldEditor != null) {
					fBooleanFieldEditor.setEnabled((boolean) event.getNewValue(), getFieldEditorParent());
				}
			}
		}
	}
	
	@Override
	public void dispose() {
		getPreferenceStore().removePropertyChangeListener(this);

		super.dispose();
	}

	@Override
	public void init(IWorkbench workbench) {
		// nothing to do..
	}
	
}