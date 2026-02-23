/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * Based on work of Martin Biolek (https://github.com/mbio16/clientDNS)
 * */
package enums;

public enum QR {
	REQUEST(false), REPLY(true);

	public final boolean code;

	QR(boolean code) {
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
