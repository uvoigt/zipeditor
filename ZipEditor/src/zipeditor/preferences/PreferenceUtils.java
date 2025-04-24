package zipeditor.preferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.preference.IPreferenceStore;

import zipeditor.PreferenceConstants;
import zipeditor.ZipEditorPlugin;
import zipeditor.model.zstd.ZstdUtilities;

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
	 * Returns the available libraries. its a two dimensional array as the libs above but contains only the available ones.
	 * @return the array of available libraries.
	 */
	public static String[][] getAvailableLibraries() {
		List<String[]> availableLibs = new ArrayList<String[]>();
		List<String[]> asList = Arrays.asList(libs);
		for (String[] libData : asList) {
			if (libData[1].equals(JNI_LIBRARY) && ZstdUtilities.isZstdJniCompressionAvailable()) {
				availableLibs.add(libData);
			} else if (libData[1].equals(AIRCOMPRESSOR) && ZstdUtilities.isAircompressorAvailable()) {
				availableLibs.add(libData);
			}
		}
		return availableLibs.toArray(new String[0][]);
	}

	/**
	 * @return true, if the zstd handling is active.
	 */
	static boolean isZstdActive() {
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

	/**
	 * Checks if any zstd library is available and the zstd active pref is true.
	 * 
	 * @return
	 */
	public static boolean isZstdAvailableAndActive() {
		return isZstdActive() && getAvailableLibraries().length > 0;
	}

	/**
	 * Checks if the selected library is available and returns it. 
	 * If the selected library is not available it returns the available one.
	 * 
	 * @return the selected or available library.
	 */
	public static String getSelectedOrAvailableLibrary() {
		String[][] availableLibraries = getAvailableLibraries();
		if (availableLibraries.length == 0) {
			return null;
		}
		String selectedZstdLib = getSelectedZstdLib();
		for (String[] strings : availableLibraries) {
			if (selectedZstdLib.equals(strings[1])) {
				return strings[1];
			}
		}
		
		return availableLibraries[0][1];
	}
}
