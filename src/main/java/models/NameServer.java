package models;
/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * */
import com.fasterxml.jackson.annotation.JsonProperty;
import javafx.scene.input.KeyCode;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedList;
import java.util.List;

/**
 * Class representing one DNS name server loaded from JSON files.
 */
@Getter
@Setter
@EqualsAndHashCode
public class NameServer {

    @JsonProperty(value = "name")
    private String name;

    @JsonProperty(value = "domainName")
    private String domainName;

    @JsonProperty(value = "ipv4")
    private List<String> ipv4;

    @JsonProperty(value = "ipv6")
    private List<String> ipv6;

    @JsonProperty()
    private KeyCode keyCode = null;

    @JsonProperty(value = "doh")
    private boolean doh = false;

    @JsonProperty(value = "isGet")
    private boolean isGet = false;

    @JsonProperty(value = "dohOnly")
    private boolean dohOnly = false;

    @JsonProperty(value = "path")
    private String path = "";

    @JsonProperty(value = "dot")
    private boolean dot = false;

    @JsonProperty(value = "dotOnly")
    private boolean dotOnly = false;

    @JsonProperty(value = "doq")
    private boolean doq = false;

    @JsonProperty(value = "port")
    private int port = 53;

    public NameServer() {

    }

    public NameServer(String name, String domainName, List<String> iPv4Addr, List<String> iPv6Addr) {
        super();
        this.name = name;
        this.domainName = domainName;
        this.ipv4 = iPv4Addr;
        this.ipv6 = iPv6Addr;
    }

    public NameServer(String name, String domainName, String iPv4Addr, String iPv6Addr) {
        this.name = name;
        this.domainName = domainName;
        this.ipv4 = new LinkedList<>();
        this.ipv6 = new LinkedList<>();
        this.ipv4.add(iPv4Addr);
        this.ipv6.add(iPv6Addr);
    }

    public NameServer(String name, String domainName, String iPv4Addr, String iPv6Addr, boolean doh, boolean isGet, String path) {
        this(name, domainName, iPv4Addr, iPv6Addr);
        this.doh = doh;
        this.isGet = isGet;
        this.path = path;
    }

    public NameServer(String name, String domainName, List<String> iPv4Addr, List<String> iPv6Addr, boolean doh, boolean isGet, String path) {
        this(name, domainName, iPv4Addr, iPv6Addr);
        this.doh = doh;
        this.isGet = isGet;
        this.path = path;
    }

    public NameServer(String name, String domainName, List<String> iPv4Addr, List<String> iPv6Addr, boolean doh, boolean isGet, String path, boolean doq, int port) {
        this(name, domainName, iPv4Addr, iPv6Addr, doh, isGet, path);
        this.doq = doq;
        this.port = port;
    }

}
