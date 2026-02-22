/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * Added Lombok annotation and removed manual written getters
 * Based on work of Martin Biolek (https://github.com/mbio16/clientDNS)
 * */
package models;

import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import lombok.Data;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import enums.APPLICATION_PROTOCOL;
import enums.CACHE;
import enums.Q_COUNT;
import enums.Q_TYPE;
import javafx.scene.control.TreeItem;
import records.*;

@Data
public class Response {

	private byte[] rawMessage;
	private String nameAsString;
	private Q_COUNT qcount;
	private Q_TYPE qtype;
	private int ttl;
	private UInt16 rdLength;
	private int byteSize;
	private int endIndex;
	private DnsRecord rdata;
	private byte rCode;
	private byte version;
	private UInt16 doBit;
	private UInt16 size;
	private String srvService;
	private String srvProtocol;
	private String srvName;
	private CACHE cache;
	private JSONArray dohData = null;
	private static final int COMPRESS_CONSTANT_NUMBER = 49152;
	private static final int DO_BIT_VALUE = 32768;
	private static final int MAX_UDP_SIZE = 1232;
	private static final String DATA_KEY = "Data";
	private static final String NAME_KEY = "Name";
	private static final String TYPE_KEY = "Type";
	private static final String TTL_KEY = "Time to Live";
	private static final String CLASS_KEY = "Class";
	private static final String CACHE_KEY = "Cache";
	private static final String KEY_OPT_UDP_SIZE = "Size";
	private static final String KEY_OPT_RCODE = "Rcode";
	private static final String KEY_OPT_VERSION = "EDSN0 version";
	private static final String KEY_FLUSH_CACHE = "Flush cache";
	private static final String KEY_OPT_DO_BIT = "Can handle DNSSEC";
	private static final String KEY_SRV_SERVICE = "Service";
	private static final String KEY_SRV_PROTOCOL = "Protocol";
	private static final String KEY_SRV_NAME = "Name";
	private static final String KEY_OPT_OPTIONS = "Options";
	public static final String ROOT_DOMAIN = ". (ROOT)";



	public Response() {

	}

	public Response(JSONArray dohData) {
		this.dohData = dohData;
	}

	public Response parseResponse(byte[] rawMessage, int startIndex)
			throws UnknownHostException, UnsupportedEncodingException {
		this.rawMessage = rawMessage;
		int currentIndex = startIndex;
		currentIndex = parseName(currentIndex, false);
		this.qcount = Q_COUNT
				.getTypeByCode(new UInt16().loadFromBytes(rawMessage[currentIndex], rawMessage[currentIndex + 1]));
		currentIndex += 2;
        assert qcount != null;
        if (qcount.equals(Q_COUNT.OPT)) {
			parseAsOPT(currentIndex);
			return this;
		}
		this.qtype = Q_TYPE
				.getTypeByCode(new UInt16().loadFromBytes(rawMessage[currentIndex], rawMessage[currentIndex + 1]));
		currentIndex += 2;
		byte[] ttlBytes = { rawMessage[currentIndex], rawMessage[currentIndex + 1], rawMessage[currentIndex + 2],
				rawMessage[currentIndex + 3] };
		this.ttl = ByteBuffer.wrap(ttlBytes).getInt();
		currentIndex += 4;
		this.rdLength = new UInt16().loadFromBytes(rawMessage[currentIndex], rawMessage[currentIndex + 1]);
		currentIndex += 2;
		this.endIndex = currentIndex + this.rdLength.getValue() - 1;
		this.rdata = parseRecord(currentIndex);
		cache = null;
		return this;
	}

	public Response parseResponseMDNS(byte[] rawMessage, int startIndex)
			throws UnknownHostException, UnsupportedEncodingException {
		this.rawMessage = rawMessage;
		int currentIndex = startIndex;
		currentIndex = parseName(currentIndex, true);
		this.qcount = Q_COUNT
				.getTypeByCode(new UInt16().loadFromBytes(rawMessage[currentIndex], rawMessage[currentIndex + 1]));
		currentIndex += 2;
        assert qcount != null;
        if (qcount.equals(Q_COUNT.OPT)) {
			parseAsOPT(currentIndex);
			return this;
		}
		checkAndParseSRV();
		UInt16 pom = new UInt16().loadFromBytes(rawMessage[currentIndex], rawMessage[currentIndex + 1]);
		this.cache = CACHE.getTypeByCode(pom);
		if (cache == CACHE.FLUSH_CACHE) {
			pom = new UInt16(pom.getValue() - CACHE.FLUSH_CACHE.value);
		}
		this.qtype = Q_TYPE.getTypeByCode(pom);
		currentIndex += 2;
		byte[] ttlBytes = { rawMessage[currentIndex], rawMessage[currentIndex + 1], rawMessage[currentIndex + 2],
				rawMessage[currentIndex + 3] };
		this.ttl = ByteBuffer.wrap(ttlBytes).getInt();
		currentIndex += 4;
		this.rdLength = new UInt16().loadFromBytes(rawMessage[currentIndex], rawMessage[currentIndex + 1]);
		currentIndex += 2;
		this.endIndex = currentIndex + this.rdLength.getValue() - 1;
		this.rdata = parseRecord(currentIndex);
		return this;
	}

	private void checkAndParseSRV() {
		if (this.qcount == Q_COUNT.SRV) {
			srvName = "";
			StringBuilder sb = new StringBuilder();
			String[] srvArray = this.nameAsString.split("\\.");
			srvService = srvArray[0];
			srvProtocol = srvArray[1];
			for (int i = 2; i < srvArray.length; i++) {
				if (i + 1 == srvArray.length)
					//srvName += srvArray[i];
					sb.append(srvArray[i]);
				else
					//srvName += srvArray[i] + ".";
					sb.append(srvArray[i])
						.append(".");
			}
			srvName = sb.toString();
		}
	}

	private void parseAsOPT(int currentIndex) throws UnknownHostException, UnsupportedEncodingException {
		size = new UInt16().loadFromBytes(rawMessage[currentIndex], rawMessage[currentIndex + 1]);
		currentIndex += 2;
		rCode = rawMessage[currentIndex];
		currentIndex += 1;
		version = rawMessage[currentIndex];
		currentIndex += 1;
		doBit = new UInt16().loadFromBytes(rawMessage[currentIndex], rawMessage[currentIndex + 1]);
		currentIndex += 2;
		rdLength = new UInt16().loadFromBytes(rawMessage[currentIndex], rawMessage[currentIndex + 1]);
		currentIndex += 2;
		this.rdata = parseRecord(currentIndex);
		nameAsString = ". (ROOT)";
		this.endIndex = currentIndex - 1;
	}

	private int parseName(int startIndex, boolean mdns) {
		int positionOfNameIndex = startIndex;
		UInt16 firstTwoBytes = new UInt16().loadFromBytes(rawMessage[startIndex], rawMessage[startIndex + 1]);
		if (firstTwoBytes.getValue() >= COMPRESS_CONSTANT_NUMBER) {
			// compress Form
			UInt16 nameStartByte = new UInt16(firstTwoBytes.getValue() - COMPRESS_CONSTANT_NUMBER);
			positionOfNameIndex = nameStartByte.getValue();
			startIndex += 2;
		} else {
			startIndex = DomainConvert.getIndexOfLastByteOfName(rawMessage, startIndex) + 1;
		}
		if (mdns) {
			this.nameAsString = DomainConvert.decodeMDNS(rawMessage, positionOfNameIndex);
		} else {
			this.nameAsString = DomainConvert.decodeDNS(rawMessage, positionOfNameIndex);
		}
		return startIndex;
	}

	/*
	* Added records CDS and CDNSKEY
	* */
	private DnsRecord parseRecord(int currentIndex) throws UnknownHostException, UnsupportedEncodingException {
        return switch (qcount) {
            case A -> new DnsRecordA(rawMessage, rdLength.getValue(), currentIndex);
            case AAAA -> new DnsRecordAAAA(rawMessage, rdLength.getValue(), currentIndex);
            case CNAME -> new DnsRecordCNAME(rawMessage, rdLength.getValue(), currentIndex);
            case NS -> new DnsRecordNS(rawMessage, rdLength.getValue(), currentIndex);
            case TXT -> new DnsRecordTXT(rawMessage, rdLength.getValue(), currentIndex);
            case MX -> new DnsRecordMX(rawMessage, rdLength.getValue(), currentIndex);
            case SOA -> new DnsRecordSOA(rawMessage, rdLength.getValue(), currentIndex);
            case DNSKEY -> new DnsRecordDNSKEY(rawMessage, rdLength.getValue(), currentIndex);
            case CAA -> new DnsRecordCAA(rawMessage, rdLength.getValue(), currentIndex);
            case RRSIG -> new DnsRecordRRSIG(rawMessage, rdLength.getValue(), currentIndex);
            case OPT -> new DnsRecordOPT(rawMessage, rdLength.getValue(), currentIndex);
            case PTR -> new DnsRecordPTR(rawMessage, rdLength.getValue(), currentIndex);
            case DS -> new DnsRecordDS(rawMessage, rdLength.getValue(), currentIndex);
            case NSEC -> new DnsRecordNSEC(rawMessage, rdLength.getValue(), currentIndex);
            case NSEC3 -> new DnsRecordNSEC3(rawMessage, rdLength.getValue(), currentIndex);
            case NSEC3PARAM -> new DnsRecordNSEC3PARAM(rawMessage, rdLength.getValue(), currentIndex);
            case SRV -> new DnsRecordSRV(rawMessage, rdLength.getValue(), currentIndex);
            case CDS -> new DnsRecordCDS(rawMessage, rdLength.getValue(), currentIndex);
            case CDNSKEY -> new DnsRecordCDNSKEY(rawMessage, rdLength.getValue(), currentIndex);
            case SVCB -> new DnsRecordSVCB(rawMessage, rdLength.getValue(), currentIndex);
            case HTTPS -> new DnsRecordHTTPS(rawMessage, rdLength.getValue(), currentIndex);
            default -> null;
        };
	}

	public TreeItem<String> getAsTreeItem() {
		TreeItem<String> main = new TreeItem<>(
				nameAsString + " " + qcount + " " + rdata.getDataForTreeViewName());
		// check if type is SRV
		if (qcount == Q_COUNT.SRV) {
			main.getChildren().add(new TreeItem<>(KEY_SRV_SERVICE + ": " + srvService));
			main.getChildren().add(new TreeItem<>(KEY_SRV_PROTOCOL + ": " + srvProtocol));
			main.getChildren().add(new TreeItem<>(KEY_SRV_NAME + ": " + srvName));
		} else {
			main.getChildren().add(new TreeItem<>(NAME_KEY + ": " + nameAsString));
		}

		// check if it is mdns
		if (cache != null) {
			main.getChildren().add(new TreeItem<>(CACHE_KEY + ": " + cache));
		}

		main.getChildren().add(new TreeItem<>(TYPE_KEY + ": " + qcount));

		if (qcount.code != Q_COUNT.OPT.code) {
			main.getChildren().add(new TreeItem<>(TTL_KEY + ": " + ttl));
			main.getChildren().add(new TreeItem<>(CLASS_KEY + ": " + qtype));
			for (String item : rdata.getValesForTreeItem()) {
				main.getChildren().add(new TreeItem<>(item));
			}
		} else {
			main.setValue(nameAsString + " " + qcount);

			main.getChildren().add(new TreeItem<>(KEY_OPT_RCODE + ": " + (int) rCode));
			main.getChildren().add(new TreeItem<>(KEY_OPT_VERSION + ": " + (int) version));
			main.getChildren().add(new TreeItem<>(KEY_OPT_UDP_SIZE + ": " + size.getValue()));
			String doBitString = doBit.getValue() >= DO_BIT_VALUE ? "true" : "false";
			main.getChildren().add(new TreeItem<>(KEY_OPT_DO_BIT + ": " + doBitString));
		}

		return main;
	}

	public static TreeItem<String> getOptAsTreeItem(boolean dnssec, boolean mdns) {
		TreeItem<String> root = new TreeItem<>(ROOT_DOMAIN + " " + Q_COUNT.OPT);
		root.getChildren().add(new TreeItem<>(NAME_KEY + ": " + ROOT_DOMAIN));
		root.getChildren().add(new TreeItem<>(TYPE_KEY + ": " + Q_COUNT.OPT));
		root.getChildren().add(new TreeItem<>(KEY_OPT_RCODE + ": " + 0));
		root.getChildren().add(new TreeItem<>(KEY_OPT_VERSION + ": " + 0));
		if (mdns) {
			root.getChildren().add(new TreeItem<>(KEY_FLUSH_CACHE + ": " + CACHE.NO_FLUSH_CACHE.code));
		}
		root.getChildren().add(new TreeItem<>(KEY_OPT_UDP_SIZE + ": " + MAX_UDP_SIZE));
		root.getChildren().add(new TreeItem<>(KEY_OPT_DO_BIT + ": " + dnssec));
		return root;
	}

	@SuppressWarnings("unchecked")
	public static JSONObject getOptRequestAsJson(boolean dnssec, boolean mdns) {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put(ROOT_DOMAIN, Q_COUNT.OPT);
		jsonObject.put(NAME_KEY, ROOT_DOMAIN);
		jsonObject.put(TYPE_KEY, Q_COUNT.OPT);
		jsonObject.put(KEY_OPT_RCODE, 0);
		jsonObject.put(KEY_OPT_VERSION, 0);
		if (mdns) {
			jsonObject.put(KEY_FLUSH_CACHE, CACHE.NO_FLUSH_CACHE.code);
		}
		jsonObject.put(KEY_OPT_UDP_SIZE, MAX_UDP_SIZE);
		jsonObject.put(KEY_OPT_DO_BIT, dnssec);
		return jsonObject;
	}

	@SuppressWarnings("unchecked")
	public JSONObject getAsJson(APPLICATION_PROTOCOL applicationProtocol) {
		boolean mdns = (applicationProtocol == APPLICATION_PROTOCOL.MDNS);
		if (qcount.equals(Q_COUNT.OPT)) {
			return getOPTAsJson(mdns);
		}
		JSONObject jsonObject = new JSONObject();

		// check if SRV
		if (qcount == Q_COUNT.SRV) {
			jsonObject.put(KEY_SRV_SERVICE, srvService);
			jsonObject.put(KEY_SRV_PROTOCOL, srvProtocol);
			jsonObject.put(KEY_SRV_NAME, srvName);
		} else {
			jsonObject.put(NAME_KEY, nameAsString);
		}

		jsonObject.put(TYPE_KEY, qcount.toString());
		jsonObject.put(CLASS_KEY, qtype);
		jsonObject.put(TTL_KEY, ttl);
		jsonObject.put(DATA_KEY, rdata.getAsJson());

		// check if mdns
		if (cache != null) {
			jsonObject.put(CACHE_KEY, cache.toString());
		}
		return jsonObject;
	}

	@SuppressWarnings("unchecked")
	private JSONObject getOPTAsJson(boolean mdns) {
		JSONObject json = new JSONObject();
		json.put(KEY_OPT_UDP_SIZE, size.getValue());
		if (mdns) {
			json.put(KEY_FLUSH_CACHE, cache);
		}
		json.put(KEY_OPT_RCODE, (int) rCode);
		json.put(KEY_OPT_VERSION, (int) version);

		json.put(KEY_OPT_DO_BIT, doBit.getValue() >= DO_BIT_VALUE);

		DnsRecordOPT r = (DnsRecordOPT) rdata;
		if (!r.getIsNull()) {
			json.put(KEY_OPT_OPTIONS, rdata.getAsJson());
		}
		return json;
	}

	public byte[] getDnssecAsBytes() {
		ArrayList<Byte> bytes = new ArrayList<>();
		bytes.add((byte) 0x00);
		bytes.add(Q_COUNT.OPT.code.getAsBytes()[1]);
		bytes.add(Q_COUNT.OPT.code.getAsBytes()[0]);
		bytes.add(new UInt16(MAX_UDP_SIZE).getAsBytes()[1]);
		bytes.add(new UInt16(MAX_UDP_SIZE).getAsBytes()[0]);
		bytes.add((byte) 0x00);
		bytes.add((byte) 0x00);
		bytes.add(new UInt16(DO_BIT_VALUE).getAsBytes()[1]);
		bytes.add(new UInt16(DO_BIT_VALUE).getAsBytes()[0]);
		bytes.add((byte) 0x00);
		bytes.add((byte) 0x00);

		byte[] returnArray = new byte[bytes.size()];

		for (int i = 0; i < returnArray.length; i++) {
			returnArray[i] = bytes.get(i);
		}
		return returnArray;
	}

	public static byte[] getDnssecAsBytesMDNS(boolean dnssecSignatures) {
		ArrayList<Byte> bytes = new ArrayList<>();
		bytes.add((byte) 0x00);
		bytes.add(Q_COUNT.OPT.code.getAsBytes()[1]);
		bytes.add(Q_COUNT.OPT.code.getAsBytes()[0]);
		bytes.add(new UInt16(MAX_UDP_SIZE).getAsBytes()[1]);
		bytes.add(new UInt16(MAX_UDP_SIZE).getAsBytes()[0]);
		bytes.add((byte) 0x00);
		bytes.add((byte) 0x00);
		if (dnssecSignatures) {
			bytes.add( new UInt16(DO_BIT_VALUE).getAsBytes()[1]);
			bytes.add( new UInt16(DO_BIT_VALUE).getAsBytes()[0]);
		} else {
			bytes.add(new UInt16(0).getAsBytes()[1]);
			bytes.add(new UInt16(0).getAsBytes()[0]);
		}
		bytes.add((byte) 0x00);
		bytes.add((byte) 0x00);

		byte[] returnArray = new byte[bytes.size()];

		for (int i = 0; i < returnArray.length; i++) {
			returnArray[i] = bytes.get(i);
		}
		return returnArray;
	}

	public String getDomain() {
		return this.nameAsString;
	}

}
