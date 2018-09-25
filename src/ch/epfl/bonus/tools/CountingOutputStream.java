package ch.epfl.bonus.tools;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;

public final class CountingOutputStream extends OutputStream {

	private int count;
	private final OutputStream innerStream;

	public CountingOutputStream(OutputStream innerStream) throws FileNotFoundException {
		this.innerStream = innerStream;
		count = 0;
	}

	public int count() {
		return count;
	}

	@Override
	public void write(int b) throws IOException {
		count++;
		innerStream.write(b);
	}

	@Override
	public void close() throws IOException {
		innerStream.close();
	}

}
