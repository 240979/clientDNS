package tasks;
/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * Methods used from Martin Biolek thesis are marked with comment
 * */

import com.google.gson.GsonBuilder;
import enums.*;
import exceptions.*;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TreeItem;
import lombok.Getter;
import lombok.Setter;
import models.*;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import ui.GeneralController;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

/**
 * Super class of every task which sends requests via specific protocol
 */
@Getter
@Setter
public abstract class DNSTaskBase extends Task<Void> {

    protected Header header;
    protected ArrayList<Request> requests;
    protected TRANSPORT_PROTOCOL transport_protocol;
    protected APPLICATION_PROTOCOL application_protocol;
    protected byte[] messageAsBytes;
    protected int byteSizeQuery;
    protected int byteSizeResponse;
    protected String domainAsString;
    protected InetAddress ip;
    protected String resolver;
    int size;
    protected Socket socket;
    protected byte[] receiveReply;
    protected boolean doFlag;
    protected TreeItem<String> root;
    protected long startTime;
    protected long stopTime;
    protected boolean adFlag;
    protected static Map<String, TCPConnection> tcp = null;
    protected IP_PROTOCOL ipProtocol;
    protected RESPONSE_MDNS_TYPE mdnsType;
    protected boolean mdnsDnssecSignatures;
    protected boolean closeConnection;
    protected Q_COUNT[] qcountTypes;
    protected NetworkInterface interfaceToSend;
    protected boolean wasSend;
    protected ProgressBar progressBar;
    public static final int MAX_MESSAGES_SENT = 3;
    public static final int TIME_OUT_MILLIS = 2000;
    public static final int MAX_UDP_SIZE = 1232;
    public static final int MDNS_PORT = 5353;
    public static final int LLMNR_PORT = 5355;
    public static final int DNS_PORT = 53;
    public static final String KEY_HEAD = "Head";
    public static final String KEY_QUERY = "Questions";
    public static final String KEY_REQUEST = "Request";
    public static final String KEY_ADDITIONAL_RECORDS = "Aditional records";
    public static final String KEY_LENGHT = "Lenght";

    public static Logger LOGGER = Logger.getLogger(DNSTaskBase.class.getName());
    protected GeneralController controller;
    protected boolean massTesting = false;
    protected boolean stop = false;
    protected Exception exc = null;
    protected MessageParser parser;

    // properties connected to GUI
    private SimpleIntegerProperty messagesSentProperty;
    private SimpleDoubleProperty durationProperty;
    private ObjectProperty<TreeItem<String>> requestProperty;
    private ObjectProperty<TreeItem<String>> responseProperty;
    private SimpleIntegerProperty querySizeProperty = new SimpleIntegerProperty();
    private SimpleIntegerProperty responseSizeProperty = new SimpleIntegerProperty();

    public int getQuerySizeProperty() {
        return querySizeProperty.get();
    }

    public SimpleIntegerProperty querySizeProperty() {
        return querySizeProperty;
    }

    public void setQuerySizeProperty(int querySizeProperty) {
        this.querySizeProperty.set(querySizeProperty);
    }

    public int getResponseSizeProperty() {
        return responseSizeProperty.get();
    }

    public SimpleIntegerProperty responseSizeProperty() {
        return responseSizeProperty;
    }

    public void setResponseSizeProperty(int responseSizeProperty) {
        this.responseSizeProperty.set(responseSizeProperty);
    }

    // variables used to store values before updating GUIS
    private int messagesSent;
    private double duration;
    private TreeItem<String> request;
    private TreeItem<String> response;

    public int getMessagesSentProperty() {
        return messagesSentProperty.get();
    }

    public void setMessagesSentProperty(int messagesSentProperty) {
        this.messagesSentProperty.set(messagesSentProperty);
    }

    public SimpleIntegerProperty messagesSentPropertyProperty() {
        return messagesSentProperty;
    }

    public double getDurationProperty() {
        return durationProperty.get();
    }

    public void setDurationProperty(double durationProperty) {
        this.durationProperty.set(durationProperty);
    }

