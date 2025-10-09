package application;
/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * */
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.javafx.collections.NonIterableChange;
import javafx.scene.input.KeyCode;
import lombok.Getter;
import lombok.Setter;
import models.GeneralConfig;
import models.LoadTestConfig;
import models.NameServer;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.LinkedList;
import java.util.List;

public class Config {

    @Getter
    private static final String configResource = "./config.properties";

    @Getter
    @Setter
    private static PropertiesConfiguration conf = null;

    @Getter
    @Setter
    private static List<NameServer> rootNameServers = new LinkedList<>();

    @Getter
    @Setter
    private static List<NameServer> NameServers = new LinkedList<>();

    @Getter
    @Setter
    private static List<NameServer> DoHNameServers = new LinkedList<>();

    @Getter
    @Setter
    private static LoadTestConfig loadTestConfig = new LoadTestConfig();

    @Getter
    @Setter
    private static GeneralConfig generalConfig = new GeneralConfig();

    public static final String ROOTSERVERSDOMAINNAMES = "rootServersDomainNames";

    public static final String SERVERSDOMAINNAMES = "serversDomainNames";

    public static final String LOADTESTCONFIG = "loadTestConfig";

    public static final String GENERALCONFIG = "generalConfig";

    public static PropertiesConfiguration getConfProperties() throws ConfigurationException {
        if (Config.conf == null) {
            Config.conf = new PropertiesConfiguration(Config.configResource);
        }
        return Config.conf;
    }

    @SuppressWarnings("unchecked")
    public static void loadConfiguration() throws ConfigurationException, IOException, ParseException {
        ObjectMapper objectMapper = new ObjectMapper();
        List<NameServer> rootNameServerList = objectMapper.readValue(
                new File(Config.getConfProperties().getString(ROOTSERVERSDOMAINNAMES)), new TypeReference<List<NameServer>>() {
                });
        rootNameServerList.forEach(nameServer -> nameServer.setKeyCode(Config.keyCodeFromDomainName(nameServer.getDomainName())));
        Config.setRootNameServers(rootNameServerList);

        List<NameServer> nameServerList = objectMapper.readValue(
                new File(Config.getConfProperties().getString(SERVERSDOMAINNAMES)), new TypeReference<List<NameServer>>() {
                });
        Config.setNameServers(nameServerList);

        LoadTestConfig loadTestConfig = objectMapper.readValue(
                new File(Config.getConfProperties().getString(LOADTESTCONFIG)), new TypeReference<LoadTestConfig>() {
                });
        Config.setLoadTestConfig(loadTestConfig);
        GeneralConfig generalConfig = objectMapper.readValue(
                new File(Config.getConfProperties().getString(GENERALCONFIG)), new TypeReference<GeneralConfig>() {
                });
        Config.setGeneralConfig(generalConfig);


    }

    /*
    * Converting domain name of root server to KeyCode object
    * */
    public static KeyCode keyCodeFromDomainName(String domain) {
        domain = domain.toUpperCase();
        String parts[] = domain.split("\\.");
        if (Array.getLength(parts) > 0) {
            return KeyCode.getKeyCode(parts[0]);
        }
        return null;
    }
}
