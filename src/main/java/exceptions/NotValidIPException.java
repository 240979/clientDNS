/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * Based on work of Martin Biolek (https://github.com/mbio16/clientDNS)
 * */
package exceptions;

public class NotValidIPException extends Exception {

	private static final long serialVersionUID = 1L;

	public NotValidIPException() {
		super("Ip is not valid");
	}
}
