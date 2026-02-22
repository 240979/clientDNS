/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * Based on work of Martin Biolek (https://github.com/mbio16/clientDNS)
 * */
package models;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Pattern;


public class DomainConvert {
	private static final Pattern pDomainNameOnly;
	private static final String DOMAIN_NAME_PATTERN = "^((?!-)[_A-Za-z0-9][A-Za-z0-9-]{0,62}(?<!-)\\.)+[A-Za-z]{2,63}\\.?$";
	private static final int COMPRESS_CONSTANT_NUMBER = 49152;
	private static final byte[] ROOT = { (byte) 0x00 };
	static {
		pDomainNameOnly = Pattern.compile(DOMAIN_NAME_PATTERN);
	}

	public static byte[] encodeDNS(String domain) {
		if (domain == null || domain.isEmpty()) {
			return ROOT;
		}
		// String original = domain;
		domain = encodeIDN(domain);
		ArrayList<Byte> resultByte = new ArrayList<>();
		String[] split = domain.split("\\.");
		for (String string : split) {
			resultByte.add((byte) string.length());
			byte[] toAdd = string.getBytes(StandardCharsets.US_ASCII);
            for (byte b : toAdd) {
                resultByte.add(b);
            }
		}
		resultByte.add((byte) 0x00);
		byte[] arrayToReturn = new byte[resultByte.size()];

		for (int i = 0; i < arrayToReturn.length; i++) {
			arrayToReturn[i] = resultByte.get(i);
		}
		return arrayToReturn;
	}

	public static byte[] encodeMDNS(String domain){
		// domain.getBytes(StandardCharsets.UTF_8);

		ArrayList<Byte> resultByte = new ArrayList<>();
		String[] split = domain.split("\\.");
		for (String string : split) {
			byte[] toAdd = string.getBytes(StandardCharsets.UTF_8);
			resultByte.add((byte) toAdd.length);
            for (byte b : toAdd) {
                resultByte.add(b);
            }
		}
		resultByte.add((byte) 0x00);
		byte[] arrayToReturn = new byte[resultByte.size()];

		for (int i = 0; i < arrayToReturn.length; i++) {
			arrayToReturn[i] = resultByte.get(i);
		}
		return arrayToReturn;
	}

	public static String decodeMDNS(byte[] encodedDomain, int startIndex) {
		int passed = startIndex;
		StringBuilder result = new StringBuilder();
		while (true) {
			int size = encodedDomain[passed];
			if (size == 0) {
				if (result.isEmpty())
					return result.toString();
				return result.substring(0, result.length() - 1);
			} else {
				if (size != 1) {
					if (isDnsNameCompressed(encodedDomain, passed)) {
						return result + getCompressedNameMDNS(encodedDomain, passed);
					}
				}

				byte[] pom = Arrays.copyOfRange(encodedDomain, passed + 1, passed + size + 1);
				result.append(new String(pom, StandardCharsets.UTF_8));
				passed += size + 1;
				result.append(".");
			}
		}
	}

	public static String encodeIDN(String domain) {
		if (!StandardCharsets.US_ASCII.newEncoder().canEncode(domain)) {
			return Punycode.toPunycode(domain);
		} else {
			return domain;
		}
	}

	public static String decodeDNS(byte[] encodedDomain) {
		int passed = 0;
		StringBuilder result = new StringBuilder();
		while (true) {
			int size = encodedDomain[passed];
			if (size == 0) {
				return result.substring(0, result.length() - 1);
			} else {
				for (int i = passed + 1; i < passed + size + 1; i++) {
					result.append((char) encodedDomain[i]);
				}
				passed += size + 1;
				result.append(".");
			}
		}
	}

	public static String decodeDNS(byte[] encodedDomain, int startIndex) {
		int passed = startIndex;
		StringBuilder result = new StringBuilder();
		while (true) {
			int size = encodedDomain[passed];
			if (size == 0) {
				if (result.isEmpty())
					return result.toString();
				return result.substring(0, result.length() - 1);
			} else {
				if (size != 1) {
					if (isDnsNameCompressed(encodedDomain, passed)) {
						return result + getCompressedName(encodedDomain, passed);
					}
				}

				for (int i = passed + 1; i < passed + size + 1; i++) {
					result.append((char) encodedDomain[i]);
				}
				passed += size + 1;
				result.append(".");
			}
		}
	}

	private static boolean isDnsNameCompressed(byte[] rawMessage, int currentPosition) {
		UInt16 firstTwoBytes = new UInt16().loadFromBytes(rawMessage[currentPosition], rawMessage[currentPosition + 1]);
        return firstTwoBytes.getValue() >= COMPRESS_CONSTANT_NUMBER;

	}

	private static String getCompressedName(byte[] rawMessage, int currentPosition) {
		UInt16 firstTwoBytes = new UInt16().loadFromBytes(rawMessage[currentPosition], rawMessage[currentPosition + 1]);
		UInt16 nameStartByte = new UInt16(firstTwoBytes.getValue() - COMPRESS_CONSTANT_NUMBER);
		return DomainConvert.decodeDNS(rawMessage, nameStartByte.getValue());
	}

	private static String getCompressedNameMDNS(byte[] rawMessage, int currentPosition) {
		UInt16 firstTwoBytes = new UInt16().loadFromBytes(rawMessage[currentPosition], rawMessage[currentPosition + 1]);
		UInt16 nameStartByte = new UInt16(firstTwoBytes.getValue() - COMPRESS_CONSTANT_NUMBER);
		return DomainConvert.decodeMDNS(rawMessage, nameStartByte.getValue());
	}

	public static int getIndexOfLastByteOfName(byte[] wholeAnswerSection, int start) {
		int position = start;
		while (true) {
			if ((int) wholeAnswerSection[position] == 0) {
				return position;
			} else {
				if ((int) wholeAnswerSection[position] != 1) {
					if (isDnsNameCompressed(wholeAnswerSection, position)) {
						return position + 1;
					}

                }
                position += (int) wholeAnswerSection[position] + 1;

            }
		}
	}

	private static boolean isUTF8Domain(String domain) {
		try {
			// domain.getBytes(StandardCharsets.UTF_8);
            String encoded = Punycode.toPunycode(domain);

			return !encoded.equals(domain);
		} catch (Exception e) {
			return false;
		}
	}

	public static boolean isValidDomainName(String domainName) {

		if (domainName.contains(":")){
			return false;
		}
		if (domainName.endsWith(".")) {
			domainName = domainName.substring(0, domainName.length() - 1);
		}
		if (domainName.split("\\.").length == 1 && domainName.length() >= 2) {
			return true;
		}
		boolean asciiName = pDomainNameOnly.matcher(domainName).find();
		if (asciiName) {
			return true;
		} else {
			return isUTF8Domain(domainName);
		}
	}
}
