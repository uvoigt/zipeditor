package zipeditor.model.zstd;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.function.IOFunction;

public interface ZstdImplementationHandler {

	default OutputStream noClose(OutputStream out) {
		return new OutputStream() {
			@Override
			public void write(byte[] b) throws IOException {
				out.write(b);
			}
			
			@Override
			public void write(int b) throws IOException {
				out.write(b);
			}
			
			@Override
			public void write(byte[] b, int off, int len) throws IOException {
				out.write(b, off, len);
			}
		};
	}

	InputStream createInputStream(InputStream input) throws IOException;

	OutputStream createOutputStream(OutputStream output) throws IOException;

	IOFunction<InputStream, InputStream> getIOFunction();

}
