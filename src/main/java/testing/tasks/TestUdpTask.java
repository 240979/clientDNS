package testing.tasks;
/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * */
import enums.R_CODE;
import exceptions.*;
import javafx.application.Platform;
import models.ConnectionSettings;
import models.Header;
import models.RequestSettings;
import models.UInt16;
import org.apache.commons.lang.exception.ExceptionUtils;
import tasks.DNSOverUDPTask;
import testing.Result;
import ui.TesterController;

import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;

/**
 * Class sends multiple requests to given server via specific protocol using method sendData()
 * from super class
 */
public class TestUdpTask extends DNSOverUDPTask {

    Result result;
    int duration;
    private final long cooldown;

    public TestUdpTask(RequestSettings requestSettings, ConnectionSettings connectionSettings, Result result, int duration, long cooldown)
            throws UnknownHostException, NotValidDomainNameException, UnsupportedEncodingException, NotValidIPException {
        super(requestSettings, connectionSettings);
        this.result = result;
        this.duration = duration;
        this.cooldown = cooldown;
        LOGGER.info("Created DnsUdpTask for "+connectionSettings.getResolverIP());
    }
    @SuppressWarnings("BusyWait")
    @Override
    protected void sendData() {
        try{
        UInt16 generator = new UInt16();
        for (int i = 0; i < duration; i++){
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
                if (parser.getHeader().getRCode() != R_CODE.NO_ERROR) {
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
        double duration = calculateDuration();
        result.getDurations().add(duration);
        LOGGER.info("Finished run of DnsDoqTask for " + getResolver() + " with duration " + duration);
    }
}
