package tasks;
/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * Methods used from Martin Biolek thesis are marked with comment
 * */
import enums.APPLICATION_PROTOCOL;
import enums.Q_COUNT;
import enums.TRANSPORT_PROTOCOL;
import exceptions.*;
import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import models.Ip;
import models.MessageParser;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import tasks.runnables.RequestResultsUpdateRunnable;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Set;

/**
 * Class representing protocol DNS over HTTPS
 */
public class DNSOverHTTPSTask extends DNSTaskBase {

    private boolean cdFlag;
    private boolean isGet;
    private String serverDomainName;
    public DNSOverHTTPSTask(boolean recursion, boolean adFlag, boolean cdFlag, boolean doFlag, String domain,
                            Q_COUNT[] types, TRANSPORT_PROTOCOL transport_protocol,
                            APPLICATION_PROTOCOL application_protocol, String resolverIP, NetworkInterface netInterface,
                            boolean isGet, String resolverUri)
            throws UnsupportedEncodingException, NotValidIPException, NotValidDomainNameException, UnknownHostException {
        super(recursion, adFlag, cdFlag, doFlag, domain, types, transport_protocol, application_protocol, resolverIP, netInterface,null);
        this.cdFlag = cdFlag;
        this.isGet = isGet;
        this.serverDomainName = resolverUri;
    }
    public DNSOverHTTPSTask(boolean recursion, boolean adFlag, boolean cdFlag, boolean doFlag, String domain,
                            Q_COUNT[] types, TRANSPORT_PROTOCOL transport_protocol,
                            APPLICATION_PROTOCOL application_protocol, String resolverIP, NetworkInterface netInterface,
                            boolean isGet)
            throws UnsupportedEncodingException, NotValidIPException, NotValidDomainNameException, UnknownHostException {
        super(recursion, adFlag, cdFlag, doFlag, domain, types, transport_protocol, application_protocol, resolverIP, netInterface, null);
        this.cdFlag = cdFlag;
        this.isGet = isGet;
    }

    /*
     * Body of method taken from Martin Biolek thesis and modified
     * */
    @Override
    protected void sendData() throws TimeoutException, MessageTooBigForUDPException, InterfaceDoesNotHaveIPAddressException, IOException, InterruptedException, ParseException, HttpCodeException, OtherHttpException, NotValidDomainNameException, NotValidIPException, QueryIdNotMatchException {
        //try {
           String httpsDomain = resolver.split("/")[0];
            //String httpsDomain = serverDomainName.split("/")[0];
            CloseableHttpResponse response;
            String[] values = new String[]{domainAsString, qcountAsString(), "" + doFlag, "" + cdFlag};

            setMessagesSent(1);

            String uri = addParamtoUris(resolver, httpRequestParamsName, values);
            //String uri = addParamtoUris(serverDomainName, httpRequestParamsName, values);
            updateProgressUI();

            response = sendAndReceiveDoH(uri, httpsDomain, isGet);
            setDuration(calculateDuration());
            if (response.getStatusLine().getStatusCode() == 200) {
                String content = EntityUtils.toString(response.getEntity());
                JSONParser parser = new JSONParser();
                this.httpResponse = (JSONObject) parser.parse(content);
                byteSizeResponseDoHDecompresed = getAllHeadersSize(response.getAllHeaders());
                byteSizeResponseDoHDecompresed += content.getBytes().length;
                parseResponseDoh(content);
            } else {
                throw new HttpCodeException(response.getStatusLine().getStatusCode());
            }
            closeHttpConnection();
            if(!massTesting){
                Platform.runLater(() -> controller.getSendButton().setText(controller.getButtonText()));
            }
        /*} catch (HttpCodeException | ParseException | InterfaceDoesNotHaveIPAddressException | SocketException
                | SSLPeerUnverifiedException | InterruptedIOException e) {
            exc = e;
            closeHttpConnection();
            e.printStackTrace();
            Platform.runLater(new StopProgressBar(this, e.getClass().getSimpleName()));
            throw e;
        } catch (Exception e) {
            exc = e;
            closeHttpConnection();
            e.printStackTrace();
            Platform.runLater(new StopProgressBar(this, "Exception"));
            throw new OtherHttpException();
        }*/
    }

    @Override
    protected void updateProgressUI() {
    }

    @Override
    protected void updateResultUI() {
        setByteSizeResponse(byteSizeResponseDoHDecompresed);
        Platform.runLater(new RequestResultsUpdateRunnable(this));
    }

    @Override
    public void stopExecution() {
        closeHttpConnection();
    }

    @Override
    protected void cleanup() {
        closeHttpConnection();
    }

    @Override
    protected MessageParser parseResponse() throws QueryIdNotMatchException, UnknownHostException, UnsupportedEncodingException {
        return new MessageParser(httpResponse);
    }

    @Override
    protected void setRequestAndResponse(MessageParser parser) {
        setResponse(parseJSON("Answer", httpResponse));
        setRequest(getAsTreeItem());
    }

