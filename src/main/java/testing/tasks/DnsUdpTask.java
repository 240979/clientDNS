package testing.tasks;
/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * */
import enums.APPLICATION_PROTOCOL;
import enums.Q_COUNT;
import enums.R_CODE;
import enums.TRANSPORT_PROTOCOL;
import exceptions.*;
import javafx.application.Platform;
import models.Header;
import models.UInt16;
import org.apache.commons.lang.exception.ExceptionUtils;
import tasks.DNSOverUDPTask;
import testing.Result;
import ui.TesterController;

import java.io.UnsupportedEncodingException;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.List;

/**
 * Class sends multiple requests to given server via specific protocol using method sendData()
 * from super class
 */
public class DnsUdpTask extends DNSOverUDPTask {

    Result result;
    int i;
    int duration;
    private long cooldown;

    public DnsUdpTask(boolean recursion, boolean adFlag, boolean caFlag, boolean doFlag,
                      String domain, Q_COUNT[] types, TRANSPORT_PROTOCOL transport_protocol,
                      APPLICATION_PROTOCOL application_protocol, String resolverIP,
                      NetworkInterface netInterface, Result result, int duration, long cooldown)
            throws UnknownHostException, NotValidDomainNameException, UnsupportedEncodingException,
            NotValidIPException {
        super(recursion,adFlag,caFlag,doFlag,domain,types,transport_protocol,application_protocol,
                resolverIP,netInterface);
        this.result = result;
        this.duration = duration;
        this.cooldown = cooldown;
        LOGGER.info("Created DnsUdpTask for "+resolverIP);
    }

    @Override
    protected void sendData() {
        try{
        UInt16 generator = new UInt16();
        for (i = 0; i < duration; i++){
            try {
                exc = null;
                LOGGER.info("Sending data to server via UDP: " + i);
                requests.clear();
                header.setId(generator.generateRandom());
                setSize(Header.getSize());
                addRequests(qcountTypes, checkAndStripFullyQualifyName(domainAsString));
                messageToBytes();
                run = true;
                super.sendData();
                parser = parseResponse();
                result.setResponseSize((parser.getByteSizeResponse()));
                if (parser.getHeader().getAnCount().getValue() == 0 || parser.getHeader().getRCode() != R_CODE.NO_ERROR) {
                    result.getSuccess().add(false);
                } else {
                    result.getResponses().add(parser.getAncountResponses());
                    result.getSuccess().add(true);
                }
                updateResultUI();
                Platform.runLater(() -> ((TesterController) controller).getResultsTableView().refresh());
                Thread.sleep(cooldown);
            } catch (TimeoutException | MessageTooBigForUDPException | InterfaceDoesNotHaveIPAddressException | NotValidDomainNameException | NotValidIPException | UnsupportedEncodingException | QueryIdNotMatchException | UnknownHostException e){
                result.getExceptions().add(e);
                result.getSuccess().add(false);
            }
        }}
        catch (InterruptedException e){
            cleanup();
            //e.printStackTrace();
            LOGGER.severe(ExceptionUtils.getStackTrace(e));
        }
    }

    @Override
    protected void updateProgressUI() {
    }

    @Override
    protected void updateResultUI() {
        LOGGER.info("Calculated duration to be stored "+calculateDuration());
        result.getDurations().add(calculateDuration());
        LOGGER.info("Finished run of DnsUdpTask for "+getResolver()+" with duration "+result.getDurations().get(i));
    }
}
