package testing.tasks;

/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * */
import exceptions.*;
import javafx.application.Platform;
import models.*;
import org.json.simple.JSONArray;
import org.json.simple.parser.ParseException;
import tasks.DNSOverHTTPSTask;
import testing.Result;
import ui.TesterController;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Class sends multiple requests to given server via specific protocol using method sendData()
 * from super class
 */
public class DnsDohTask extends DNSOverHTTPSTask {
    private final Result result;
    private final int numberOfRequests;
    private final long cooldown;

    public DnsDohTask(RequestSettings requestSettings, ConnectionSettings connectionSettings, Result result, int numberOfRequests, long cooldown) throws UnsupportedEncodingException, NotValidIPException, NotValidDomainNameException, UnknownHostException {
        super(requestSettings, connectionSettings);
        this.result = result;
        this.numberOfRequests = numberOfRequests;
        this.cooldown = cooldown;
    }

    @Override
    protected void sendData() {
        try {
            UInt16 generator = new UInt16();
            for (int i = 0; i < numberOfRequests; i++) {
                try {
                    // perform certain number of requests
                    exc = null;
                    requests.clear();
                    // prepare request
                    header.setId(generator.generateRandom());
                    setSize(Header.getSize());
                    addRequests(qcountTypes, checkAndStripFullyQualifyName(domainAsString));
                    messageToBytes();
                    // send request via super class method sendData()
                    super.sendData();
                    result.setResponseSize((byteSizeResponseDoHDecompressed));
                    parser = new MessageParser(httpResponse);
                    long status = (long) httpResponse.get("Status");
                    JSONArray answers = (JSONArray) httpResponse.get("Answer");
                    if (answers == null || answers.isEmpty() || status != 0) {
                        exc = new Exception();
                    }
                    LOGGER.info("Calculated duration to be stored " + calculateDuration());
                    // store duration of request
                    result.getDurations().add(getDuration());
                    if (exc != null) {
                        result.getSuccess().add(false);
                        result.getExceptions().add(exc);
                    } else {
                        List<Response> tmp = new LinkedList<>();
                        tmp.add(new Response(answers));
                        result.getResponses().add(tmp);
                        result.getSuccess().add(true);
                    }
                    Platform.runLater(() -> ((TesterController) controller).getResultsTableView().refresh());
                    // waiting between requests
                    Thread.sleep(cooldown);
                } catch (IOException
                         | NotValidIPException
                         | NotValidDomainNameException
                          | MessageTooBigForUDPException
                         | QueryIdNotMatchException
                         | InterfaceDoesNotHaveIPAddressException
                          | OtherHttpException
                         | ParseException
                          | HttpCodeException
                          | TimeoutException
                          | ExecutionException e) {
                         // | URISyntaxException e) {
                    result.getExceptions().add(e);
                    result.getSuccess().add(false);
                }
            }
        } catch (InterruptedException e){
            // handling of the Stop button action
            cleanup();
            LOGGER.info("DnsDohTask interrupted");
        }
    }

    @Override
    public void updateResultUI(){

    }
}
