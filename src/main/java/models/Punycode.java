/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * Based on work of Martin Biolek (https://github.com/mbio16/clientDNS)
 * */
package models;

import java.net.IDN;

public class Punycode {

	public static String fromPunnycode(String domainName) {
		return IDN.toUnicode(domainName);
	}

	public static String toPunycode(String domainName) {
		return IDN.toASCII(domainName);
	}

}
