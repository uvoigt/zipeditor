package zipeditor.model.zstd;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.function.IOFunction;

import io.airlift.compress.zstd.ZstdInputStream;
import io.airlift.compress.zstd.ZstdOutputStream;

public class AircompressorHandler implements ZstdImplementationHandler {

	@Override
	public InputStream createInputStream(InputStream input) {
		return new ZstdInputStream(input);
	}

	@Override
	public OutputStream createOutputStream(OutputStream output) throws IOException {
		return new ZstdOutputStream(noClose(output));
	}

	@Override
	public IOFunction<InputStream, InputStream> getIOFunction() {
		return ZstdInputStream::new;
	}
}
