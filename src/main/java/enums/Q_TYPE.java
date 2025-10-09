/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * Based on work of Martin Biolek (https://github.com/mbio16/clientDNS)
 * */
package enums;

import models.UInt16;

public enum Q_TYPE {
	IN(1);

	public UInt16 code;

	private Q_TYPE(UInt16 code) {
		this.code = code;
	}

	private Q_TYPE(int code) {
		this.code = new UInt16(code);
	}

	public static Q_TYPE getTypeByCode(UInt16 code) {
		for (Q_TYPE e : Q_TYPE.values()) {
			if (e.code.equals(code))
				return e;
		}
		return null;
	}
}