    public SimpleDoubleProperty durationPropertyProperty() {
        return durationProperty;
    }

    public TreeItem<String> getRequestProperty() {
        return requestProperty.get();
    }

    public void setRequestProperty(TreeItem<String> requestProperty) {
        this.requestProperty.set(requestProperty);
    }

    public ObjectProperty<TreeItem<String>> requestPropertyProperty() {
        return requestProperty;
    }

    public TreeItem<String> getResponseProperty() {
        return responseProperty.get();
    }

    public void setResponseProperty(TreeItem<String> responseProperty) {
        this.responseProperty.set(responseProperty);
    }

    public ObjectProperty<TreeItem<String>> responsePropertyProperty() {
        return responseProperty;
    }

    public void setReceiveReply(byte[] receiveReply) {
        this.receiveReply = receiveReply;
        //System.out.println("setReceiveReply " + receiveReply.length);
        LOGGER.info("setReceiveReply " + receiveReply.length);
        byteSizeResponse = this.receiveReply.length;
    }

    public void stopExecution(){}

    public static Map<String, TCPConnection> getTcp() {
        return DNSTaskBase.tcp;
    }

    public static void setTcp(Map<String, TCPConnection> tcp) {
        DNSTaskBase.tcp = tcp;
    }

    public static TCPConnection getTcpConnectionForServer(String name) {
        return DNSTaskBase.tcp == null ? null : DNSTaskBase.tcp.get(name);
    }

    /**
     * Method used to store TCP connection to server
     * @param name server name
     * @param addr server address
     * @throws UnknownHostException
     */
    public static void setTcpConnectionForServer(String name, String addr) throws UnknownHostException {
        if (DNSTaskBase.tcp == null) {
            DNSTaskBase.tcp = new HashMap<>();
        }
        DNSTaskBase.tcp.put(name, new TCPConnection(InetAddress.getByName(addr)));
    }

    /**
     * Method terminates all stored TCP connections
     */
    public static void terminateAllTcpConnections() {
        if (DNSTaskBase.tcp != null) {
            DNSTaskBase.tcp.forEach((s, tcpConnection) -> {
                try {
                    tcpConnection.closeAll();
                } catch (IOException e) {
                    //e.printStackTrace();
                    LOGGER.severe(ExceptionUtils.getStackTrace(e));
                }
            });
        }
    }
/*
    public DNSTaskBase(boolean recursion, boolean adFlag, boolean cdFlag, boolean doFlag, String domain, Q_COUNT[] types,
                       TRANSPORT_PROTOCOL transport_protocol, APPLICATION_PROTOCOL application_protocol, String resolverIP, NetworkInterface netInterface, RESPONSE_MDNS_TYPE mdnsType) throws UnsupportedEncodingException, NotValidIPException, NotValidDomainNameException, UnknownHostException {
        super();
        this.mdnsType = mdnsType;
        requests = new ArrayList<Request>();
        header = new Header(recursion, adFlag, types.length, doFlag, cdFlag);
        size = Header.getSize();
        // TODO addRequests(types, checkAndStripFullyQualifyName(domain));
        addRequests(types, checkAndStripFullyQualifyName(domain));

        messagesSentProperty = new SimpleIntegerProperty();
        durationProperty = new SimpleDoubleProperty();

        requestProperty = new SimpleObjectProperty<TreeItem<String>>();
        responseProperty = new SimpleObjectProperty<TreeItem<String>>();

        // this.resolverIP = resolverIP;
        this.transport_protocol = transport_protocol;
        this.application_protocol = application_protocol;
        if (application_protocol != APPLICATION_PROTOCOL.DOH) {
            this.ip = InetAddress.getByName(resolverIP);
        }
        this.resolver = resolverIP;
        this.receiveReply = new byte[1232];
        this.doFlag = doFlag;
        this.adFlag = adFlag;
        this.qcountTypes = types;
        this.domainAsString = domain;
        setMessagesSentProperty(0);

        this.wasSend = false;
        interfaceToSend = netInterface;
    }*/
    public DNSTaskBase(RequestSettings requestSettings, ConnectionSettings connectionSettings, RESPONSE_MDNS_TYPE mdnsType) throws UnsupportedEncodingException, NotValidIPException, NotValidDomainNameException, UnknownHostException {
        super();

        this.mdnsType = mdnsType;
        requests = new ArrayList<Request>();
        header = requestSettings.getHeader();
        size = Header.getSize();
        // TODO addRequests(types, checkAndStripFullyQualifyName(domain));
        addRequests(requestSettings.getTypes(), checkAndStripFullyQualifyName(requestSettings.getDomain()));

        messagesSentProperty = new SimpleIntegerProperty();
        durationProperty = new SimpleDoubleProperty();

        requestProperty = new SimpleObjectProperty<TreeItem<String>>();
        responseProperty = new SimpleObjectProperty<TreeItem<String>>();

        // this.resolverIP = resolverIP;
        this.transport_protocol = connectionSettings.getTransport_protocol();
        this.application_protocol = connectionSettings.getApplication_protocol();
        if (application_protocol != APPLICATION_PROTOCOL.DOH) {
            this.ip = InetAddress.getByName(connectionSettings.getResolverIP());
        }
        this.resolver = connectionSettings.getResolverIP();
        LOGGER.info("Resolver IP: " + connectionSettings.getResolverIP());
        this.receiveReply = new byte[1232];
        this.doFlag = requestSettings.isDoFlag();
        this.adFlag = requestSettings.isAdFlag();;
        this.qcountTypes = requestSettings.getTypes();
        this.domainAsString = requestSettings.getDomain();
        setMessagesSentProperty(0);

        this.wasSend = false;
        interfaceToSend = connectionSettings.getNetInterface();
    }

