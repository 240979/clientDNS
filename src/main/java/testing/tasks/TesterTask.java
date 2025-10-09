package testing.tasks;
/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * */
import enums.APPLICATION_PROTOCOL;
import enums.Q_COUNT;
import enums.TRANSPORT_PROTOCOL;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import lombok.Data;
import org.apache.commons.lang.exception.ExceptionUtils;
import tasks.DNSTaskBase;
import testing.*;
import ui.GeneralController;
import ui.TesterController;

import java.net.NetworkInterface;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
/**
* Main Mass Testing task, which starts protocol specific tasks also preparing data structure for storing of results,
* after start of protocol specific task waits until the end of all requests, calculate results on fly and display them
*
* */
@Data
public class TesterTask extends Task<Void> {

    List<Task> tasks = new LinkedList<>();
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
    private List<List<Result>> observableList;
    private long cooldown;
    private GeneralController controller;
    private static Logger LOGGER = Logger.getLogger(TesterTask.class.getName());

    public TesterTask(boolean recursion, boolean adFlag, boolean caFlag, boolean doFlag, boolean holdConnection, String domain, Q_COUNT[] types, TRANSPORT_PROTOCOL transport_protocol, APPLICATION_PROTOCOL application_protocol, NetworkInterface netInterface, int duration, List<List<Result>> observableList, long cooldown, GeneralController controller) {

        this.recursion = recursion;
        this.adFlag = adFlag;
        this.caFlag = caFlag;
        this.doFlag = doFlag;
        this.holdConnection = holdConnection;
        this.domain = domain;
        this.types = types;
        this.transport_protocol = transport_protocol;
        this.application_protocol = application_protocol;
        this.netInterface = netInterface;
        this.duration = duration;
        this.observableList = observableList;
        this.cooldown = cooldown;
        this.controller = controller;
        LOGGER.info("Created main thread for mass testing");
    }

    @Override
    protected Void call() throws Exception {
        // start thread for testing each server
        List<Thread> threads = null;
        try {
            LOGGER.info("Main mass testing thread started");
            threads = new LinkedList<>();
            List<Task> tasks = new LinkedList<>();
            // create tasks, threads and pass result data structure to task
            for (List<Result> result : observableList) {
                Task task;
                if (application_protocol == APPLICATION_PROTOCOL.DOH) {
                    task = new DoHTester(recursion, adFlag, caFlag, doFlag, types,
                            TRANSPORT_PROTOCOL.TCP, APPLICATION_PROTOCOL.DOH, netInterface, duration, result, cooldown);
                    ((DoHTester) task).setController(controller);
                } else if (application_protocol == APPLICATION_PROTOCOL.DOT) {
                    task = new DoTTester(recursion, adFlag, caFlag, doFlag, types,
                            TRANSPORT_PROTOCOL.TCP, application_protocol, netInterface, result, duration, cooldown);
                    ((DoTTester) task).setController(controller);
                } else if (transport_protocol == TRANSPORT_PROTOCOL.UDP) {
                    task = new UdpTester(recursion, adFlag, caFlag, doFlag, types,
                            TRANSPORT_PROTOCOL.UDP, APPLICATION_PROTOCOL.DNS, netInterface, duration, result, cooldown);
                    ((UdpTester) task).setController(controller);
                } else {
                    task = new TcpTester(recursion, adFlag, caFlag, doFlag, holdConnection, types,
                            TRANSPORT_PROTOCOL.TCP, APPLICATION_PROTOCOL.DNS, netInterface, duration, result, cooldown);
                    ((TcpTester) task).setController(controller);
                }
                Thread thread = new Thread(task);
                tasks.add(task);
                threads.add(thread);
            }
            LOGGER.info("Created TcpTester tasks");
            // start threads and watch over until all tasks finish
            threads.forEach(Thread::start);
            LOGGER.info("Started all TcpTester tasks");
            boolean notFinished = true;
            // threads watchdog
            while (notFinished) {
                //LOGGER.info("Checking if TcpTester tasks completed");
                notFinished = false;
                for (List<Result> results : observableList) {
                    for (Result result : results) {
                        // if at least one result is marked as not finished continue with cycle
                        if (!result.isComplete()) {
                            notFinished = true;
                        }
                        // propagate results to UI
                        // calculate average duration for every result
                        // TODO maybe use runnable and Platform.runLater(...)
                        //LOGGER.info("Calculating average duration of requests in TcpTester task for " + result.getName());
                        result.setAverage(Math.floor(result.getDurations().stream().mapToDouble(value -> value).average().orElse(0.0)*100)/100);
                        result.setMax(Math.floor(result.getDurations().stream().mapToDouble(value -> value).max().orElse(0.0)*100)/100);
                        result.setMin(Math.floor(result.getDurations().stream().mapToDouble(value -> value).min().orElse(0.0)*100)/100);
                        result.setSuccessful(result.getSuccess().stream().filter(aBoolean -> aBoolean).count());
                        result.setFailed(result.getSuccess().stream().filter(aBoolean -> !aBoolean).count());
                    }
                }
                LOGGER.info("notFinished: " + notFinished);
                Thread.sleep(750);
            }
            LOGGER.info("All TcpTester tasks completed, let's wait for all TcpTester Threads to join");
            for (Thread t : threads) {
                try {
                    t.join();
                } catch (InterruptedException e) {
                    //e.printStackTrace();
                    LOGGER.severe(ExceptionUtils.getStackTrace(e));
                }
            }
            LOGGER.info("All threads joined, ending mass testing main thread");
            if (DNSTaskBase.getTcp() != null) {
                DNSTaskBase.getTcp().clear();
            }
            observableList.forEach(result ->
                    result.forEach(result1 ->
                            LOGGER.info(result1.getName() + ": " + result1.getAverage())));
            Platform.runLater(() -> {
                controller.getProgressBar().setProgress(0);
                controller.getSendButton().setText(controller.getButtonText());
                ((TesterController) controller).getResultsTableView().refresh();
            });
            controller.setThread(null);
        } catch (InterruptedException e) {
            Platform.runLater(()->controller.getProgressBar().setProgress(0));
            LOGGER.info("TesterTask interrupted");
            threads.forEach(Thread::interrupt);
        }
        return null;
    }


}
