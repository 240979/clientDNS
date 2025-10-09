/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * Based on work of Martin Biolek (https://github.com/mbio16/clientDNS)
 * */
package exceptions;

public class MessageTooBigForUDPException extends Exception {

	private static final long serialVersionUID = 1L;

	public MessageTooBigForUDPException() {
		super("Message has more than 512 bytes, too big for UDP message.");
	}
}
