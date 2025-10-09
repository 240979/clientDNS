package models;
/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * */
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GeneralConfig {

    @JsonProperty(value = "maxMessagesSent", defaultValue = "3")
    private int maxMessagesSent;

    @JsonProperty(value = "timeout", defaultValue = "2000")
    private long timeout;

    @JsonProperty(value = "language", defaultValue = "CZ")
    private String language;

    public GeneralConfig(){
        maxMessagesSent = 3;
        timeout = 2000;
    }

    public GeneralConfig(int maxMessagesSent, int timeout){

        this.maxMessagesSent = maxMessagesSent;
        this.timeout = timeout;
    }

}
