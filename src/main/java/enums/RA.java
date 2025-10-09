/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * Based on work of Martin Biolek (https://github.com/mbio16/clientDNS)
 * */
package enums;

public enum RA {
	RECURSION_AVAIBLE((boolean) true), RECURSION_NON_AVAIBLE((boolean) false);

	public boolean code;

	private RA(boolean code) {
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
