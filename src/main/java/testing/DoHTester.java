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
import testing.tasks.DnsDohTask;
import ui.GeneralController;

import java.util.List;
import java.util.logging.Logger;
/**
* Protocol specific task, which starts another task, which perform repeated querying on domain against given server
* Sequentially for each domain task is started after previous has finished
* */
@Data
public class DoHTester extends Task<Void> {

    private int duration;
    private List<Result> results;
    private long cooldown;
    private static Logger LOGGER = Logger.getLogger(DoHTester.class.getName());
    private GeneralController controller;
    private RequestSettings requestSettings;
    private ConnectionSettings connectionSettings;

    public DoHTester(RequestSettings rs, ConnectionSettings cs,
                     int duration, List<Result> results, long cooldown){
        this.requestSettings = rs;
        this.connectionSettings = cs;
        this.duration = duration;
        this.results = results;
        this.cooldown = cooldown;
    }


    @Override
    protected Void call() throws Exception {
        // for each domain
        for (Result result: results){
            LOGGER.info("DoHTester task started for "+result.getName());
            // create new thread, which will start given task, which runs DNS over HTTPS and stores durations of requests
            // in given Result data structure
            LOGGER.info("Starting DoHTester for "+result.getName());

            // This settings must be altered here, because this parameters are changing
            this.connectionSettings.setResolverIP(result.getIp());
            this.connectionSettings.setPath(result.getNs().getPath());
            this.connectionSettings.setDomainNameUsed(false);
            this.connectionSettings.setGet(result.getNs().isGet());
            this.connectionSettings.setResolverUri(result.getNs().getDomainName());
            this.connectionSettings.setReqJsonFormat(true);

            this.requestSettings.setDomain(result.getDomain());
             DNSTaskBase task = new DnsDohTask(this.requestSettings, this.connectionSettings, result,duration,cooldown);

            task.setMassTesting(true);
            task.setController(controller);
            Thread thread = new Thread(task);
            try {
                thread.start();
                thread.join();
            }
            catch (InterruptedException e)
            {
                // handle interruption from Stop button
                LOGGER.info("DoHTester interrupted");
                thread.interrupt();
                results.forEach(r -> r.setComplete(true));
                LOGGER.warning(ExceptionUtils.getStackTrace(e));
                return null;
            }
            LOGGER.info("Ended DoHTester for "+result.getName());
            LOGGER.info("Setting complete to true");
            result.setComplete(true);

        }
        return null;
    }
}
