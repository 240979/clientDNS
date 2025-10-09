/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * Based on work of Martin Biolek (https://github.com/mbio16/clientDNS)
 * */
package enums;

public enum QR {
	REQUEST((boolean) false), REPLY((boolean) true);

	public boolean code;

	private QR(boolean code) {
		this.code = code;
	}

	public static QR getTypeByCode(boolean code) {
		for (QR e : QR.values()) {
			if (e.code == code)
				return e;
		}
		return null;
	}
}
