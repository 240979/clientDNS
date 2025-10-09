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
import testing.tasks.DnsDotTask;
import ui.GeneralController;

import java.net.NetworkInterface;
import java.util.List;
import java.util.logging.Logger;
/**
 * Protocol specific task, which starts another task, which perform repeated querying on domain against given server
 * Sequentially for each domain task is started after previous has finished
 * */
@Data
public class DoTTester extends Task<Void> {

    private static final Logger LOGGER = Logger.getLogger(TcpTester.class.getName());
    private boolean recursion;
    private boolean adFlag;
    private boolean caFlag;
    private boolean doFlag;
    private Q_COUNT[] types;
    private TRANSPORT_PROTOCOL transport_protocol;
    private APPLICATION_PROTOCOL application_protocol;
    private NetworkInterface netInterface;
    private List<Result> results;
    private int duration;
    private long cooldown;
    private GeneralController controller;

    public DoTTester(boolean recursion, boolean adFlag, boolean caFlag, boolean doFlag,
                     Q_COUNT[] types, TRANSPORT_PROTOCOL transport_protocol,
                     APPLICATION_PROTOCOL application_protocol,
                     NetworkInterface netInterface, List<Result> results, int duration, long cooldown){

        this.recursion = recursion;
        this.adFlag = adFlag;
        this.caFlag = caFlag;
        this.doFlag = doFlag;
        this.types = types;
        this.transport_protocol = transport_protocol;
        this.application_protocol = application_protocol;
        this.netInterface = netInterface;
        this.results = results;
        this.duration = duration;
        this.cooldown = cooldown;
    }

    @Override
    protected Void call() throws Exception {
        for (Result result: results){
            LOGGER.info("DotTester task started for "+result.getName());
            // create new thread, which will start given task, which runs DNS over TCP and returns duration of request
            // to given Double object which was passed inside
            LOGGER.info("Starting DotTester for "+result.getName());
            DNSTaskBase task = new DnsDotTask(recursion,adFlag,caFlag,doFlag,result.getDomain(),types,
                    TRANSPORT_PROTOCOL.TCP, APPLICATION_PROTOCOL.DNS,result.getIp(),netInterface,
                    result,duration, cooldown);
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
