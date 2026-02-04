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
import tasks.DNSOverTLS;
import testing.Result;
import ui.TesterController;

import javax.net.ssl.SSLException;
import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;

/**
 * Class sends multiple requests to given server via specific protocol using method sendData()
 * from super class
 */
public class DnsDotTask extends DNSOverTLS {

    private final Result result;
    private final int duration;
    private final long cooldown;
    private int i;

    public DnsDotTask(RequestSettings requestSettings, ConnectionSettings connectionSettings, Result result, int duration, long cooldown) throws UnknownHostException, NotValidDomainNameException, UnsupportedEncodingException, NotValidIPException {
        super(requestSettings, connectionSettings);
        this.result = result;
        this.duration = duration;
        this.cooldown = cooldown;
    }

    @Override
    protected void sendData() {
        try {
            UInt16 generator = new UInt16();
            for (i = 0; i < duration; i++){
                try{
                requests.clear();
                header.setId(generator.generateRandom());
                setSize(Header.getSize());
                addRequests(qcountTypes,checkAndStripFullyQualifyName(domainAsString));
                messageToBytes();
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
                Platform.runLater(()->((TesterController) controller).getResultsTableView().refresh());
                Thread.sleep(cooldown);
                } catch (NotValidIPException | UnsupportedEncodingException | NotValidDomainNameException | TimeoutException | SSLException | QueryIdNotMatchException | UnknownHostException e){
                    result.getExceptions().add(e);
                    result.getSuccess().add(false);
                }
            }
        } catch (InterruptedException e) {
            getChannel().close();
            LOGGER.severe(ExceptionUtils.getStackTrace(e));
        }
    }

    @Override
    protected void updateResultUI() {
        result.getDurations().add(calculateDuration());
        LOGGER.info("Finished run of DnsDotTask for "+getResolver()+" with duration "+result.getDurations().get(i));
    }
}
