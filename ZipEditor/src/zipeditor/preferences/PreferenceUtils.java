package zipeditor.preferences;

import org.eclipse.jface.preference.IPreferenceStore;

import zipeditor.PreferenceConstants;
import zipeditor.ZipEditorPlugin;

public class PreferenceUtils {

	/**
	 * Preference value for the aircompressor library.
	 */
	public static final String AIRCOMPRESSOR = "aircompressor"; //$NON-NLS-1$
	
	/**
	 * Preference value for the zstd-jni library.
	 */
	public static final String JNI_LIBRARY = "jniLibrary"; //$NON-NLS-1$

	public static final String[][] libs = { { "JNI Library", JNI_LIBRARY }, { "Aircompressor", AIRCOMPRESSOR } }; //$NON-NLS-1$//$NON-NLS-2$

	private static String getSelectedZstdLib() {
		IPreferenceStore preferenceStore = ZipEditorPlugin.getDefault().getPreferenceStore();
		String selectedLib = preferenceStore
				.getString(PreferenceConstants.PREFIX_EDITOR + PreferenceConstants.SELECTED_ZSTD_LIB);
		return selectedLib;
	}

	/**
	 * @return true, if the zstd handling is active.
	 */
	public static boolean isZstdActive() {
		IPreferenceStore preferenceStore = ZipEditorPlugin.getDefault().getPreferenceStore();
		return preferenceStore.getBoolean(PreferenceConstants.PREFIX_EDITOR + PreferenceConstants.ACTIVATE_ZSTD_LIB);
	}

	/**
	 * @return true, if the zstd-jni library is selected for zstd handling.
	 */
	public static boolean isJNIZstdSelected() {
		String selectedLib = getSelectedZstdLib();

		return JNI_LIBRARY.equals(selectedLib);
	}

	/**
	 * @return true, if the aircompressor library is selected for zstd handling.
	 */
	public static boolean isAircompressorSelected() {
		String selectedLib = getSelectedZstdLib();

		return AIRCOMPRESSOR.equals(selectedLib);
	}
}
