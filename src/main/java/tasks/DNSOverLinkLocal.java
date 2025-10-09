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
import models.LLMNRHeader;
import models.MessageParser;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.json.simple.parser.ParseException;
import tasks.runnables.ProgressUpdateRunnable;
import tasks.runnables.RequestResultsUpdateRunnable;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;

/**
 * Class representing protocol Link-Local Multicast Name Resolution
 */
public class DNSOverLinkLocal extends DNSTaskBase{

    private int messagesSent;
    private boolean run=true;
    MulticastSocket socket;

    public DNSOverLinkLocal(boolean recursion, boolean adFlag, boolean cdFlag, boolean doFlag, String domain, Q_COUNT[] types, TRANSPORT_PROTOCOL transport_protocol, APPLICATION_PROTOCOL application_protocol, String resolverIP, NetworkInterface netInterface) throws UnsupportedEncodingException, NotValidIPException, NotValidDomainNameException, UnknownHostException {
        super(recursion, adFlag, cdFlag, doFlag, domain, types, transport_protocol, application_protocol, resolverIP, netInterface, null);
        header = new LLMNRHeader(types.length,false,false);
    }

    /*
    * Similar to Martin Biolek MessageSender.mdns(), removed multicast response, fixed response over unicast and modified
    * to specification of LLMNR
    * */
    @Override
    protected void sendData() throws TimeoutException, MessageTooBigForUDPException, InterfaceDoesNotHaveIPAddressException, IOException, ParseException, HttpCodeException, OtherHttpException, InterruptedException {
        long startTime;
        setMessagesSent(1);

        InetAddress group = getGroup();
        while (run) {
            try {
                socket = new MulticastSocket();
                socket.setNetworkInterface(interfaceToSend);
                socket.joinGroup(group);
                socket.setTimeToLive(1);
                DatagramPacket datagramPacket = new DatagramPacket(getMessageAsBytes(), getMessageAsBytes().length, group,
                        LLMNR_PORT);
                startTime = System.nanoTime();
                setStartTime(startTime);
                socket.setSoTimeout(TIME_OUT_MILLIS);
                DatagramPacket receivePacket = new DatagramPacket(getRecieveReply(), getRecieveReply()
                        .length,socket.getLocalAddress(),socket.getLocalPort());
                socket.send(datagramPacket);
                // first receive to consume ICMP message
                try {
                    socket.receive(receivePacket);
                } catch (SocketException e)
                {
                    socket.receive(receivePacket);
                }

                wasSend = true;
                stopTime = System.nanoTime();
                setStopTime(stopTime);
                setDuration(calculateDuration());
                socket.close();
                if(!massTesting){
                    Platform.runLater(() -> controller.getSendButton().setText(controller.getButtonText()));
                }
                return;

            } catch (BindException e) {
                throw new BindException();
            }
            catch (Exception e) {
                //e.printStackTrace();
                LOGGER.severe(ExceptionUtils.getStackTrace(e));
                if (!run){
                    throw new InterruptedException();
                }
                if (getMessagesSent() < 3) {
                    setMessagesSent(getMessagesSent()+1);;
                    updateProgressUI();
                } else {
                    setMessagesSent(getMessagesSent());;
                    stopTime = System.nanoTime();
                    setStopTime(stopTime);
                    setDuration(9999);
                    updateProgressUI();
                    if (socket!= null){
                        socket.close();
                    }
                    throw new TimeoutException();
                }
            }
            updateProgressUI();
        }
    }

    private InetAddress getGroup() throws UnknownHostException {
            return InetAddress.getByName(resolver);
    }

    @Override
    protected void updateProgressUI(){
        Platform.runLater(new ProgressUpdateRunnable(this));
    }

    @Override
    protected void updateResultUI(){
        Platform.runLater(new RequestResultsUpdateRunnable(this));
    }

    /**
     * Creates Message Parser and parses LLMNR response
     * @return Message Parser containing parsed LLMNR response
     * @throws QueryIdNotMatchException
     * @throws UnknownHostException
     * @throws UnsupportedEncodingException
     */
    @Override
    protected MessageParser parseResponse() throws QueryIdNotMatchException, UnknownHostException, UnsupportedEncodingException {
        MessageParser parser = new MessageParser(getRecieveReply(),getHeader(),getTransport_protocol());
        parser.parseLLMNR();
        return parser;
    }

    @Override
    public void stopExecution() {
        run=false;
        setResponseSizeProperty(0);
        setByteSizeResponse(0);
        setResponseProperty(null);
        setResponse(null);
        socket.close();
    }
}