    @SuppressWarnings("unchecked")
    protected static TreeItem<String> parseJSON(String name, Object json) {
        TreeItem<String> item = new TreeItem<>();
        if (json instanceof JSONObject) {
            item.setValue(name);
            JSONObject object = (JSONObject) json;
            ((Set<Map.Entry>) object.entrySet()).forEach(entry -> {
                String childName = (String) entry.getKey();
                Object childJson = entry.getValue();
                TreeItem<String> child = parseJSON(childName, childJson);
                item.getChildren().add(child);
            });
        } else if (json instanceof JSONArray) {
            item.setValue(name);
            JSONArray array = (JSONArray) json;
            for (int i = 0; i < array.size(); i++) {
                String childName = String.valueOf(i);
                Object childJson = array.get(i);
                TreeItem<String> child = parseJSON(childName, childJson);
                item.getChildren().add(child);
            }
        } else {
            item.setValue(name + " : " + json);
        }
        return item;
    }

    /*
     * Body of method taken from Martin Biolek thesis
     * */
    private String addParamtoUris(String uri, String[] paramNames, String[] values) {

        String splited[] = uri.split("/");
        if (Ip.isIpv6Address(splited[0])) {

            uri = "[" + splited[0] + "]";
            if (splited.length > 1) {
                uri += "/" + splited[1];
            }
        }
        String result = "https://" + uri + "?";
        for (int i = 0; i < values.length; i++) {
            if (i == 0) {
                result += paramNames[i] + "=" + values[i];
            } else {
                result += "&" + paramNames[i] + "=" + values[i];
            }
        }
        return result;

    }

    /*
     * Body of method taken from Martin Biolek thesis and modified
     * */
    private String qcountAsString() {
        String result = "";
        for (int i = 0; i < qcountTypes.length; i++) {
            if (i == 0) {
                result += qcountTypes[i];
            } else {
                result += "," + qcountTypes[i];
            }
        }
        return result;
    }

    /*
     * Body of method taken from Martin Biolek thesis and modified to use directly IP instead of domain name of DNS server
     * */
    private CloseableHttpResponse sendAndReceiveDoH(String uri, String host, boolean httpGet)
            throws ClientProtocolException, IOException, InterfaceDoesNotHaveIPAddressException {
        HttpRequestBase request;
        if (httpGet) {
            request = new HttpGet(uri);

        } else {
            request = new HttpPost(uri);
        }
        request.addHeader("Accept", "application/dns-json");
        request.addHeader("Accept-Encoding", "gzip, deflate, br");
        request.addHeader("User-Agent", "Client-DNS");
        // host header omitted when using IP instead of domain
        if (!Ip.isIpValid(host)) {
            request.addHeader("Host", host);
        }
        request.setConfig(getRequestConfig(host));
        httpRequestAsString(request);
        httpClient = HttpClients.createDefault();
        startTime = System.nanoTime();
        CloseableHttpResponse response = httpClient.execute(request);
        stopTime = System.nanoTime();
        setStartTime(startTime);
        setStopTime(stopTime);
        return response;

    }

    /*
     * Body of method taken from Martin Biolek thesis
     * */
    private RequestConfig getRequestConfig(String host) throws InterfaceDoesNotHaveIPAddressException {
        try {
            if (Ip.isIpValid(host)) {
                return RequestConfig.custom().setLocalAddress(Ip.getIpAddressFromInterface(interfaceToSend, host))
                        .build();
            }
            return RequestConfig.custom().setLocalAddress(interfaceToSend.getInterfaceAddresses().get(0).getAddress())
                    .build();
        } catch (Exception e) {
            //e.printStackTrace();
            LOGGER.severe(ExceptionUtils.getStackTrace(e));
            throw new InterfaceDoesNotHaveIPAddressException();
        }
    }

    /*
     * Body of method taken from Martin Biolek thesis
     * */
    private void httpRequestAsString(HttpRequestBase request) {
        String result = request.toString() + "\n";
        for (org.apache.http.Header httpHeader : request.getAllHeaders()) {
            result += httpHeader.toString() + "\n";
        }
        this.byteSizeQuery = result.getBytes().length;
        setByteSizeQuery(byteSizeQuery);
        httpRequest = result;
    }

    /*
     * Body of method taken from Martin Biolek thesis
     * */
    private int getAllHeadersSize(org.apache.http.Header[] allHeaders) {
        int size = 0;
        for (org.apache.http.Header header : allHeaders) {
            size += header.toString().getBytes().length;
        }
        return size + 1;
    }

    /*
     * Body of method taken from Martin Biolek thesis
     * */
    protected void closeHttpConnection() {
        try {
            httpClient.close();
        } catch (Exception e) {
            // already closed, just return
            return;
        }
    }

    /*
     * Body of method taken from Martin Biolek thesis
     * */
    private void parseResponseDoh(String response) throws ParseException {
        JSONParser parser = new JSONParser();
        this.httpResponse = (JSONObject) parser.parse(response);
    }
}
