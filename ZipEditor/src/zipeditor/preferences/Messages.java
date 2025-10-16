package zipeditor.preferences;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = Messages.class.getPackageName() + ".messages"; //$NON-NLS-1$
	public static String PreferenceUtils_AircompressorLabel;
	public static String PreferenceUtils_ZSTDJniLibraryLabel;
	public static String ZipEditorPreferencePage_LibAvailable;
	public static String ZipEditorPreferencePage_LibNotAvailable;
	public static String ZipEditorPreferencePage_ZstdAsDefaultCompression;
	public static String ZipEditorPreferencePage_ZstdCompressionLevel;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}

	public static String ZipEditorPreferencePage_ActivateZstdSupport;
	public static String ZipEditorPreferencePage_HintNoLibraries;
	public static String ZipEditorPreferencePage_ScaleValueCompressionLevel;
	public static String ZipEditorPreferencePage_ZstdLibrary;
	public static String ZipEditorPreferencePage_ZstdSettingsGroupTitle;
}
