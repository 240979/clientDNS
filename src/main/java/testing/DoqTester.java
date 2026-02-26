package testing;

import javafx.concurrent.Task;
import lombok.Data;
import lombok.EqualsAndHashCode;
import models.ConnectionSettings;
import models.RequestSettings;
import org.apache.commons.lang.exception.ExceptionUtils;
import tasks.DNSTaskBase;
import testing.tasks.DnsDoqTask;
import ui.GeneralController;

import java.util.List;
import java.util.logging.Logger;

@EqualsAndHashCode(callSuper = true)
@Data
public class DoqTester extends Task<Void> {

    private static final Logger LOGGER = Logger.getLogger(DoqTester.class.getName());

    private List<Result> results;
    private int numberOfRequests;
    private long cooldown;
    private GeneralController controller;
    private RequestSettings requestSettings;
    private ConnectionSettings connectionSettings;

    public DoqTester(RequestSettings rs, ConnectionSettings cs, int numberOfRequests, List<Result> results, long cooldown) {
        this.results = results;
        this.numberOfRequests = numberOfRequests;
        this.cooldown = cooldown;
        this.requestSettings = rs;
        this.connectionSettings = cs;
    }

    @Override
    protected Void call() throws Exception {
        for (Result result : results) {
            LOGGER.info("DoqTester task started for " + result.getName());
            LOGGER.info("Starting DoqTester for " + result.getName());
            this.connectionSettings.setResolverIP(result.getIp());
            this.connectionSettings.setResolverPort(result.getPort());
            this.requestSettings.setDomain(result.getDomain());
            DNSTaskBase task = new DnsDoqTask(requestSettings, connectionSettings, result, numberOfRequests, cooldown);
            LOGGER.info("Setting resolver IP: " + result.getIp());
            LOGGER.info("Set resolver IP: " + this.connectionSettings.getResolverIP());
            LOGGER.info("Setting port: " + result.getPort());
            task.setMassTesting(true);
            task.setController(controller);
            Thread thread = new Thread(task);
            try {
                thread.start();
                thread.join();
            } catch (InterruptedException e) {
                thread.interrupt();
                results.forEach(r -> r.setComplete(true));
                LOGGER.severe(ExceptionUtils.getStackTrace(e));
                return null;
            }
            LOGGER.info("Ended DoqTester for " + result.getName());
            LOGGER.info("Setting complete to true");
            result.setComplete(true);
        }
        return null;
    }
}