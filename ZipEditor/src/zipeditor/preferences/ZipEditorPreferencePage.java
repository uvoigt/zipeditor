package zipeditor.preferences;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.*;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
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
	private Composite fZstdLibSelectionComposite;
	private BooleanFieldEditor zstdStateFieldEditor;

	public ZipEditorPreferencePage() {
		super(GRID);
		setPreferenceStore(ZipEditorPlugin.getDefault().getPreferenceStore());
	}
	
	public void createFieldEditors() {
		Group group = new Group(getFieldEditorParent(), SWT.NONE);
		group.setText(Messages.ZipEditorPreferencePage_ZstdSettingsGroupTitle);
		
		GridDataFactory.fillDefaults().grab(true, true).align(SWT.FILL, SWT.FILL).applyTo(group);
		GridLayoutFactory.fillDefaults().numColumns(1).spacing(5, 5).margins(5, 5).applyTo(group);
		
		String[][] availableLibraries = PreferenceUtils.getAvailableLibraries();
		if (availableLibraries.length >= 1) {
			Composite composite = new Composite(group, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, true).align(SWT.FILL, SWT.FILL).applyTo(composite);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(composite);
			
			zstdStateFieldEditor = new BooleanFieldEditor(
					PreferenceConstants.PREFIX_EDITOR + PreferenceConstants.ACTIVATE_ZSTD_LIB,
					Messages.ZipEditorPreferencePage_ActivateZstdSupport,
					composite);
			addField(zstdStateFieldEditor);
			zstdStateFieldEditor.load();
			
			fZstdLibSelectionComposite = new Composite(composite, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, true).applyTo(fZstdLibSelectionComposite);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(fZstdLibSelectionComposite);

			if (availableLibraries.length == 1) {
				Label lLibField = new Label(fZstdLibSelectionComposite, SWT.NONE);
				lLibField.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
				lLibField.setText(Messages.ZipEditorPreferencePage_ZstdLibrary);
				
				Label lLibraryName = new Label(fZstdLibSelectionComposite, SWT.NONE);
				lLibraryName.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
				lLibraryName.setText(availableLibraries[0][0]);
			} else {
				zstdLibComboFieldEditor = new ComboFieldEditor(PreferenceConstants.PREFIX_EDITOR + PreferenceConstants.SELECTED_ZSTD_LIB, Messages.ZipEditorPreferencePage_ZstdLibrary, PreferenceUtils.libs, fZstdLibSelectionComposite);
				addField(zstdLibComboFieldEditor);
				zstdLibComboFieldEditor.setEnabled(PreferenceUtils.isZstdActive(), fZstdLibSelectionComposite);
				
				GridDataFactory.fillDefaults().grab(true, false).applyTo(new Label(fZstdLibSelectionComposite, SWT.WRAP));
		
				getPreferenceStore().addPropertyChangeListener(this);
			}
		} else {
			Composite composite = new Composite(group, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, true).align(SWT.FILL, SWT.FILL).applyTo(composite);
			GridLayoutFactory.fillDefaults().margins(5, 5).applyTo(composite);

			Label lHint = new Label(composite, SWT.WRAP);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(lHint);
			lHint.setText(Messages.ZipEditorPreferencePage_HintNoLibraries);
		}
		group.pack();
		group.layout(true);
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		super.propertyChange(event);

		if (zstdLibComboFieldEditor != null && event.getSource() instanceof BooleanFieldEditor booleanFieldEditor) {
			if ((PreferenceConstants.PREFIX_EDITOR + PreferenceConstants.ACTIVATE_ZSTD_LIB).equals(booleanFieldEditor.getPreferenceName())) {
				zstdLibComboFieldEditor.setEnabled((boolean) event.getNewValue(), fZstdLibSelectionComposite);
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
		// nothing to do..
	}
	
}