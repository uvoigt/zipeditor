package zipeditor.model.zstd;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.compress.compressors.zstandard.ZstdCompressorInputStream;
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorOutputStream;
import org.apache.commons.io.function.IOFunction;

import zipeditor.preferences.PreferenceUtils;

public class JniZstdLibCompressorHandler implements ZstdImplementationHandler {

	@Override
	public InputStream createInputStream(InputStream input) throws IOException {
		return new ZstdCompressorInputStream(input);
	}

	@Override
	public OutputStream createOutputStream(OutputStream output) throws IOException {
		return new ZstdCompressorOutputStream(noClose(output), PreferenceUtils.getCompressionLevel());
	}

	@Override
	public IOFunction<InputStream, InputStream> getIOFunction() {
		return ZstdCompressorInputStream::new;
	}
}
