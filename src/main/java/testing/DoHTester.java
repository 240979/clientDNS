package testing;
/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * */
import enums.APPLICATION_PROTOCOL;
import enums.Q_COUNT;
import enums.TRANSPORT_PROTOCOL;
import javafx.concurrent.Task;
import lombok.Data;
import org.apache.commons.lang.exception.ExceptionUtils;
import tasks.DNSTaskBase;
import testing.tasks.DnsDohTask;
import ui.GeneralController;

import java.net.NetworkInterface;
import java.util.List;
import java.util.logging.Logger;
/**
* Protocol specific task, which starts another task, which perform repeated querying on domain against given server
* Sequentially for each domain task is started after previous has finished
* */
@Data
public class DoHTester extends Task<Void> {

    private boolean recursion;
    private boolean adFlag;
    private boolean cdFlag;
    private boolean doFlag;
    private Q_COUNT[] types;
    private TRANSPORT_PROTOCOL transport_protocol;
    private APPLICATION_PROTOCOL application_protocol;
    private NetworkInterface netInterface;
    private int duration;
    private List<Result> results;
    private long cooldown;
    private static Logger LOGGER = Logger.getLogger(DoHTester.class.getName());
    private GeneralController controller;

    public DoHTester(boolean
                             recursion, boolean adFlag, boolean cdFlag, boolean doFlag,
                     Q_COUNT[] types, TRANSPORT_PROTOCOL transport_protocol,
                     APPLICATION_PROTOCOL application_protocol, NetworkInterface netInterface,
                     int duration, List<Result> results, long cooldown){

        this.recursion = recursion;
        this.adFlag = adFlag;
        this.cdFlag = cdFlag;
        this.doFlag = doFlag;
        this.types = types;
        this.transport_protocol = transport_protocol;
        this.application_protocol = application_protocol;
        this.netInterface = netInterface;
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
            DNSTaskBase task = new DnsDohTask(recursion,adFlag,cdFlag,doFlag,result.getDomain(),types,
                    TRANSPORT_PROTOCOL.TCP,APPLICATION_PROTOCOL.DOH,
                    result.getIp(),result.getNs().getDomainName() +"/"+ result.getNs().getPath(), netInterface,result,duration,cooldown);
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
                //e.printStackTrace();
                LOGGER.severe(ExceptionUtils.getStackTrace(e));
                return null;
            }
            LOGGER.info("Ended DoHTester for "+result.getName());
            LOGGER.info("Setting complete to true");
            result.setComplete(true);

        }
        return null;
    }
}
