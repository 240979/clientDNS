/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * Based on work of Martin Biolek (https://github.com/mbio16/clientDNS)
 * */
package enums;

public enum RA {
	RECURSION_AVAILABLE(true), RECURSION_NON_AVAILABLE(false);

	public final boolean code;

	RA(boolean code) {
		this.code = code;
	}

	public static RA getTypeByCode(boolean code) {
		for (RA e : RA.values()) {
			if (e.code == code)
				return e;
		}
		return null;
	}

}
