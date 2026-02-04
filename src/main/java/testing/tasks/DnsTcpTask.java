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
import lombok.Getter;
import lombok.Setter;
import models.ConnectionSettings;
import models.Header;
import models.RequestSettings;
import models.UInt16;
import org.apache.commons.lang.exception.ExceptionUtils;
import tasks.DNSOverTCPTask;
import tasks.DNSTaskBase;
import testing.Result;
import ui.TesterController;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.List;
import java.util.logging.Logger;

/**
 * Class sends multiple requests to given server via specific protocol using method sendData()
 * from super class
 */
@Getter
@Setter
public class DnsTcpTask extends DNSOverTCPTask {
    private Result result;
    private int numberOfRequests;
    private long cooldown;
    private int i;
    public static Logger LOGGER = Logger.getLogger(DnsTcpTask.class.getName());
/*
    public DnsTcpTask(boolean recursion, boolean adFlag, boolean caFlag, boolean doFlag, boolean holdConnection,
                      String domain, Q_COUNT[] types, TRANSPORT_PROTOCOL transport_protocol,
                      APPLICATION_PROTOCOL application_protocol, String resolverIP, NetworkInterface netInterface,
                      Result result, int numberOfRequests, long cooldown) throws IOException, NotValidIPException, NotValidDomainNameException {
        super(recursion, adFlag, caFlag, doFlag, holdConnection, domain, types, transport_protocol,
                application_protocol, resolverIP, netInterface);
        this.result = result;
        this.numberOfRequests = numberOfRequests;
        this.cooldown = cooldown;

        LOGGER.info("Created DnsTcpTask for " + resolverIP);
    } */
    public DnsTcpTask(RequestSettings requestSettings, ConnectionSettings connectionSettings,
                      Result result, int numberOfRequests, long cooldown) throws IOException, NotValidIPException, NotValidDomainNameException {
        super(requestSettings, connectionSettings);
        this.result = result;
        this.numberOfRequests = numberOfRequests;
        this.cooldown = cooldown;

        LOGGER.info("Created DnsUdpTask for "+connectionSettings.getResolverIP());
    }

    @Override
    protected void sendData() {
        try {
            UInt16 generator = new UInt16();
            for (i = 0; i < numberOfRequests; i++) {
                try {
                    LOGGER.info("Sending data to server via TCP: " + i);
                    requests.clear();
                    header.setId(generator.generateRandom());
                    setSize(Header.getSize());
                    addRequests(qcountTypes, checkAndStripFullyQualifyName(domainAsString));
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
                    if (!holdConnection) {
                        if (DNSTaskBase.getTcpConnectionForServer(resolver) != null) {
                            try {
                                DNSTaskBase.getTcpConnectionForServer(resolver).closeAll();
                            } catch (IOException e) {
                                //e.printStackTrace();
                                LOGGER.severe(ExceptionUtils.getStackTrace(e));
                            }
                            DNSTaskBase.getTcp().remove(resolver);
                        }
                    }
                    Platform.runLater(() -> ((TesterController) controller).getResultsTableView().refresh());
                    Thread.sleep(cooldown);
                } catch (NotValidIPException | UnsupportedEncodingException |
                        NotValidDomainNameException | TimeoutException |
                        QueryIdNotMatchException | UnknownHostException e){
                    result.getExceptions().add(e);
                    result.getSuccess().add(false);
                }
            }
            if (DNSTaskBase.getTcpConnectionForServer(resolver) != null) {
                try {
                    DNSTaskBase.getTcpConnectionForServer(resolver).closeAll();
                } catch (IOException e) {
                    //e.printStackTrace();
                    LOGGER.severe(ExceptionUtils.getStackTrace(e));
                }
                DNSTaskBase.getTcp().remove(resolver);
            }
        } catch (InterruptedException e){
            DNSTaskBase.terminateAllTcpConnections();
            LOGGER.info("DnsTcpTask interrupted");
        }
    }

    @Override
    protected void updateProgressUI() {
    }

    @Override
    protected void updateResultUI() {
        LOGGER.info("Calculated duration to be stored " + calculateDuration());
        result.getDurations().add(calculateDuration());
        LOGGER.info("Finished run of DnsTcpTask for " + getResolver() + " with duration " + result.getDurations().get(i));
    }
}
