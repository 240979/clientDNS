/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * Based on work of Martin Biolek (https://github.com/mbio16/clientDNS)
 * Added LLMNR and load testing domain names and changed the way of storing domain names
 * Added screen hash which is used to open app on screen where was previously closed
 * */
package models;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.swing.filechooser.FileSystemView;

import lombok.Data;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

@Data
public class Settings {

	public static final String SETTINGS_FILE_NAME = "settings.json";
	public static final String SETTINGS_FOLDER_NAME = "DNSClient";
	public static final String DNS_SERVERS = "DNS_SERVERS";
	public static final String DOMAIN_NAMES_LLMNR = "DOMAIN_NAMES_LLMNR";
	public static final String DOMAIN_NAMES_LOAD = "DOMAIN_NAMES_LOAD";
	public static final String DOMAIN_NAMES_mDNS = "DOMAIN_NAMES_mDNS";
	public static final String DOMAIN_NAMES_DNS = "DOMAIN_NAMES_DNS";
	public static final String LAST_USED_INTERFACE = "LAST_USED_INTERFACE";
	public static final String LAST_USED_SCREEN = "LAST_USED_SCREEN";
	private String filePath;
	private File file;
	private NetworkInterface netInterface;
	private ArrayList<String> dnsServers;
	private List<String> domainNamesDNS;
	private List<String> domainNamesMDNS;
	private List<String> domainNamesLLMNR;
	private List<String> domainNamesLOAD;
	private List<String> screensHash;
	private static final Logger LOGGER = Logger.getLogger(Settings.class.getName());

	public Settings() {
		dnsServers = new ArrayList<>();
		domainNamesDNS = new ArrayList<>();
		domainNamesMDNS = new ArrayList<>();
		domainNamesLLMNR = new ArrayList<>();
		domainNamesLOAD = new ArrayList<>();
		screensHash = new ArrayList<>();
		checkIfFileExistsOrCreate();
		readValues();
	}

	private void checkIfFileExistsOrCreate() {
		String userDocumentsFolder = FileSystemView.getFileSystemView().getDefaultDirectory().getPath().toString();

		Path folderPath = Paths.get(userDocumentsFolder, SETTINGS_FOLDER_NAME);
		Path filePath = Paths.get(folderPath.toString(), SETTINGS_FILE_NAME);

		File folder = new File(folderPath.toString());
		file = new File(filePath.toString());

		if (!folder.exists()) {
			folder.mkdirs();
		}
		if (!file.exists()) {
			try {
				file.createNewFile();
				setupJsonFile();
			} catch (Exception e) {
				LOGGER.severe("Could not write to file: \n" + e.toString());
			}
		}
		this.filePath = file.getPath().toString();
	}

	@SuppressWarnings("unchecked")
	private void setupJsonFile() throws IOException {
		Map<String, List<String>> jsonMap = new HashMap<>();
		jsonMap.put(DNS_SERVERS, dnsServers);
		jsonMap.put(DOMAIN_NAMES_DNS, domainNamesDNS);
		jsonMap.put(DOMAIN_NAMES_mDNS, domainNamesMDNS);
		jsonMap.put(DOMAIN_NAMES_LLMNR, domainNamesLLMNR);
		jsonMap.put(DOMAIN_NAMES_LOAD, domainNamesLOAD);
		jsonMap.put(LAST_USED_SCREEN, screensHash);
		Map<String, String> jsonMap2 = new HashMap<String, String>();
		if (netInterface == null) {
			netInterface = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
		}
		jsonMap2.put(LAST_USED_INTERFACE, netInterface.getName());
		JSONObject json = new JSONObject(jsonMap);
		json.putAll(jsonMap2);
		try (FileWriter fw = new FileWriter(file, StandardCharsets.UTF_8);
				BufferedWriter writer = new BufferedWriter(fw)) {
			writer.append(json.toString());
		}
		jsonMap.clear();
	}

	private void readValues() {
		JSONParser jsonParser = new JSONParser();
		try {
			FileReader reader = new FileReader(filePath, StandardCharsets.UTF_8);
			JSONObject jsonObject = (JSONObject) jsonParser.parse(reader);
			dnsServers = readJsonArraylist(DNS_SERVERS, jsonObject);
			domainNamesMDNS = readJsonArraylist(DOMAIN_NAMES_mDNS, jsonObject);
			domainNamesDNS = readJsonArraylist(DOMAIN_NAMES_DNS, jsonObject);
			domainNamesLLMNR = readJsonArraylist(DOMAIN_NAMES_LLMNR, jsonObject);
			domainNamesLOAD = readJsonArraylist(DOMAIN_NAMES_LOAD, jsonObject);
			screensHash = readJsonArraylist(LAST_USED_SCREEN, jsonObject);
			try {
				netInterface = NetworkInterface.getByName((String) jsonObject.get(LAST_USED_INTERFACE));
			} catch (Exception e) {
				netInterface = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
			}
			reader.close();
			jsonObject = null;
			jsonParser = null;
		} catch (Exception e) {
			LOGGER.severe("Could not parse settings from file: \n" + e.toString());
		}
	}

	private ArrayList<String> readJsonArraylist(String key, JSONObject jsonObject) {
		JSONArray jsonArray = (JSONArray) jsonObject.get(key);
		ArrayList<String> list = new ArrayList<String>();
		jsonArray.forEach(o -> list.add((String) o));
		return list;
	}

	public void addMDNSDomain(String domain) {
		addDomain(domain,domainNamesMDNS);
	}

	public void addDNSDomain(String domain) {
		addDomain(domain,domainNamesDNS);
	}

	public void addLLMNRDomain(String domain){
		addDomain(domain,domainNamesLLMNR);
	}

	public void addLoadDomain(String domain){
		if (!domainNamesLOAD.contains(domain)) {
			domainNamesLOAD.add(domain);
			LOGGER.info(domain+" domain name added");
		} else {
			LOGGER.info(domain + " domain already in list");
		}
	}

	private void addDomain(String domain, List<String> list){
		if (DomainConvert.isValidDomainName(domain)) {
			if (!list.contains(domain)) {
				list.add(domain);
				LOGGER.info(domain+" domain name added");
			} else {
				LOGGER.info(domain + " domain already in list");
			}
		} else {
			LOGGER.warning(domain + " domain name is not valid");
		}
	}

	public void saveScreenHash(String hash) {
		screensHash.clear();
		screensHash.add(hash);
	}

	public void appIsClossing() {
		file.delete();
		checkIfFileExistsOrCreate();
		LOGGER.info("Setting written in file");
	}

	public void eraseDomainNames() {
		this.domainNamesDNS = new ArrayList<String>();
	}

	public void eraseDNSServers() {
		this.dnsServers = new ArrayList<String>();
	}

	public void eraseMDNSDomainNames() {
		this.domainNamesMDNS = new ArrayList<String>();
	}

	public void eraseLLMNRDomainNames() {
		domainNamesLLMNR = new ArrayList<>();
	}

	public void eraseLOADDomainNames() {
		domainNamesLOAD = new ArrayList<>();
	}
}
