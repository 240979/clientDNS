package testing.tasks;
/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * */
import enums.APPLICATION_PROTOCOL;
import enums.TRANSPORT_PROTOCOL;
import javafx.application.Platform;
import javafx.concurrent.Task;
import lombok.Data;
import models.ConnectionSettings;
import models.RequestSettings;
import org.apache.commons.lang.exception.ExceptionUtils;
import tasks.DNSTaskBase;
import testing.*;
import ui.GeneralController;
import ui.TesterController;

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
    private int duration;
    private List<List<Result>> observableList;
    private long cooldown;
    private GeneralController controller;
    private static Logger LOGGER = Logger.getLogger(TesterTask.class.getName());
    private RequestSettings requestSettings;
    private ConnectionSettings connectionSettings;
    public TesterTask(RequestSettings rs, ConnectionSettings cs, int duration, List<List<Result>> observableList, long cooldown, GeneralController controller) {
        this.duration = duration;
        this.observableList = observableList;
        this.cooldown = cooldown;
        this.controller = controller;
        LOGGER.info("Created main thread for mass testing");
        this.requestSettings = rs;
        this.connectionSettings = cs;
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
                if (connectionSettings.getApplication_protocol() == APPLICATION_PROTOCOL.DOH) {
                    task = new DoHTester(requestSettings, connectionSettings, duration, result, cooldown);
                    ((DoHTester) task).setController(controller);
                } else if (connectionSettings.getApplication_protocol() == APPLICATION_PROTOCOL.DOT) {
                    task = new DoTTester(requestSettings, connectionSettings, duration, result, cooldown);
                    ((DoTTester) task).setController(controller);
                } else if (connectionSettings.getTransport_protocol() == TRANSPORT_PROTOCOL.UDP) {
                    task = new UdpTester(requestSettings, connectionSettings, duration, result, cooldown);
                    ((UdpTester) task).setController(controller);
                } else {
                    task = new TcpTester(requestSettings,connectionSettings,duration,result,cooldown);
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
                notFinished = false;
                for (List<Result> results : observableList) {
                    for (Result result : results) {
                        // if at least one result is marked as not finished continue with cycle
                        if (!result.isComplete()) {
                            notFinished = true;
                        }
                        // propagate results to UI
                        // calculate average duration for every result
                        result.setAverage(Math.floor(result.getDurations().stream().mapToDouble(d -> d).average().orElse(0.0)*100)/100);
                        result.setMax(Math.floor(result.getDurations().stream().mapToDouble(d -> d).max().orElse(0.0)*100)/100);
                        result.setMin(Math.floor(result.getDurations().stream().mapToDouble(d -> d).min().orElse(0.0)*100)/100);
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
