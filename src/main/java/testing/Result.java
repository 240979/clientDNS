package testing;
/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * */
import lombok.Data;
import models.NameServer;
import models.Response;
import org.json.simple.JSONObject;

import java.util.LinkedList;
import java.util.List;

/*
* Stores data about test of one domain against NameServer
* */
@Data
public class Result {
    private List<Double> durations;
    private Double average;
    private String domain;
    private String name;
    private boolean complete;
    private String ip;
    private NameServer ns;
    private Double max;
    private Double min;
    private Long successful;
    private Long failed;
    private List<Boolean> success;
    private Integer responseSize;
    private List<Exception> exceptions;
    private List<List<Response>> responses;

    public Result(String name, String ip, String domain) {
        this.name = name;
        this.ip = ip;
        this.domain = domain;
        average = 0.0;
        max = 0.0;
        min = 0.0;
        successful = 0L;
        failed = 0L;
        durations = new LinkedList<>();
        complete = false;
        success = new LinkedList<>();
        exceptions = new LinkedList<>();
        responses = new LinkedList<>();
        responseSize = 0;
    }

    public Result(NameServer ns, String ip, String domain) {
        this(ns.getName(), ip, domain);
        this.ns = ns;
        this.domain = domain;
    }

    public String toCsvString(){
        return ""+name+";"+domain+";"+average+";"+min+";"+max+";"+successful+";"+failed+";"+responseSize+"\n";
    }

    public JSONObject toJsonObject(){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("Name", name);
        jsonObject.put("Domain", domain);
        jsonObject.put("Average", average);
        jsonObject.put("Min", min);
        jsonObject.put("Max", max);
        jsonObject.put("Successful", successful);
        jsonObject.put("Failed", failed);
        jsonObject.put("ResponseSize", responseSize);
        return jsonObject;
    }

}
