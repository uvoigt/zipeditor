package zipeditor.preferences;

import org.eclipse.jface.preference.*;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbench;
import zipeditor.PreferenceConstants;
import zipeditor.ZipEditorPlugin;

/**
 * This class represents a preference page that
 * is contributed to the Preferences dialog. By 
 * subclassing <samp>FieldEditorPreferencePage</samp>, we
 * can use the field support built into JFace that allows
 * us to create a page that is small and knows how to 
 * save, restore and apply itself.
 * <p>
 * This page is used to modify preferences only. They
 * are stored in the preference store that belongs to
 * the main plug-in class. That way, preferences can
 * be accessed directly via the preference store.
 */

public class ZipEditorPreferencePage
	extends FieldEditorPreferencePage
	implements IWorkbenchPreferencePage, IPropertyChangeListener {

	private ComboFieldEditor zstdLibComboFieldEditor;

	public ZipEditorPreferencePage() {
		super(GRID);
		setPreferenceStore(ZipEditorPlugin.getDefault().getPreferenceStore());
		setDescription("Generic Zip Editor Settings"); //$NON-NLS-1$
	}
	
	/**
	 * Creates the field editors. Field editors are abstractions of
	 * the common GUI blocks needed to manipulate various types
	 * of preferences. Each field editor knows how to save and
	 * restore itself.
	 */
	public void createFieldEditors() {
		addField(new BooleanFieldEditor(
				PreferenceConstants.PREFIX_EDITOR + PreferenceConstants.ACTIVATE_ZSTD_LIB,
				"Activate Zstd Support", //$NON-NLS-1$
				getFieldEditorParent()));
		
		zstdLibComboFieldEditor = new ComboFieldEditor(PreferenceConstants.PREFIX_EDITOR + PreferenceConstants.SELECTED_ZSTD_LIB, "Zstd Library", PreferenceUtils.libs, getFieldEditorParent()); //$NON-NLS-1$
		addField(zstdLibComboFieldEditor);
		zstdLibComboFieldEditor.setEnabled(PreferenceUtils.isZstdActive(), getFieldEditorParent());
	
		getPreferenceStore().addPropertyChangeListener(this);
		
	}
	
	@Override
	public void propertyChange(PropertyChangeEvent event) {
		super.propertyChange(event);

		if (event.getSource() instanceof BooleanFieldEditor) {
			BooleanFieldEditor booleanFieldEditor = (BooleanFieldEditor) event.getSource();
			if ((PreferenceConstants.PREFIX_EDITOR + PreferenceConstants.ACTIVATE_ZSTD_LIB).equals(booleanFieldEditor.getPreferenceName())) {
				zstdLibComboFieldEditor.setEnabled((boolean) event.getNewValue(), getFieldEditorParent());
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