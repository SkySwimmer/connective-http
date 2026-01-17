package org.asf.connective.io;

import java.io.IOException;
import java.io.InputStream;

public class LengthLimitedStream extends InputStream {

	private InputStream delegate;
	private long currentPos;
	private long length;
	private boolean allowClose;
	private boolean closed;

	public LengthLimitedStream(InputStream delegate, boolean allowClose, long length) {
		this.delegate = delegate;
		this.length = length;
		this.allowClose = allowClose;
	}

	@Override
	public int read() throws IOException {
		// Check
		if (closed)
			throw new IOException("Stream closed");
		if (currentPos >= length)
			return -1;

		// Read
		int b = delegate.read();
		currentPos++;
		if (b == -1)
			currentPos = length;
		return b;
	}

	@Override
	public int read(byte[] data) throws IOException {
		return read(data, 0, data.length);
	}

	@Override
	public int read(byte[] data, int start, int end) throws IOException {
		// Check position
		if (closed)
			throw new IOException("Stream closed");
		if (currentPos >= length)
			return -1;

		// Check
		if (end == 0)
			return 0;

		// Check start and length
		if (start > data.length || end > data.length - start || end < 0 || start < 0)
			throw new IndexOutOfBoundsException();

		// Get amount to read
		int bytesRead = 0;
		int bytesToRead = end - start;

		// Read block
		int amount = bytesToRead;
		if (amount > (length - currentPos))
			amount = (int) (length - currentPos);
		byte[] buffer = new byte[amount];
		int read = delegate.read(buffer, 0, amount);
		if (read == -1) {
			// End of stream
			return -1;
		}
		bytesRead += read;
		currentPos += read;

		// Write block to output
		for (int i = 0; i < buffer.length; i++)
			data[start + i] = buffer[i];

		// Return
		return bytesRead;
	}

	@Override
	public void close() throws IOException {
		if (allowClose)
			delegate.close();
		closed = true;
	}

}