    /*
    * Method from Martin Biolek thesis
    * */
    protected void addRequests(Q_COUNT[] types, String domain)
            throws NotValidIPException, UnsupportedEncodingException, NotValidDomainNameException {
        for (Q_COUNT qcount : types) {
            Request r = new Request(domain, qcount);
            requests.add(r);
            size += r.getSize();
        }
    }

    /**
     * Method checks if parameter domain ends with <b>.</b>, thus it is considered as fully qualified domain name
     * and trailing <b>.</b> is removed.
     *
     * @param domain contains domain name string which can be trailed with <b>.</b>
     * @return string that is not ended with <b>.</b>
     */
    /*
     * Method from Martin Biolek thesis
     * */
    protected String checkAndStripFullyQualifyName(String domain) {
        if (domain.endsWith(".")) {
            return domain.substring(0, domain.length() - 1);
        } else {
            return domain;
        }
    }

    /**
     * Method converts DNS request query to bytes, so it can be sent via TCP or UDP socket
     */
    /*
     * Method from Martin Biolek thesis
     * */
    protected void messageToBytes() {
        int curentIndex = 0;
        if (doFlag) {
            size += new Response().getDnssecAsBytes().length;
        }
        if (transport_protocol == TRANSPORT_PROTOCOL.TCP) {

            this.messageAsBytes = new byte[size + 2];
            UInt16 tcpSize = new UInt16(size);
            curentIndex = 2;
            this.messageAsBytes[1] = tcpSize.getAsBytes()[0];
            this.messageAsBytes[0] = tcpSize.getAsBytes()[1];
        } else {
            this.messageAsBytes = new byte[size];
        }

        byte[] head = header.getHeaderAsBytes();
        for (int i = 0; i < head.length; i++) {
            this.messageAsBytes[curentIndex] = head[i];
            curentIndex++;
        }
        for (Request r : requests) {
            byte[] requestBytes = r.getRequestAsBytes();
            for (int i = 0; i < requestBytes.length; i++) {
                this.messageAsBytes[curentIndex] = requestBytes[i];
                curentIndex++;
            }
        }
        byte[] opt = new Response().getDnssecAsBytes();
        int j = 0;
        for (int i = curentIndex; i < size; i++) {
            this.messageAsBytes[i] = opt[j];
            j++;
        }
        byteSizeQuery = messageAsBytes.length;

    }

