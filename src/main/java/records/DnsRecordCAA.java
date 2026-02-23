/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * Based on work of Martin Biolek (https://github.com/mbio16/clientDNS)
 * */
package records;

import org.json.simple.JSONObject;
import enums.CERTIFICATE_FLAG;

public class DnsRecordCAA extends DnsRecord {
	private CERTIFICATE_FLAG flag;
    private String tag;
	private String value;
	private static final String KEY_CERTIFICATE_FLAG = "Flag";
	private static final String KEY_TAG = "Tag";
	private static final String KEY_VALUE = "Value";

	public DnsRecordCAA(byte[] rawMessage, int length, int startIndex) {
		super(rawMessage, length, startIndex);
		tag = "";
		value = "";
		parse();
	}

	private void parse() {
		StringBuilder sb = new StringBuilder();
		flag = CERTIFICATE_FLAG.getTypeByCode(rawMessage[startIndex]);
        int tagLength = rawMessage[startIndex + 1];
		int currentIndex = startIndex + 2;
		for (int i = currentIndex; i < currentIndex + tagLength; i++) {
			sb.append(rawMessage[i]);
		}
		tag = sb.toString();
		sb.setLength(0); // This should clear SB
		currentIndex += tagLength;

		for (int i = currentIndex; i < startIndex + length; i++) {
			sb.append(rawMessage[i]);
		}
		value = sb.toString();
	}

	@SuppressWarnings("unchecked")
	@Override
	public JSONObject getAsJson() {
		JSONObject json = new JSONObject();
		json.put(KEY_CERTIFICATE_FLAG, flag);
		json.put(KEY_TAG, tag);
		json.put(KEY_VALUE, value);
		return json;
	}

	@Override
	public String toString() {
		return KEY_CERTIFICATE_FLAG + ": " + flag + "\n " + KEY_TAG + ": " + ": " + tag + "\n" + KEY_VALUE + ": " + ": "
				+ value;
	}

	@Override
	public String[] getValuesForTreeItem() {
        return new String[]{ KEY_CERTIFICATE_FLAG + ": " + flag, KEY_TAG + ": " + tag, KEY_VALUE + ": " + value };
	}

	@Override
	public String getDataForTreeViewName() {
		return flag + " " + tag + " " + value;
	}
}
