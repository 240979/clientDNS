/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * Based on work of Martin Biolek (https://github.com/mbio16/clientDNS)
 * */
package records;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import org.json.simple.JSONObject;

public class DnsRecordTXT extends DnsRecord {

	private static final String KEY_TEXT = "Text";
	private String stringText;

	public DnsRecordTXT(byte[] rawMessage, int length, int startIndex) throws UnsupportedEncodingException {
		super(rawMessage, length, startIndex);
		parse();
	}

	public void parse() throws UnsupportedEncodingException {
		byte[] textByte = new byte[length - 1];
		int j = 0;
		for (int i = startIndex + 1; i < startIndex + length; i++) {
			textByte[j] = rawMessage[i];
			j++;
		}
		// Has to be repaired
		stringText = new String(textByte, StandardCharsets.UTF_8);

	}

	@Override
	public String getDataAsString() {
		return stringText;
	}

	@SuppressWarnings("unchecked")
	@Override
	public JSONObject getAsJson() {
		JSONObject object = new JSONObject();
		object.put(KEY_TEXT, stringText);
		return object;
	}

	@Override
	public String[] getValuesForTreeItem() {
        return new String[]{ KEY_TEXT + ": " + stringText };
	}

	@Override
	public String getDataForTreeViewName() {
		return stringText;
	}
}