    /**
     * Method returns DNS response in form of TreeItem which can be used do display response in TreeView
     *
     * @return Response in form of tree consisting of TreeItem objects
     */
    /*
     * Method from Martin Biolek thesis
     * */
    public TreeItem<String> getAsTreeItem() {
        root = new TreeItem<String>(KEY_REQUEST);
        root.getChildren().add(header.getAsTreeItem());
        addRequestToTreeItem(); //11111111
        // OPT in DNS
        if (doFlag && mdnsType == null) {
            TreeItem<String> optRecord = new TreeItem<String>(MessageParser.KEY_ADDITIONAL_RECORDS);
            optRecord.getChildren().add(Response.getOptAsTreeItem(true, false));
            root.getChildren().add(optRecord);
        }
        if (mdnsType != null) {
            TreeItem<String> optRecord = new TreeItem<String>(MessageParser.KEY_ADDITIONAL_RECORDS);
            if (doFlag){
                optRecord.getChildren().add(Response.getOptAsTreeItem(doFlag, true));
                root.getChildren().add(optRecord);
            }
        }
        if (transport_protocol == TRANSPORT_PROTOCOL.TCP) {
            TreeItem<String> tcpTreeItem = new TreeItem<String>("");
            tcpTreeItem.getChildren().add(new TreeItem<String>(KEY_LENGHT + ": " + (byteSizeQuery - 2)));
            tcpTreeItem.getChildren().add(root);
            return tcpTreeItem;

        }
        return root;
    }

    /*
     * Method from Martin Biolek thesis
     * */
    private void addRequestToTreeItem() {
        TreeItem<String> subRequest = new TreeItem<String>(KEY_QUERY);
        if (!requests.isEmpty()) {
            for (Request request : requests) {
                subRequest.getChildren().add(request.getAsTreeItem());
            }
            root.getChildren().add(subRequest);
        }
    }

    /**
     * Method to be overriden in subclasses with implementation of protocol specific way of sending data
     * @throws TimeoutException
     * @throws MessageTooBigForUDPException
     * @throws InterfaceDoesNotHaveIPAddressException
     * @throws IOException
     * @throws InterruptedException
     * @throws ParseException
     * @throws HttpCodeException
     * @throws OtherHttpException
     * @throws NotValidDomainNameException
     * @throws NotValidIPException
     * @throws URISyntaxException
     * @throws NoSuchAlgorithmException
     * @throws QueryIdNotMatchException
     */
    protected abstract void sendData() throws TimeoutException, MessageTooBigForUDPException, InterfaceDoesNotHaveIPAddressException, IOException, InterruptedException, ParseException, HttpCodeException, OtherHttpException, NotValidDomainNameException, NotValidIPException, URISyntaxException, NoSuchAlgorithmException, QueryIdNotMatchException, ExecutionException, KeyStoreException, IllegalArgumentException, CancellationException;

    protected abstract void updateProgressUI();

    protected abstract void updateResultUI();

