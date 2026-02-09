package models;

import enums.APPLICATION_PROTOCOL;
import enums.TRANSPORT_PROTOCOL;
import lombok.Getter;
import lombok.Setter;

import java.net.NetworkInterface;

public class ConnectionSettings {
    @Getter
    String domain;
    @Getter
    @Setter
    TRANSPORT_PROTOCOL transport_protocol;
    @Getter
    APPLICATION_PROTOCOL application_protocol;
    @Getter
    @Setter
    String resolverIP;
    @Getter
    NetworkInterface netInterface;
    @Getter
    @Setter
    boolean isGet;
    @Getter
    @Setter
    String resolverUri;
    @Getter
    @Setter
    String path;
    @Getter
    @Setter
    boolean isReqJsonFormat;
    @Getter
    @Setter
    boolean isDomainNameUsed;
    @Getter
    int resolverPort;
    @Getter
    boolean holdConnection;


    private ConnectionSettings(ConnectionSettingsBuilder csb){
        this.transport_protocol = csb.transport_protocol;
        this.application_protocol = csb.application_protocol;
        this.resolverIP = csb.resolverIP;
        this.netInterface = csb.netInterface;
        this.isGet = csb.isGet;
        this.resolverUri  = csb.resolverUri;
        this.isReqJsonFormat = csb.isReqJsonFormat;
        this.isDomainNameUsed = csb.isDomainNameUsed;
        this.path = csb.path;
        this.resolverPort = csb.resolverPort;
        this.holdConnection = csb.holdConnection;
    }
    public ConnectionSettings build(){
        return this;
    }
    public static class ConnectionSettingsBuilder {
        TRANSPORT_PROTOCOL transport_protocol;
        APPLICATION_PROTOCOL application_protocol;
        String resolverIP;
        NetworkInterface netInterface;
        boolean isGet;
        String resolverUri;
        boolean isReqJsonFormat;
        boolean isDomainNameUsed;
        String path;
        int resolverPort;
        boolean holdConnection;

        public ConnectionSettingsBuilder(){
            this.transport_protocol = null;
            this.application_protocol = null;
            this.resolverIP = null;
            this.netInterface = null;
            this.isGet = false;
            this.resolverUri  = null;
            this.isReqJsonFormat = false;
            this.isDomainNameUsed = false;
            this.path = null;
            this.resolverPort = 0;
            holdConnection=false;
        }

        public ConnectionSettings build(){
            return new ConnectionSettings(this);
        }
        public ConnectionSettingsBuilder transport_protocol(TRANSPORT_PROTOCOL transport_protocol){
            this.transport_protocol = transport_protocol;
            return this;
        }
        public ConnectionSettingsBuilder application_protocol(APPLICATION_PROTOCOL application_protocol){
            this.application_protocol = application_protocol;
            return this;
        }
        public ConnectionSettingsBuilder resolverIP(String resolverIP){
            this.resolverIP = resolverIP;
            return this;
        }
        public ConnectionSettingsBuilder netInterface(NetworkInterface netInterface){
            this.netInterface = netInterface;
            return this;
        }
        public ConnectionSettingsBuilder isGet(boolean isGet){
            this.isGet = isGet;
            return this;
        }
        public ConnectionSettingsBuilder resolverUri(String resolverUri){
            this.resolverUri = resolverUri;
            return this;
        }
        public ConnectionSettingsBuilder isReqJsonFormat(boolean isReqJsonFormat){
            this.isReqJsonFormat = isReqJsonFormat;
            return this;
        }
        public ConnectionSettingsBuilder isDomainNameUsed(boolean isDomainNameUsed){
            this.isDomainNameUsed = isDomainNameUsed;
            return this;
        }
        public ConnectionSettingsBuilder path(String path){
            this.path = path;
            return this;
        }
        public ConnectionSettingsBuilder resolverPort(int resolverPort){
            this.resolverPort = resolverPort;
            return this;
        }
        public ConnectionSettingsBuilder holdConnection(boolean holdConnection){
            this.holdConnection = holdConnection;
            return this;
        }

    }

}
