package testing.tasks;
/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * */
import enums.R_CODE;
import exceptions.*;
import javafx.application.Platform;
import lombok.Getter;
import lombok.Setter;
import models.*;
import org.apache.commons.lang.exception.ExceptionUtils;
import tasks.DNSOverTCPTask;
import testing.Result;
import ui.TesterController;

import java.io.IOException;
import java.net.InetAddress;
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
    private TCPConnection localConnection;
    public static Logger LOGGER = Logger.getLogger(DnsTcpTask.class.getName());

    public DnsTcpTask(RequestSettings requestSettings, ConnectionSettings connectionSettings,
                      Result result, int numberOfRequests, long cooldown) throws IOException, NotValidIPException, NotValidDomainNameException {
        super(requestSettings, connectionSettings);
        this.result = result;
        this.numberOfRequests = numberOfRequests;
        this.cooldown = cooldown;
        LOGGER.info("Created DnsUdpTask for " + connectionSettings.getResolverIP());
    }
    @SuppressWarnings("BusyWait")
    @Override
    protected void sendData() {
        try {
            UInt16 generator = new UInt16();
            for (int i = 0; i < numberOfRequests; i++) {
                try {
                    LOGGER.info("Sending data to server via TCP: " + i);
                    requests.clear();
                    header.setId(generator.generateRandom());
                    setSize(Header.getSize());
                    addRequests(qcountTypes, checkAndStripFullyQualifyName(domainAsString));
                    messageToBytes();

                    // use local connection instead of shared static map
                    if (localConnection == null || localConnection.isClosed()) {
                        localConnection = new TCPConnection(InetAddress.getByName(resolver));
                    }
                    setStartTime(System.nanoTime());
                    setReceiveReply(localConnection.send(getMessageAsBytes(), getIp(), false, getInterfaceToSend()));
                    setStopTime(System.nanoTime());
                    setWasSend(true);

                    parser = parseResponse();
                    result.setResponseSize(parser.getByteSizeResponse());
                    if (parser.getHeader().getAnCount().getValue() == 0 || parser.getHeader().getRCode() != R_CODE.NO_ERROR) {
                        result.getSuccess().add(false);
                    } else {
                        result.getResponses().add(parser.getAncountResponses());
                        result.getSuccess().add(true);
                    }
                    updateResultUI();
                    Platform.runLater(() -> ((TesterController) controller).getResultsTableView().refresh());
                    Thread.sleep(cooldown);
                } catch (NotValidIPException
                         | NotValidDomainNameException
                         | TimeoutException
                         | QueryIdNotMatchException
                         | InterfaceDoesNotHaveIPAddressException
                         | CouldNotUseHoldConnectionException
                         | IOException e) {
                    result.getExceptions().add(e);
                    result.getSuccess().add(false);
                }
            }
        } catch (InterruptedException e) {
            LOGGER.info("DnsTcpTask interrupted");
        } finally {
            // always close the local connection when done
            if (localConnection != null && !localConnection.isClosed()) {
                try {
                    localConnection.closeAll();
                } catch (IOException e) {
                    LOGGER.severe(ExceptionUtils.getStackTrace(e));
                }
            }
        }
    }

    @Override
    protected void updateResultUI() {
        double duration = calculateDuration();
        result.getDurations().add(duration);
        LOGGER.info("Finished run of DnsDoqTask for " + getResolver() + " with duration " + duration);
    }
}