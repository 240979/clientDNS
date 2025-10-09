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
import tasks.DNSTaskBase;
import testing.tasks.DnsTcpTask;
import testing.tasks.DnsUdpTask;
import ui.GeneralController;

import java.net.NetworkInterface;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
/**
 * Protocol specific task, which starts another task, which perform repeated querying on domain against given server
 * Sequentially for each domain task is started after previous has finished
 * */
@Data
public class UdpTester extends Task<Void> {

    private boolean recursion;
    private boolean adFlag;
    private boolean caFlag;
    private boolean doFlag;
    private Q_COUNT[] types;
    private TRANSPORT_PROTOCOL transport_protocol;
    private APPLICATION_PROTOCOL application_protocol;
    private NetworkInterface netInterface;
    private int duration;
    private List<Result> results;
    private long cooldown;
    private List<DnsTcpTask> tasks = new LinkedList<>();
    private static Logger LOGGER = Logger.getLogger(TcpTester.class.getName());
    private GeneralController controller;

    public UdpTester(boolean recursion, boolean adFlag, boolean caFlag, boolean doFlag, Q_COUNT[] types,
                     TRANSPORT_PROTOCOL transport_protocol, APPLICATION_PROTOCOL application_protocol,
                     NetworkInterface netInterface, int duration, List<Result> results, long cooldown) {
        this.recursion = recursion;
        this.adFlag = adFlag;
        this.caFlag = caFlag;
        this.doFlag = doFlag;
        this.types = types;
        this.transport_protocol = transport_protocol;
        this.application_protocol = application_protocol;
        this.netInterface = netInterface;
        this.duration = duration;
        this.results = results;
        this.cooldown = cooldown;
        LOGGER.info("Created UdpTester task");
    }

    @Override
    protected Void call() throws Exception {
        LOGGER.info("UdpTester task started");
        for (Result result: results){
            // create new thread, which will start given task, which runs DNS over TCP and returns duration of request
            // to given Double object which was passed inside
            LOGGER.info("Starting UdpTester for "+result.getName());
            DNSTaskBase task = new DnsUdpTask(recursion,adFlag,caFlag,doFlag,result.getDomain(),types,
                    TRANSPORT_PROTOCOL.UDP, APPLICATION_PROTOCOL.DNS,result.getIp(),netInterface,
                    result,duration, cooldown);
            Thread thread = new Thread(task);
            task.setMassTesting(true);
            task.setController(controller);
            try {
                thread.start();
                thread.join();
            }
            catch (InterruptedException e)
            {
                thread.interrupt();
                results.forEach(r -> r.setComplete(true));
                LOGGER.info("UdpTester interrupted");
                //
                // e.printStackTrace();
                return null;
            }
            LOGGER.info("Ended UdpTester for "+result.getName());
            LOGGER.info("Setting complete to true");
            result.setComplete(true);
        }
        return null;
    }
}
