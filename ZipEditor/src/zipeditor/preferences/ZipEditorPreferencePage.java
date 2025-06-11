package zipeditor.preferences;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.*;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
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
	private ScaleFieldEditor zstdCompressionLevelFieldEditor;
	private Label fLScaleValue;

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
				GridDataFactory.fillDefaults().span(2, 1).applyTo(lLibraryName);
				lLibraryName.setText(Messages.ZipEditorPreferencePage_ZstdLibrary + ' ' + availableLibraries[0][0]);
				
				
				if (PreferenceUtils.JNI_LIBRARY.equals(availableLibraries[0][1])) {
					addZstdCompressionLevelField();
				}
			} else {
				String selectedZstdLibPrefName = PreferenceConstants.PREFIX_EDITOR + PreferenceConstants.SELECTED_ZSTD_LIB;
				zstdLibComboFieldEditor = new ComboFieldEditor(selectedZstdLibPrefName, Messages.ZipEditorPreferencePage_ZstdLibrary, availableLibraries, getFieldEditorParent()) {
					@Override
					protected void valueChanged(String oldValue, String newValue) {
						super.valueChanged(oldValue, newValue);
						
						boolean isVisible = PreferenceUtils.JNI_LIBRARY.equals(newValue);
						zstdCompressionLevelFieldEditor.getScaleControl().setVisible(isVisible);
						zstdCompressionLevelFieldEditor.getLabelControl(getFieldEditorParent()).setVisible(isVisible);
						fLScaleValue.setVisible(isVisible);
						
						zstdCompressionLevelFieldEditor.getScaleControl().setEnabled(isVisible);
						zstdCompressionLevelFieldEditor.getLabelControl(getFieldEditorParent()).setEnabled(isVisible);
						fLScaleValue.setEnabled(isVisible);
					}
				};
				addField(zstdLibComboFieldEditor);
				zstdLibComboFieldEditor.setEnabled(PreferenceUtils.isZstdActive(), getFieldEditorParent());
				getPreferenceStore().addPropertyChangeListener(this);

				addZstdCompressionLevelField();
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

	private void addZstdCompressionLevelField() {
		String zstdCompressionLevelPrefName = PreferenceConstants.PREFIX_EDITOR + PreferenceConstants.COMPRESSION_LEVEL;
		zstdCompressionLevelFieldEditor = new ScaleFieldEditor(zstdCompressionLevelPrefName, Messages.ZipEditorPreferencePage_ZstdCompressionLevel, getFieldEditorParent());
		addField(zstdCompressionLevelFieldEditor);
		zstdCompressionLevelFieldEditor.setMaximum(0);
		zstdCompressionLevelFieldEditor.setMaximum(22);
		zstdCompressionLevelFieldEditor.setIncrement(1);
		fLScaleValue = new Label(getFieldEditorParent(), SWT.WRAP);
		GridDataFactory.fillDefaults().span(2, 1).align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(fLScaleValue);
		zstdCompressionLevelFieldEditor.getScaleControl().addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				int selection = zstdCompressionLevelFieldEditor.getScaleControl().getSelection();
				fLScaleValue.setText(Messages.ZipEditorPreferencePage_ScaleValueCompressionLevel + selection);
			}
		});
		
		zstdCompressionLevelFieldEditor.load();
		fLScaleValue.setText(Messages.ZipEditorPreferencePage_ScaleValueCompressionLevel + PreferenceUtils.getCompressionLevel());
		
		zstdCompressionLevelFieldEditor.getScaleControl().setVisible(PreferenceUtils.isJNIZstdSelected());
		zstdCompressionLevelFieldEditor.getLabelControl(getFieldEditorParent()).setVisible(PreferenceUtils.isJNIZstdSelected());
		fLScaleValue.setVisible(PreferenceUtils.isJNIZstdSelected());
		
		if (PreferenceUtils.isJNIZstdSelected()) {
			boolean isVisible = PreferenceUtils.isZstdAvailableAndActive();
			
			zstdCompressionLevelFieldEditor.getScaleControl().setEnabled(isVisible);
			zstdCompressionLevelFieldEditor.getLabelControl(getFieldEditorParent()).setEnabled(isVisible);
			fLScaleValue.setEnabled(isVisible);
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		super.propertyChange(event);

		if (event.getSource() instanceof BooleanFieldEditor booleanFieldEditor) {
			if ((PreferenceConstants.PREFIX_EDITOR + PreferenceConstants.ACTIVATE_ZSTD_LIB).equals(booleanFieldEditor.getPreferenceName())) {
				boolean isEnabled = (boolean) event.getNewValue();
				if (zstdLibComboFieldEditor != null) {
					zstdLibComboFieldEditor.setEnabled(isEnabled, getFieldEditorParent());
				} 
				if (zstdCompressionLevelFieldEditor != null) {
					zstdCompressionLevelFieldEditor.setEnabled(isEnabled, getFieldEditorParent());
				}
				if (fLScaleValue != null) {
					fLScaleValue.setEnabled(isEnabled);
				}
				if (fBooleanFieldEditor != null) {
					fBooleanFieldEditor.setEnabled(isEnabled, getFieldEditorParent());
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