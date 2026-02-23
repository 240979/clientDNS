/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * Based on work of Martin Biolek (https://github.com/mbio16/clientDNS)
 * */
package exceptions;

import lombok.Getter;

import java.io.Serial;

@Getter
public class HttpCodeException extends Exception {
	@Serial
	private static final long serialVersionUID = 1L;
	private final int code;

	public HttpCodeException(int code) {
		super("" + code);
		this.code = code;
	}

}
