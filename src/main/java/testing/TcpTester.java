package testing;
/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * */
import enums.APPLICATION_PROTOCOL;
import enums.Q_COUNT;
import enums.TRANSPORT_PROTOCOL;
import javafx.concurrent.Task;
import lombok.Getter;
import lombok.Setter;
import models.ConnectionSettings;
import models.RequestSettings;
import org.apache.commons.lang.exception.ExceptionUtils;
import tasks.DNSTaskBase;
import testing.tasks.TestTcpTask;
import ui.GeneralController;

import java.net.NetworkInterface;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
/**
 * Protocol specific task, which starts another task, which perform repeated querying on domain against given server
 * Sequentially for each domain task is started after previous has finished
 * */
@Getter
@Setter
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
    private List<TestTcpTask> tasks = new LinkedList<>();
    private static Logger LOGGER = Logger.getLogger(TcpTester.class.getName());
    private GeneralController controller;
    private RequestSettings requestSettings;
    private ConnectionSettings connectionSettings;

    public TcpTester(RequestSettings requestSettings, ConnectionSettings connectionSettings, int duration, List<Result> results, long cooldown)
    {
        this.requestSettings = requestSettings;
        this.connectionSettings = connectionSettings;
        this.duration = duration;
        this.results = results;
        this.cooldown = cooldown;
        LOGGER.info("Created TcpTester task");
    }

    @Override
    protected Void call() throws Exception {
        LOGGER.info("TcpTester task started");
        for (Result result : results) {
            // create new thread, which will start given task, which runs DNS over TCP and returns duration of request
            // to given Double object which was passed inside
            ConnectionSettings cs = new ConnectionSettings.ConnectionSettingsBuilder(this.connectionSettings)
                    .resolverIP(result.getIp())
                    .build();
            RequestSettings rs = new RequestSettings.RequestSettingsBuilder(this.requestSettings)
                    .domain(result.getDomain())
                    .build();
            DNSTaskBase task = new TestTcpTask(rs, cs, result, duration, cooldown);
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
