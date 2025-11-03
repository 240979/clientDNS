/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * Based on work of Martin Biolek (https://github.com/mbio16/clientDNS)
 * */
package records;

import org.json.simple.JSONObject;

import enums.DNSSEC_ALGORITHM_TYPE;
import enums.KEY_PROTOCOL;
import enums.KEY_TYPE;
import enums.KEY_USAGE;

public class DnsRecordDNSKEY extends DnsRecord {

	private KEY_TYPE keyType;
	private KEY_USAGE keyUsage;
	private KEY_PROTOCOL keyProtocol;
	private DNSSEC_ALGORITHM_TYPE dnssecAlgorithmType;
	private String key;

	private static final String JSON_KEY_TYPE = "Key type";
	private static final String JSON_KEY_USAGE = "Key usage";
	private static final String JSON_KEY_PROTOCOL = "Protocol";
	private static final String JSON_KEY_ALGORITHM_TYPE = "Algorithm";
	private static final String JSON_PUBLIC_KEY = "Public key";

	public DnsRecordDNSKEY(byte[] rawMessage, int length, int startIndex) {
		super(rawMessage, length, startIndex);
		key = "";
		parse();
	}

	private void parse() {
		keyType = KEY_TYPE.getTypeByCode(rawMessage[startIndex]);
		keyUsage = KEY_USAGE.getTypeByCode(rawMessage[startIndex + 1]);
		keyProtocol = KEY_PROTOCOL.getTypeByCode(rawMessage[startIndex + 2]);
		dnssecAlgorithmType = DNSSEC_ALGORITHM_TYPE.getTypeByCode(rawMessage[startIndex + 3]);
		int currentIndex = startIndex + 4;
		for (int i = currentIndex; i < length; i++) {
			key += String.format("%02x", rawMessage[i]);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public JSONObject getAsJson() {
		JSONObject object = new JSONObject();
		object.put(JSON_KEY_TYPE, keyType);
		object.put(JSON_KEY_USAGE, keyUsage);
		object.put(JSON_KEY_PROTOCOL, keyProtocol);
		object.put(JSON_KEY_ALGORITHM_TYPE, dnssecAlgorithmType);
		object.put(JSON_PUBLIC_KEY, key);
		return object;
	}

	@Override
	public String[] getValesForTreeItem() {
        return new String[]{ JSON_KEY_TYPE + ": " + keyType, JSON_KEY_USAGE + ": " + keyUsage,
                JSON_KEY_PROTOCOL + ": " + keyProtocol, JSON_KEY_ALGORITHM_TYPE + ": " + dnssecAlgorithmType,
                JSON_PUBLIC_KEY + ": " + key };
	}

	@Override
	public String getDataForTreeViewName() {
		return keyType + " " + keyUsage + " " + keyProtocol + "  ...";
	}
}
