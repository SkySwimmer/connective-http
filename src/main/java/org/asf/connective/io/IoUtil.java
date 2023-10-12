package org.asf.connective.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class IoUtil {

	public static byte[] readNBytes(InputStream input, int num) throws IOException {
		byte[] res = new byte[num];
		int c = 0;
		while (true) {
			try {
				int r = input.read(res, c, num);
				if (r == -1)
					break;
				c += r;
			} catch (Exception e) {
				int b = input.read();
				if (b == -1)
					break;
				res[c++] = (byte) b;
			}
			if (c >= num)
				break;
		}
		return Arrays.copyOfRange(res, 0, c);
	}

}
