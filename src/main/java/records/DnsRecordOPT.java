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
	private ArrayList<UInt16> optionDataLenght;
	private ArrayList<String> optionData;
	private boolean isNull;
	private static final String KEY_OPTION_CODE = "Option code";
	private static final String KEY_DATA_LENGHT = "Data lenght";
	private static final String KEY_DATA = "Data";
	private static final String KEY_RETURN = "Option data";

	public DnsRecordOPT(byte[] rawMessage, int length, int startIndex) {
		super(rawMessage, length, startIndex);
		optionCode = new ArrayList<UInt16>();
		optionDataLenght = new ArrayList<UInt16>();
		optionData = new ArrayList<String>();
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
				UInt16 lenghtOption = new UInt16().loadFromBytes(rawMessage[currentIndex],
						rawMessage[currentIndex + 1]);
				optionDataLenght.add(lenghtOption);
				currentIndex += 2;
				String data = "";
				for (int i = 0; i < lenghtOption.getValue(); i++) {
					data += String.format("%02x", rawMessage[currentIndex + i]);
				}
				currentIndex += lenghtOption.getValue();
				optionData.add(data);
				isNull = false;
			}
		} else {
			optionCode = null;
			optionData = null;
			optionDataLenght = null;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public JSONObject getAsJson() {

		JSONArray jsonArray = new JSONArray();
		if (optionCode.equals(null)) {
			return new JSONObject();
		}
		JSONObject jsonSupObject = new JSONObject();
		for (int i = 0; i < optionCode.size(); i++) {
			jsonSupObject.put(KEY_OPTION_CODE, optionCode.get(i).getValue());
			jsonSupObject.put(KEY_DATA_LENGHT, optionDataLenght.get(i).getValue());
			jsonSupObject.put(KEY_DATA, optionData.get(i));
			jsonArray.add(jsonSupObject);
		}
		JSONObject returnObject = new JSONObject();
		returnObject.put(KEY_RETURN, jsonArray);
		return returnObject;
	}

}
