package zipeditor.model;

import java.io.IOException;

/**
 * This is just a marker class to differ all other {@link IOException}s from the
 * Zstd problems. If this exception is thrown, than either the zstd support is
 * not activated or the selected zstd compression library is not available. If
 * this exception is thrown the zip editor should warn, that this zip file
 * contains zstd compression which can't be opened.
 */
public class ZipEditorZstdException extends IOException {

	public ZipEditorZstdException(String message) {
		super(message);
	}

	private static final long serialVersionUID = 8897411573370620194L;

}
