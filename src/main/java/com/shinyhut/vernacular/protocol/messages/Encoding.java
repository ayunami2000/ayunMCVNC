//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.shinyhut.vernacular.protocol.messages;

import com.shinyhut.vernacular.client.exceptions.UnsupportedEncodingException;
import java.util.Arrays;

public enum Encoding {
	RAW(0),
	COPYRECT(1),
	RRE(2),
	HEXTILE(5),
	ZLIB(6),
	DESKTOP_SIZE(-223),
	CURSOR(-239),
	QEMU_AUDIO(-259);

	private int code;

	private Encoding(int code) {
		this.code = code;
	}

	public int getCode() {
		return this.code;
	}

	public static Encoding resolve(int code) throws UnsupportedEncodingException {
		return (Encoding)Arrays.stream(values()).filter((e) -> {
			return e.code == code;
		}).findFirst().orElseThrow(() -> {
			return new UnsupportedEncodingException(code);
		});
	}
}
