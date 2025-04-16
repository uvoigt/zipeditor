package zipeditor.model.zstd;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.archivers.zip.ZipFile.Builder;
import org.apache.commons.compress.compressors.zstandard.ZstdUtils;
import zipeditor.model.ZipEditorZstdException;
import zipeditor.preferences.PreferenceUtils;

public class ZstdUtilities {

	public static InputStream getInputStream(InputStream in) throws IOException {
		if (!PreferenceUtils.isZstdActive()) {
			throw new ZipEditorZstdException(Messages.ZstdHandler_notActive);
		} else {
			if (PreferenceUtils.isJNIZstdSelected()) {
				if (!ZstdUtils.isZstdCompressionAvailable()) {
					throw new ZipEditorZstdException(Messages.ZstdHandler_jniLibNotAvailable);
				}
				return new JniZstdLibCompressorHandler().createInputStream(in);
			} else {
				if (!isAircompressorAvailable()) {
					throw new ZipEditorZstdException(Messages.ZstdHandler_aircompLibNotAvailable);
				}
				return new AircompressorHandler().createInputStream(in);
			}
		}
	}

	public static OutputStream getOutputStream(OutputStream out) throws IOException {
		if (!PreferenceUtils.isZstdActive()) {
			throw new ZipEditorZstdException(Messages.ZstdHandler_notActive);
		} else {
			if (PreferenceUtils.isJNIZstdSelected()) {
				if (!ZstdUtils.isZstdCompressionAvailable()) {
					throw new ZipEditorZstdException(Messages.ZstdHandler_jniLibNotAvailable);
				}
				return new JniZstdLibCompressorHandler().createOutputStream(out);
			} else {
				if (!isAircompressorAvailable()) {
					throw new ZipEditorZstdException(Messages.ZstdHandler_aircompLibNotAvailable);
				}
				return new AircompressorHandler().createOutputStream(out);
			}
		}
	}

	private static boolean hasAircompressor;
	private static boolean checked;
	public static boolean isAircompressorAvailable() {
		if (checked) 
			return hasAircompressor;

		checked = true;
		try {
			Class.forName("io.airlift.compress.zstd.ZstdInputStream"); //$NON-NLS-1$
			hasAircompressor = true;
		} catch (ClassNotFoundException e) {
			hasAircompressor = false;
		}
		return hasAircompressor;
	}

	public static Builder getZipFileBuilder() throws ZipEditorZstdException {
		if (!PreferenceUtils.isZstdActive()) {
			throw new ZipEditorZstdException(Messages.ZstdHandler_notActive);
		}
		if (PreferenceUtils.isJNIZstdSelected()) {
			if (!ZstdUtils.isZstdCompressionAvailable()) {
				throw new ZipEditorZstdException(Messages.ZstdHandler_jniLibNotAvailable);
			}
			return ZipFile.builder(); 
		} else {
			if (!isAircompressorAvailable()) {
				throw new ZipEditorZstdException(Messages.ZstdHandler_aircompLibNotAvailable);
			}
			return ZipFile.builder().setZstdInputStreamFactory(new AircompressorHandler().getIOFunction());
		}
	}

	public static boolean isZstdJniCompressionAvailable() {
		return ZstdUtils.isZstdCompressionAvailable();
	}
}
