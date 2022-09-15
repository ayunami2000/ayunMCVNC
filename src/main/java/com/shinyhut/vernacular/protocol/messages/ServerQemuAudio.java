package com.shinyhut.vernacular.protocol.messages;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ServerQemuAudio {

	private final byte[] audio;

	public ServerQemuAudio(byte[] audio) {
		this.audio = audio;
	}

	public byte[] getAudio() {
		return audio;
	}

	public static ServerQemuAudio decode(InputStream in) throws IOException {
		DataInputStream dataInput = new DataInputStream(in);
		dataInput.readFully(new byte[2]);
		int operation = readUint16BE(dataInput);
		if (operation == 2) {
			long length = readUint32BE(dataInput);
			byte[] out = new byte[(int) length];
			dataInput.readFully(out);
			return new ServerQemuAudio(out);
		}
		return new ServerQemuAudio(new byte[0]);
	}

	/** Parse 4 bytes from the byte array (starting at the offset) as unsigned 32-bit integer in big endian format. */
	public static long readUint32BE(DataInputStream bytes) throws IOException {
		return ((bytes.read() & 0xffL) << 24) |
				((bytes.read() & 0xffL) << 16) |
				((bytes.read() & 0xffL) << 8) |
				(bytes.read() & 0xffL);
	}

	/** Parse 2 bytes from the byte array (starting at the offset) as unsigned 16-bit integer in big endian format. */
	public static int readUint16BE(DataInputStream bytes) throws IOException {
		return ((bytes.read() & 0xff) << 8) |
				(bytes.read() & 0xff);
	}
}