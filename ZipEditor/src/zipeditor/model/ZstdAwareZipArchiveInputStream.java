package zipeditor.model;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;

import zipeditor.model.zstd.ZstdUtilities;

public final class ZstdAwareZipArchiveInputStream extends ZipArchiveInputStream {
	public ZstdAwareZipArchiveInputStream(InputStream inputStream) {
		super(inputStream);
	}

	@Override
	protected InputStream createZstdInputStream(InputStream in) throws IOException {
		return ZstdUtilities.getInputStream(in);
	}
}