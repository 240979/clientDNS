package testing.tasks;

import enums.R_CODE;
import exceptions.*;
import io.netty.channel.socket.ChannelOutputShutdownException;
import javafx.application.Platform;
import models.ConnectionSettings;
import models.Header;
import models.RequestSettings;
import models.UInt16;
import tasks.DNSOverQUICTask;
import testing.Result;
import ui.TesterController;

import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;


public class DnsDoqTask extends DNSOverQUICTask {

    private final Result result;
    private final int numberOfRequests;
    private final long cooldown;

    public DnsDoqTask(RequestSettings requestSettings, ConnectionSettings connectionSettings, Result result, int numberOfRequests, long cooldown) throws UnknownHostException, NotValidDomainNameException, UnsupportedEncodingException, NotValidIPException {
        super(requestSettings, connectionSettings);
        this.result = result;
        this.numberOfRequests = numberOfRequests;
        this.cooldown = cooldown;
    }

    @Override
    protected void sendData() {
        try {
            UInt16 generator = new UInt16();
            int i;
            for (i = 0; i < numberOfRequests; i++) {
                try {
                    requests.clear();
                    header.setId(generator.generateRandom());
                    setSize(Header.getSize());
                    addRequests(qcountTypes, checkAndStripFullyQualifyName(domainAsString));
                    messageToBytes();
                    super.sendData();
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
                         | UnsupportedEncodingException
                         | NotValidDomainNameException
                         | exceptions.TimeoutException
                         | QueryIdNotMatchException
                         | UnknownHostException
                         | KeyStoreException
                         | NoSuchAlgorithmException
                         | ExecutionException
                         | CancellationException
                         | ChannelOutputShutdownException
                         | TimeoutException
                         | IllegalArgumentException
                         | InterfaceDoesNotHaveIPAddressException e) {
                    result.getExceptions().add(e);
                    result.getSuccess().add(false);
                }
            }
        } catch (InterruptedException e) {
            LOGGER.info("DnsDoqTask interrupted");
        }
    }

    @Override
    protected void updateResultUI() {
        double duration = calculateDuration();
        result.getDurations().add(duration);
        LOGGER.info("Finished run of DnsDoqTask for " + getResolver() + " with duration " + duration);
    }
}