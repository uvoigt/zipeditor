package zipeditor.preferences;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.preference.IPreferenceStore;

import zipeditor.PreferenceConstants;
import zipeditor.ZipEditorPlugin;
import zipeditor.model.zstd.ZstdUtilities;

public class PreferenceUtils {

	public record ZstdLibrary(String label, String identifier) {
		public ZstdLibrary(String label, String identifier) {
			this.label = label;
			this.identifier = identifier;
		}
	}
	/**
	 * Preference value for the aircompressor library.
	 */
	public static final String AIRCOMPRESSOR = "aircompressor"; //$NON-NLS-1$
	
	/**
	 * Preference value for the zstd-jni library.
	 */
	public static final String JNI_LIBRARY = "jniLibrary"; //$NON-NLS-1$

	public static final List<ZstdLibrary> libraries = new ArrayList<PreferenceUtils.ZstdLibrary>();
	static {
		libraries.add(new ZstdLibrary(Messages.PreferenceUtils_ZSTDJniLibraryLabel, JNI_LIBRARY));
		libraries.add(new ZstdLibrary(Messages.PreferenceUtils_AircompressorLabel, AIRCOMPRESSOR));
	}
	
	private static String getSelectedZstdLib() {
		IPreferenceStore preferenceStore = ZipEditorPlugin.getDefault().getPreferenceStore();
		String selectedLib = preferenceStore
				.getString(PreferenceConstants.PREFIX_EDITOR + PreferenceConstants.SELECTED_ZSTD_LIB);
		return selectedLib;
	}

	/**
	 * Returns the available libraries.
	 * @return a {@link List} of available {@link ZstdLibrary} objects.
	 */
	public static List<ZstdLibrary> getAvailableLibraries() {
		List<ZstdLibrary> availableLibs = new ArrayList<ZstdLibrary>();
		for (ZstdLibrary libData : libraries) {
			if (libData.identifier.equals(JNI_LIBRARY) && ZstdUtilities.isZstdJniCompressionAvailable()) {
				availableLibs.add(libData);
			} else if (libData.identifier.equals(AIRCOMPRESSOR) && ZstdUtilities.isAircompressorAvailable()) {
				availableLibs.add(libData);
			}
		}
		return availableLibs;
	}
	
	/**
	 * Returns the available libraries.
	 * @return a {@link List} of available {@link ZstdLibrary} objects.
	 */
	public static String[][] getAvailableLibrariesForPreferenceUI() {
		List<ZstdLibrary> availableLibraries = getAvailableLibraries();
		String[][] libs = new String[availableLibraries.size()][2];
		for (int i = 0; i < availableLibraries.size(); i++) {
			ZstdLibrary zstdLibrary = availableLibraries.get(i);
			libs[i] = new String[] { zstdLibrary.label, zstdLibrary.identifier };
		}
		return libs;
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
		return isZstdActive() && getAvailableLibraries().size() > 0;
	}
	
	public static boolean isZstdDefaultCompression() {
		IPreferenceStore preferenceStore = ZipEditorPlugin.getDefault().getPreferenceStore();
		return preferenceStore.getBoolean(PreferenceConstants.PREFIX_EDITOR + PreferenceConstants.USE_ZSTD_AS_DEFAULT);
	}

	/**
	 * Checks if the selected library is available and returns it. 
	 * If the selected library is not available it returns the available one.
	 * 
	 * @return the selected or available library.
	 * 			identifier of the library which is written in the constants JNI_LIBRARY or AIRCOMPRESSOR 
	 * 			or null if there is no library available.
	 */
	public static String getSelectedOrAvailableLibrary() {
		List<ZstdLibrary> availableLibraries = getAvailableLibraries();
		if (availableLibraries.size() == 0) {
			return null;
		}
		String selectedZstdLibIdentifier = getSelectedZstdLib();
		for (ZstdLibrary library : availableLibraries) {
			if (selectedZstdLibIdentifier.equals(library.identifier)) {
				return selectedZstdLibIdentifier;
			}
		}
		
		return availableLibraries.get(0).identifier;
	}
}
