package com.chen;

public class HexUtil {
	private static final char[] UPPER_DIGITS = new char[] { '0', '1', '2', '3', '4', //
			'5', '6', '7', '8', '9', //
			'A', 'B', 'C', 'D', 'E', //
			'F' };
	private static final char[] LOWER_DIGITS = new char[] { '0', '1', '2', '3', '4', //
			'5', '6', '7', '8', '9', //
			'a', 'b', 'c', 'd', 'e', //
			'f' };

	public static final byte[] emptybytes = new byte[0];

	/**
	 * @return String Hex String
	 */
	public static String toLowerHex(byte b) {
		return toHex(b, LOWER_DIGITS);
	}

	public static String toUpperHex(byte b) {
		return toHex(b, UPPER_DIGITS);
	}

	public static String toHex(byte b, final char[] digits) {
		char[] buf = new char[2];
		buf[1] = digits[b & 0xF];
		b = (byte) (b >>> 4);
		buf[0] = digits[b & 0xF];
		return new String(buf);
	}

	public static String toLowerHex(byte[] bytes) {
		return toHex(bytes, LOWER_DIGITS);
	}

	private static String toHex(byte[] bytes, char[] digits) {
		if (bytes == null || bytes.length == 0) {
			return null;
		}

		char[] buf = new char[bytes.length << 1];
		try {
			for (int i = 0; i < bytes.length; i++) {
				byte b = bytes[i];
				buf[(i << 1) + 1] = digits[b & 0xF];
				b = (byte) (b >>> 4);
				buf[(i << 1) + 0] = digits[b & 0xF];
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		return new String(buf);
	}

	public static String toUpperHex(byte[] bytes) {
		return toHex(bytes, UPPER_DIGITS);
	}

	/**
	 * @return byte
	 */
	public static byte toByte(String str) {
		if (str != null && str.length() == 1) {
			return char2Byte(str.charAt(0));
		} else {
			return 0;
		}
	}

	/**
	 * @return byte
	 */
	public static byte char2Byte(char ch) {
		if (ch >= '0' && ch <= '9') {
			return (byte) (ch - '0');
		} else if (ch >= 'a' && ch <= 'f') {
			return (byte) (ch - 'a' + 10);
		} else if (ch >= 'A' && ch <= 'F') {
			return (byte) (ch - 'A' + 10);
		} else {
			return 0;
		}
	}

	public static byte[] toBytes(String str) {
		if (str == null || str.equals("")) {
			return emptybytes;
		}

		byte[] bytes = new byte[str.length() / 2];
		try {
			for (int i = 0; i < bytes.length; i++) {
				char high = str.charAt(i * 2);
				char low = str.charAt(i * 2 + 1);
				bytes[i] = (byte) (char2Byte(high) * 16 + char2Byte(low));
			}
		} catch (Exception e) {
			System.out.println(" === hexStr2Bytes error === " + e.toString());
			return emptybytes;
		}

		return bytes;
	}
}
