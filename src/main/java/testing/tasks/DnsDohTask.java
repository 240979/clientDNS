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
import io.netty.handler.codec.MessageAggregationException;
import javafx.application.Platform;
import models.Header;
import models.MessageParser;
import models.Response;
import models.UInt16;
import org.json.simple.JSONArray;
import org.json.simple.parser.ParseException;
import tasks.DNSOverHTTPSTask;
import testing.Result;
import ui.TesterController;

import javax.net.ssl.SSLPeerUnverifiedException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.UnsupportedEncodingException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;

/**
 * Class sends multiple requests to given server via specific protocol using method sendData()
 * from super class
 */
public class DnsDohTask extends DNSOverHTTPSTask {
    private Result result;
    private int numberOfRequests;
    private long cooldown;

    public DnsDohTask(boolean recursion, boolean adFlag, boolean cdFlag, boolean doFlag, String domain, Q_COUNT[] types, TRANSPORT_PROTOCOL transport_protocol, APPLICATION_PROTOCOL application_protocol, String resolverIP, NetworkInterface netInterface, Result result, int numberOfRequests, long cooldown) throws UnsupportedEncodingException, NotValidIPException, NotValidDomainNameException, UnknownHostException {
        super(recursion, adFlag, cdFlag, doFlag, domain, types, transport_protocol, application_protocol, resolverIP+"/"+result.getNs().getPath(), netInterface, result.getNs().isGet());
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
                    result.setResponseSize((byteSizeResponseDoHDecompresed));
                    parser = new MessageParser(httpResponse);
                    long status = (long) httpResponse.get("Status");
                    JSONArray answers = (JSONArray) httpResponse.get("Answer");
                    if (answers == null || answers.size() == 0 || status != 0) {
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
                } catch (IOException | NotValidIPException |
                        NotValidDomainNameException | MessageTooBigForUDPException |
                        QueryIdNotMatchException | InterfaceDoesNotHaveIPAddressException |
                        OtherHttpException | ParseException | HttpCodeException |
                        TimeoutException e) {
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
    protected void updateProgressUI() {
    }

    @Override
    public void updateResultUI(){

    }
}
