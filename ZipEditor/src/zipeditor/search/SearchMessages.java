package zipeditor.search;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class SearchMessages {
	private static final String BUNDLE_NAME = "zipeditor.search.SearchMessages"; //$NON-NLS-1$

	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

	private SearchMessages() {
	}

	public static String getString(String key) {
		try {
			return RESOURCE_BUNDLE.getString(key);
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}
}
