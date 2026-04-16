/*
* This is basically copy of DnsDohTask, nothing new here, just needed to change the inheritance
* */
package testing.tasks;

import exceptions.*;
import javafx.application.Platform;
import models.*;
import org.json.simple.JSONArray;
import org.json.simple.parser.ParseException;
import tasks.DNSOverHTTPS3Task;
import testing.Result;
import ui.TesterController;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class TestDoh3Task extends DNSOverHTTPS3Task {
    private final Result result;
    private final int numberOfRequests;
    private final long cooldown;

    public TestDoh3Task(RequestSettings requestSettings, ConnectionSettings connectionSettings, Result result, int numberOfRequests, long cooldown)
            throws UnsupportedEncodingException, NotValidIPException, NotValidDomainNameException, UnknownHostException {
        super(requestSettings, connectionSettings);
        this.result = result;
        this.numberOfRequests = numberOfRequests;
        this.cooldown = cooldown;
        this.massTesting = true;
    }

    @Override
    protected void sendData() {
        try {
            UInt16 generator = new UInt16();
            for (int i = 0; i < numberOfRequests; i++) {
                try {
                    exc = null;
                    requests.clear();
                    header.setId(generator.generateRandom());
                    setSize(Header.getSize());
                    addRequests(qcountTypes, checkAndStripFullyQualifyName(domainAsString));
                    messageToBytes();
                    resetLatch();
                    httpResponse = null;
                    super.sendData();
                    result.setResponseSize(byteSizeResponseDoHDecompressed);

                    if (httpResponse != null) {
                        parser = new MessageParser(httpResponse);
                        JSONArray answers = (JSONArray) httpResponse.get("Answer");
                        List<Response> tmp = new LinkedList<>();
                        tmp.add(new Response(answers));
                        result.getResponses().add(tmp);
                        result.getSuccess().add(true);
                    } else {
                        parser = parseResponse();
                        result.getResponses().add(parser.getAncountResponses());
                        result.getSuccess().add(true);
                    }
                    result.getDurations().add(getDuration());
                    Platform.runLater(() -> ((TesterController) controller).getResultsTableView().refresh());
                    Thread.sleep(cooldown);
                } catch (IOException
                         | NotValidIPException
                         | NotValidDomainNameException
                         | QueryIdNotMatchException
                         | InterfaceDoesNotHaveIPAddressException
                         | ParseException
                         | HttpCodeException
                         | TimeoutException
                         | NoSuchAlgorithmException
                         | KeyStoreException
                         | ExecutionException e) {
                    result.getExceptions().add(e);
                    result.getSuccess().add(false);
                    result.getDurations().add(getDuration());
                }
            }
        } catch (InterruptedException e) {
            cleanup();
            LOGGER.info("DnsDoH3Task interrupted");
        }
    }

    @Override
    public void updateResultUI() {
    }
}