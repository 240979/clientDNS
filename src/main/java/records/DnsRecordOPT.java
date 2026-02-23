/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * Based on work of Martin Biolek (https://github.com/mbio16/clientDNS)
 * */
package records;

import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import models.UInt16;

public class DnsRecordOPT extends DnsRecord {

	private ArrayList<UInt16> optionCode;
	private ArrayList<UInt16> optionDataLength;
	private ArrayList<String> optionData;
	private boolean isNull;
	private static final String KEY_OPTION_CODE = "Option code";
	private static final String KEY_DATA_LENGTH = "Data length";
	private static final String KEY_DATA = "Data";
	private static final String KEY_RETURN = "Option data";

	public DnsRecordOPT(byte[] rawMessage, int length, int startIndex) {
		super(rawMessage, length, startIndex);
		optionCode = new ArrayList<>();
		optionDataLength = new ArrayList<>();
		optionData = new ArrayList<>();
		isNull = true;
		parse();
	}

	public boolean getIsNull() {
		return isNull;
	}

	private void parse() {
		int currentIndex = startIndex;
		if (length > 4) {
			while (length + startIndex > currentIndex) {
				optionCode.add(new UInt16().loadFromBytes(rawMessage[currentIndex], rawMessage[currentIndex + 1]));
				currentIndex += 2;
				UInt16 lengthOption = new UInt16().loadFromBytes(rawMessage[currentIndex],
						rawMessage[currentIndex + 1]);
				optionDataLength.add(lengthOption);
				currentIndex += 2;
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < lengthOption.getValue(); i++) {
					sb.append(String.format("%02x", rawMessage[currentIndex + i]));
				}
				currentIndex += lengthOption.getValue();
				optionData.add(sb.toString());
				isNull = false;
			}
		} else {
			optionCode = null;
			optionData = null;
			optionDataLength = null;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public JSONObject getAsJson() {

		JSONArray jsonArray = new JSONArray();
		if (optionCode == null) {
			return new JSONObject();
		}
		JSONObject jsonSupObject = new JSONObject();
		for (int i = 0; i < optionCode.size(); i++) {
			jsonSupObject.put(KEY_OPTION_CODE, optionCode.get(i).getValue());
			jsonSupObject.put(KEY_DATA_LENGTH, optionDataLength.get(i).getValue());
			jsonSupObject.put(KEY_DATA, optionData.get(i));
			jsonArray.add(jsonSupObject);
		}
		JSONObject returnObject = new JSONObject();
		returnObject.put(KEY_RETURN, jsonArray);
		return returnObject;
	}

}
