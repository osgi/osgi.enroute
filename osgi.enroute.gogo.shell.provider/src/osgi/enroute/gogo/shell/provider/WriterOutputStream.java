package osgi.enroute.gogo.shell.provider;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

/**
 * Wrap a Writer as an OutputStream.
 * @author Neil Bartlett
 */
class WriterOutputStream extends OutputStream {
	
	private final Writer writer;
	private final char[] cbuf = new char[1024];

	public WriterOutputStream(Writer writer) {
		this.writer = writer;
	}

	@Override
	public void write(int b) throws IOException {
		writer.write(b);
	}
	
	@Override
	public void write(byte[] bbuf) throws IOException {
		write(bbuf, 0, bbuf.length);
	}
	
	@Override
	public void write(byte[] bbuf, int off, int len) throws IOException {
		while (len > 0) {
			int bytesToWrite = Math.min(len, 1024);
			for (int i = 0; i < bytesToWrite; i++)
				cbuf[i] = (char) bbuf[i];
			writer.write(cbuf, 0, bytesToWrite);
			len -= bytesToWrite;
		}
	}

}
