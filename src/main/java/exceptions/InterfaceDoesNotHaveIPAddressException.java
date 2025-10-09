/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * Based on work of Martin Biolek (https://github.com/mbio16/clientDNS)
 * */
package exceptions;

public class InterfaceDoesNotHaveIPAddressException extends Exception {
	private static final long serialVersionUID = 1L;

	public InterfaceDoesNotHaveIPAddressException() {
		super("Network Interface does not have IP address");
	}
}
