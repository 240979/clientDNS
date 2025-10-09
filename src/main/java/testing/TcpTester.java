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
import lombok.EqualsAndHashCode;
import org.apache.commons.lang.exception.ExceptionUtils;
import tasks.DNSTaskBase;
import testing.tasks.DnsTcpTask;
import ui.GeneralController;

import java.io.IOException;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
/**
 * Protocol specific task, which starts another task, which perform repeated querying on domain against given server
 * Sequentially for each domain task is started after previous has finished
 * */
@Data
public class TcpTester extends Task<Void> {

    private boolean recursion;
    private boolean adFlag;
    private boolean caFlag;
    private boolean doFlag;
    private boolean holdConnection;
    private String domain;
    private Q_COUNT[] types;
    private TRANSPORT_PROTOCOL transport_protocol;
    private APPLICATION_PROTOCOL application_protocol;
    private NetworkInterface netInterface;
    private int duration;
    private long cooldown;
    private List<Result> results;
    private List<DnsTcpTask> tasks = new LinkedList<>();
    private static Logger LOGGER = Logger.getLogger(TcpTester.class.getName());
    private GeneralController controller;

    public TcpTester(boolean recursion, boolean adFlag, boolean caFlag, boolean doFlag, boolean holdConnection,
                     Q_COUNT[] types, TRANSPORT_PROTOCOL transport_protocol, APPLICATION_PROTOCOL application_protocol,
                     NetworkInterface netInterface,int duration, List<Result> results, long cooldown)
    {
        this.recursion = recursion;
        this.adFlag = adFlag;
        this.caFlag = caFlag;
        this.doFlag = doFlag;
        this.holdConnection = holdConnection;
        this.cooldown = cooldown;
        this.domain = domain;
        this.types = types;
        this.transport_protocol = transport_protocol;
        this.application_protocol = application_protocol;
        this.netInterface = netInterface;
        this.duration = duration;
        this.results = results;
        LOGGER.info("Created TcpTester task");
    }

    @Override
    protected Void call() throws Exception {
        LOGGER.info("TcpTester task started");
        for (Result result : results) {
            // create new thread, which will start given task, which runs DNS over TCP and returns duration of request
            // to given Double object which was passed inside
            LOGGER.info("StartingTcpTester for " + result.getName());
            DNSTaskBase task = new DnsTcpTask(recursion, adFlag, caFlag, doFlag, holdConnection,
                    result.getDomain(), types, TRANSPORT_PROTOCOL.TCP, APPLICATION_PROTOCOL.DNS, result.getIp(),
                    netInterface, result, duration, cooldown);
            task.setMassTesting(true);
            task.setController(controller);
            Thread thread = new Thread(task);
            try {
                thread.start();
                thread.join();
            } catch (InterruptedException e) {
                DNSTaskBase.terminateAllTcpConnections();
                LOGGER.info("TcpTester interrupted");
                thread.interrupt();
                results.forEach(r -> r.setComplete(true));
                //e.printStackTrace();
                LOGGER.severe(ExceptionUtils.getStackTrace(e));
                return null;
            }
            LOGGER.info("Ended TcpTester for " + result.getName());
            LOGGER.info("Setting complete to true");
            result.setComplete(true);
        }
        return null;
    }
}
