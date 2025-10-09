package tasks.runnables;
/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * */

import javafx.scene.control.TreeItem;
import tasks.DNSTaskBase;
import ui.GeneralController;

import java.util.logging.Logger;
/**
 * Class extends Runnable and is used to publish results of requests to GUI after end of sending messages
 */
public class RequestResultsUpdateRunnable implements Runnable {

    DNSTaskBase dnsTaskBase = null;
    protected Logger LOGGER = Logger.getLogger(RequestResultsUpdateRunnable.class.getName());

    public RequestResultsUpdateRunnable(DNSTaskBase dnsTaskBase)
    {
        this.dnsTaskBase = dnsTaskBase;
    }

    protected void expandAll(TreeItem<String> t) {
        try {
            if(t.getChildren().size() == 0)
            {
                t.setExpanded(true);
                return;
            }
            for(TreeItem<String> item: t.getChildren())
            {
                expandAll(item);
                item.setExpanded(true);
            }
        } catch (Exception e) {
            LOGGER.warning(e.toString());
        }
    }

    @Override
    public void run() {
        // update
        GeneralController controller = dnsTaskBase.getController();
        controller.getSendButton().setText(controller.getButtonText());
        controller.getProgressBar().setProgress(0);
        dnsTaskBase.setMessagesSentProperty(dnsTaskBase.getMessagesSent());
        dnsTaskBase.setRequestProperty(dnsTaskBase.getRequest());
        dnsTaskBase.setResponseProperty(dnsTaskBase.getResponse());
        dnsTaskBase.setDurationProperty(dnsTaskBase.getDuration());
        expandAll(dnsTaskBase.getRequestProperty());
        expandAll(dnsTaskBase.getResponseProperty());
        dnsTaskBase.setQuerySizeProperty(dnsTaskBase.getByteSizeQuery());
        dnsTaskBase.setResponseSizeProperty(dnsTaskBase.getByteSizeResponse());
        LOGGER.info("setting progress to 0 to reset");
        dnsTaskBase.getProgressBar().setProgress(0);
        dnsTaskBase.getRequestProperty().setExpanded(true);
        dnsTaskBase.getResponseProperty().setExpanded(true);
    }
}
