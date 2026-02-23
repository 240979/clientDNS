/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * Based on work of Martin Biolek (https://github.com/mbio16/clientDNS)
 * */
package exceptions;

import java.io.Serial;

public class QueryIdNotMatchException extends Exception {

	@Serial
	private static final long serialVersionUID = 1L;

	public QueryIdNotMatchException() {
		super("Query does not match exception");
	}
}
