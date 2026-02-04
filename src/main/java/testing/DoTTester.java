package testing;
/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * */
import javafx.concurrent.Task;
import lombok.Data;
import models.ConnectionSettings;
import models.RequestSettings;
import org.apache.commons.lang.exception.ExceptionUtils;
import tasks.DNSTaskBase;
import testing.tasks.DnsDotTask;
import ui.GeneralController;

import java.util.List;
import java.util.logging.Logger;
/**
 * Protocol specific task, which starts another task, which perform repeated querying on domain against given server
 * Sequentially for each domain task is started after previous has finished
 * */
@Data
public class DoTTester extends Task<Void> {

    private static final Logger LOGGER = Logger.getLogger(TcpTester.class.getName());

    private List<Result> results;
    private int duration;
    private long cooldown;
    private GeneralController controller;
    private RequestSettings requestSettings;
    private ConnectionSettings connectionSettings;

    public DoTTester(RequestSettings rs, ConnectionSettings cs, int duration, List<Result> results, long cooldown){
        this.results = results;
        this.duration = duration;
        this.cooldown = cooldown;
        this.requestSettings = rs;
        this.connectionSettings = cs;
    }

    @Override
    protected Void call() throws Exception {
        for (Result result: results){
            LOGGER.info("DotTester task started for "+result.getName());
            // create new thread, which will start given task, which runs DNS over TCP and returns duration of request
            // to given Double object which was passed inside
            LOGGER.info("Starting DotTester for "+result.getName());
            this.connectionSettings.setResolverIP(result.getIp());
            this.requestSettings.setDomain(result.getDomain());
            DNSTaskBase task = new DnsDotTask(requestSettings, connectionSettings, result,duration, cooldown);
            LOGGER.info("Setting resolver IP: " + result.getIp());
            LOGGER.info("Set resolver IP: " + this.connectionSettings.getResolverIP());
            task.setMassTesting(true);
            task.setController(controller);
            Thread thread = new Thread(task);
            try {
                thread.start();
                thread.join();
            }
            catch (InterruptedException e)
            {
                thread.interrupt();
                results.forEach(r -> r.setComplete(true));
               // e.printStackTrace();
                LOGGER.severe(ExceptionUtils.getStackTrace(e));
                return null;
            }
            LOGGER.info("Ended DotTester for "+result.getName());
            LOGGER.info("Setting complete to true");
            result.setComplete(true);
        }
        return null;
    }
}
