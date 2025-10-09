package tasks;
/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * Methods used from Martin Biolek thesis are marked with comment
 * */
import enums.APPLICATION_PROTOCOL;
import enums.Q_COUNT;
import enums.RESPONSE_MDNS_TYPE;
import enums.TRANSPORT_PROTOCOL;
import exceptions.*;
import javafx.application.Platform;
import models.MessageParser;
import models.Request;
import models.Response;
import org.apache.commons.lang.exception.ExceptionUtils;
import tasks.runnables.ProgressUpdateRunnable;
import tasks.runnables.RequestResultsUpdateRunnable;

import java.io.UnsupportedEncodingException;
import java.net.*;
/**
 * Class representing protocol multicast DNS
 */
public class DNSOverMulticastTask extends DNSTaskBase {
    private static final String IPv4_MDNS = "224.0.0.251";
    private static final String IPv6_MDNS = "ff02::fb";
    //private int messagesSent;
    private boolean multicast;
    private boolean isIPv4;
    private boolean run=true;
    MulticastSocket socket;

    public DNSOverMulticastTask(boolean multicast, boolean adFlag, boolean caFlag, boolean doFlag, String domain,
                                Q_COUNT[] types, TRANSPORT_PROTOCOL transport_protocol,
                                APPLICATION_PROTOCOL application_protocol, String resolverIP,
                                NetworkInterface netInterface, boolean
                                       isIPv4, RESPONSE_MDNS_TYPE mdnsType) throws UnsupportedEncodingException, NotValidIPException,
            NotValidDomainNameException,
            UnknownHostException {
        super(false, adFlag, caFlag, doFlag, domain, types, transport_protocol, application_protocol,
                resolverIP, netInterface, mdnsType);
        this.multicast = multicast;
        this.isIPv4 = isIPv4;
    }

    /*
     * Body of method taken from Martin Biolek thesis
     * */
    @Override
    protected void messageToBytes() {
        int curentIndex = 0;
        size += Response.getDnssecAsBytesMDNS(isDoFlag()).length;
        this.messageAsBytes = new byte[size];
        byte head[] = header.getHaderAsBytes();
        for (int i = 0; i < head.length; i++) {
            this.messageAsBytes[curentIndex] = head[i];
            curentIndex++;
        }
        for (Request r : requests) {
            byte requestBytes[] = r.getRequestAsBytes();
            for (int i = 0; i < requestBytes.length; i++) {
                this.messageAsBytes[curentIndex] = requestBytes[i];
                curentIndex++;
            }
        }
        byte opt[] = new Response().getDnssecAsBytesMDNS(doFlag);
        int j = 0;
        for (int i = curentIndex; i < size; i++) {
            this.messageAsBytes[i] = opt[j];
            j++;
        }
        byteSizeQuery = messageAsBytes.length;
    }

    /*
     * Body of method taken from Martin Biolek thesis
     * */
    @Override
    protected void addRequests(Q_COUNT[] types, String domain)
            throws NotValidIPException, UnsupportedEncodingException, NotValidDomainNameException {
        for (Q_COUNT qcount : types) {
            Request r = new Request(domain, qcount,mdnsType);
            requests.add(r);
            size += r.getSize();
        }
    }

    /*
     * Body of method taken from Martin Biolek thesis
     * */
    private InetAddress getGroup() throws UnknownHostException {
        if (isIPv4) {
            return InetAddress.getByName(IPv4_MDNS);
        } else {
            return InetAddress.getByName(IPv6_MDNS);
        }
    }

    /*
     * Body of method taken from Martin Biolek thesis
     * */
    private void isResponse() throws SocketTimeoutException {
        if (!(Byte.toUnsignedInt(recieveReply[2]) > 128)) {
            LOGGER.warning("Not a response message");
            throw new SocketTimeoutException("Not a response message Exception");
        }
    }

    /*
     * Body of method taken from Martin Biolek thesis and modified
     * */
    @Override
    protected void sendData() throws TimeoutException, MessageTooBigForUDPException,
            InterfaceDoesNotHaveIPAddressException, UnknownHostException, BindException, InterruptedException {

            long startTime;
            setMessagesSent(1);

            InetAddress group = getGroup();
            while (run) {
                try {
                    socket = mdnsType == RESPONSE_MDNS_TYPE.RESPONSE_UNICAST ? new MulticastSocket():new MulticastSocket(MDNS_PORT);
                    socket.setTimeToLive(1);
                    socket.setNetworkInterface(interfaceToSend);
                    socket.joinGroup(group);
                    DatagramPacket datagramPacket = new DatagramPacket(getMessageAsBytes(), getMessageAsBytes().length, group,
                            MDNS_PORT);
                    startTime = System.nanoTime();
                    setStartTime(startTime);
                    socket.setSoTimeout(TIME_OUT_MILLIS);
                    DatagramPacket receivePacket = new DatagramPacket(getRecieveReply(), getRecieveReply()
                            .length);

                    if (mdnsType == RESPONSE_MDNS_TYPE.RESPONSE_UNICAST) {
                        socket.send(datagramPacket);
                        wasSend = true;
                        socket.leaveGroup(group);
                        try {
                            socket.receive(receivePacket);
                        } catch (SocketException e){
                            socket.receive(receivePacket);
                        }
                        System.out.println("Received packet");
                        stopTime = System.nanoTime();
                        setStopTime(stopTime);
                        setDuration(calculateDuration());
                        socket.close();
                        return;
                    } else {
                        socket.send(datagramPacket);
                        wasSend = true;
                        socket.receive(receivePacket);
                        socket.receive(receivePacket);
                        isResponse();
                        stopTime = System.nanoTime();
                        setStopTime(stopTime);
                        setDuration(calculateDuration());
                        socket.leaveGroup(group);
                        socket.close();
                        return;
                    }

                } catch (BindException e) {
                    throw new BindException();
                }
                catch (Exception e) {
                    if (!run){
                        throw new InterruptedException();
                    }
                    //e.printStackTrace();
                    //LOGGER.info("Caught exception");
                    //e.printStackTrace();
                    LOGGER.severe(ExceptionUtils.getStackTrace(e));
                    //updateProgress(messagesSent, 3);
                    if (getMessagesSent() < 3) {
                        setMessagesSent(getMessagesSent()+1);
                        updateProgressUI();
                    } else {
                        setMessagesSent(getMessagesSent());
                        stopTime = System.nanoTime();
                        setStopTime(stopTime);
                        setDuration(9999);
                        updateProgressUI();
                        throw new TimeoutException();
                    }
                }
                updateProgressUI();
            }
    }

    @Override
    protected void updateProgressUI() {
        Platform.runLater(new ProgressUpdateRunnable(this));
    }

    @Override
    protected void updateResultUI() {
        Platform.runLater(new RequestResultsUpdateRunnable(this));
    }

    /**
     * Creates Message Parser and parses mDNS response
     * @return Message Parser containing parsed mDNS response
     * @throws QueryIdNotMatchException
     * @throws UnknownHostException
     * @throws UnsupportedEncodingException
     */
    @Override
    protected MessageParser parseResponse() throws QueryIdNotMatchException, UnknownHostException, UnsupportedEncodingException {
        MessageParser parser = new MessageParser(getRecieveReply(), getHeader(), getTransport_protocol());
        parser.parseMDNS();
        return parser;
    }

    @Override
    public void stopExecution(){
        run=false;
        setResponseSizeProperty(0);
        setByteSizeResponse(0);
        setResponseProperty(null);
        setResponse(null);
        socket.close();

    }

}