    @Override
    protected Void call(){
        try {
            // transform message to bytes array
            long start = System.nanoTime();
            setTaskProgress(0.1);
            messageToBytes();
            setTaskProgress(0.2);
            root = new TreeItem<>();
            addRequestToTreeItem();

            // send data over network
            // method will be specified in the implementation of method
            sendData();
            //Thread.sleep(500);
            //System.out.println("Taskbase " + recieveReply.length);
            LOGGER.info("Taskbase " + receiveReply.length);
            setTaskProgress(0.8);
            parser = parseResponse();
            // parse received message
            GeneralController.requestResponseMap.put("response", parser.getAsJsonString());
            GeneralController.requestResponseMap.put("request", getAsJsonString());
            // store request and response as TreeItem<String> so it can be passed to GUI
            setRequestAndResponse(parser);

            setByteSizeResponse(parser.getByteSizeResponse());
            double h = (System.nanoTime() - start) / 1000000.00;
            h = Math.round(h * 100) / 100.0;
            if (h < 1000) {
                Thread.sleep((long) ((1000 - duration)));
            }
            // update UI with results
            updateResultUI();
        } catch (InterruptedException e) {
            if(!massTesting)
            {
                Platform.runLater(() -> {
                    controller.getSendButton().setText(controller.getButtonText());
                    controller.getProgressBar().setProgress(0);
                    //controller.showAlert(e.getClass().getSimpleName());
                    controller.showAlert(e);
                });
            }
            cleanup();
            LOGGER.info("DNSTaskBase got interrupted");
        } catch (URISyntaxException
                 | QueryIdNotMatchException
                 | TimeoutException
                 | NoSuchAlgorithmException
                 | MessageTooBigForUDPException
                 | InterfaceDoesNotHaveIPAddressException
                 | IOException
                 | ParseException
                 | HttpCodeException
                 | OtherHttpException
                 | NotValidDomainNameException
                 | NotValidIPException
                 | KeyStoreException
                 | IllegalArgumentException
                 | ExecutionException
                 | CancellationException e){
            Platform.runLater(()-> {
                controller.getSendButton().setText(controller.getButtonText());
                controller.getProgressBar().setProgress(0);
                controller.showAlert(e);
                //controller.showAlert(e.getClass().getSimpleName()); // Exception
                //LOGGER.severe(e.getClass().getSimpleName());
                //LOGGER.severe(e.getCause().getClass().getSimpleName());
            });
            //e.printStackTrace();
        /*
        }catch (ExecutionException e){
            // 240979: Had to split Execution exception, because this can be caused by many things, so I opted for raising error from the cause
            Platform.runLater(()-> {
                controller.getSendButton().setText(controller.getButtonText());
                controller.getProgressBar().setProgress(0);
                controller.showAlert(e.getCause().getClass().getSimpleName()); // Cause
                LOGGER.severe(e.getClass().getSimpleName());
                LOGGER.severe(e.getCause().getClass().getSimpleName());
            });*/
        }
        return null;

    }

    protected void setTaskProgress(double progress) {
        //progressBar.setProgress(progress);
    }

    protected void cleanup() {
        DNSTaskBase.terminateAllTcpConnections();
    }

    protected void setRequestAndResponse(MessageParser parser) {
        setResponse(parser.getAsTreeItem());
        setRequest(getAsTreeItem());
    }

    protected MessageParser parseResponse() throws QueryIdNotMatchException, UnknownHostException, UnsupportedEncodingException {
        parser = new MessageParser(getReceiveReply(), getHeader(), getTransport_protocol());
        parser.parse();
        return parser;
    }

    /**
     * Method used to calculate duration of DNS request
     *
     * @return Duration of DNS request
     */
    protected double calculateDuration() {
        double h = (getStopTime() - getStartTime()) / 1000000.00;
        h = Math.round(h * 100) / 100.0;
        return h;
    }

    /*
     * Method from Martin Biolek thesis
     * */
    public JSONObject getAsJson() {
        JSONObject jsonObject = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        jsonObject.put(KEY_HEAD, header.getAsJson());

        for (Request request : requests) {
            jsonArray.add(request.getAsJson());
        }
        jsonObject.put(KEY_QUERY, jsonArray);
        if (transport_protocol == TRANSPORT_PROTOCOL.TCP)
            jsonObject.put(KEY_LENGHT, (byteSizeQuery - 2));

        // opt record
        if (header.getArCount().getValue() == 1) {
            boolean mdns = (mdnsType != null);
            if (mdns)
                jsonObject.put(KEY_ADDITIONAL_RECORDS, Response.getOptRequestAsJson(mdnsDnssecSignatures, mdns));
            else
                jsonObject.put(KEY_ADDITIONAL_RECORDS, Response.getOptRequestAsJson(true, mdns));
        }
        return jsonObject;
    }

    /*
     * Method from Martin Biolek thesis
     * */
    public String getAsJsonString() {
        return new GsonBuilder().setPrettyPrinting().create().toJson(getAsJson());
    }

}
