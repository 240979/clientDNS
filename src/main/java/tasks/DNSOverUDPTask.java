package tasks;
/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * Methods used from Martin Biolek thesis are marked with comment
 * */
import application.Config;
import enums.APPLICATION_PROTOCOL;
import enums.Q_COUNT;
import enums.TRANSPORT_PROTOCOL;
import exceptions.*;
import javafx.application.Platform;
import lombok.Getter;
import lombok.Setter;
import models.Ip;
import org.apache.commons.lang.exception.ExceptionUtils;
import tasks.runnables.ProgressUpdateRunnable;
import tasks.runnables.RequestResultsUpdateRunnable;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.UnsupportedEncodingException;
import java.net.*;

/**
 * Class representing protocol DNS using UDP connection
 */
@Getter
@Setter
public class DNSOverUDPTask extends DNSTaskBase{

    protected DatagramSocket datagramSocket;
    protected boolean run = true;

    public DNSOverUDPTask(boolean recursion, boolean adFlag, boolean caFlag, boolean doFlag, String domain, Q_COUNT[] types, TRANSPORT_PROTOCOL transport_protocol, APPLICATION_PROTOCOL application_protocol, String resolverIP, NetworkInterface netInterface) throws UnsupportedEncodingException, NotValidIPException, NotValidDomainNameException, UnknownHostException {
        super(recursion, adFlag, caFlag, doFlag, domain, types, transport_protocol, application_protocol, resolverIP, netInterface, null);
    }

    /*
    * Body of method taken from Martin Biolek thesis and modified
    * */
    @Override
    protected void sendData() throws TimeoutException, MessageTooBigForUDPException, InterfaceDoesNotHaveIPAddressException, NotValidDomainNameException, NotValidIPException, UnsupportedEncodingException, InterruptedException, QueryIdNotMatchException, UnknownHostException {
        if (getSize() > MAX_UDP_SIZE){
            exc = new MessageTooBigForUDPException();
            throw new MessageTooBigForUDPException();
        }
        setMessagesSent(0);

        try {
            datagramSocket = new DatagramSocket(0, Ip.getIpAddressFromInterface(getInterfaceToSend(), getResolver()));
        } catch (Exception e) {
            exc = new InterfaceDoesNotHaveIPAddressException();
            throw new InterfaceDoesNotHaveIPAddressException();
        }

        boolean exception = false;
        boolean timeout = false;
        while (run) {
            try {
                if (getMessagesSent() == Config.getGeneralConfig().getMaxMessagesSent()) {
                    exc = new TimeoutException();
                    throw new TimeoutException();
                }

                DatagramPacket responsePacket = new DatagramPacket(getRecieveReply(), getRecieveReply().length);
                DatagramPacket datagramPacket = new DatagramPacket(getMessageAsBytes(), getMessageAsBytes().length, getIp(), DNSTaskBase.DNS_PORT);
                // DatagramPacket datagramPacket = new Data
                datagramSocket.setSoTimeout(DNSTaskBase.TIME_OUT_MILLIS);
                setStartTime(System.nanoTime());

                datagramSocket.send(datagramPacket);
                setWasSend(true);
                datagramSocket.receive(responsePacket);

                setStopTime(System.nanoTime());
                datagramSocket.close();
                run = false;
            } catch (SocketTimeoutException | SocketException e) {
                //e.printStackTrace();
                LOGGER.severe(ExceptionUtils.getStackTrace(e));
                LOGGER.warning("Time out for the: " + (getMessagesSent() + 1) + " message");
                timeout = true;
                if (getMessagesSent() == Config.getGeneralConfig().getMaxMessagesSent()) {
                    datagramSocket.close();
                }
            } catch(InterruptedIOException e){
                //e.printStackTrace();
                LOGGER.severe(ExceptionUtils.getStackTrace(e));
                throw new InterruptedException();
            }
            catch (IOException e) {
                //e.printStackTrace();
                LOGGER.severe(ExceptionUtils.getStackTrace(e));
            }
            setMessagesSent(getMessagesSent()+1);
            if (timeout)
            {
                timeout = false;
                setDuration(9999);
            }
            else
            {
                setDuration(calculateDuration());
            }
            updateProgressUI();
        }
        if (timeout) {
            exc = new TimeoutException();
            throw new TimeoutException();
        }
    }

    @Override
    protected void updateProgressUI()
    {
        Platform.runLater(new ProgressUpdateRunnable(this));
    }

    @Override
    protected void updateResultUI()
    {
        Platform.runLater(new RequestResultsUpdateRunnable(this));
    }

    @Override
    public void stopExecution(){
        run=false;
        datagramSocket.close();
    }

}
