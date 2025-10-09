package tasks.runnables;
/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * */
import javafx.scene.control.TreeItem;
import tasks.DNSTaskBase;

import java.util.logging.Logger;

/**
 * Class extends Runnable and is used to publish results of requests to GUI during send of messages
 */
public class ProgressUpdateRunnable implements Runnable
{
    DNSTaskBase dnsTaskBase = null;
    protected Logger LOGGER = Logger.getLogger(ProgressUpdateRunnable.class.getName());

    public ProgressUpdateRunnable(DNSTaskBase dnsTaskBase)
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
    public void run()
    {
        dnsTaskBase.setMessagesSentProperty(dnsTaskBase.getMessagesSent());
        dnsTaskBase.setRequestProperty(dnsTaskBase.getRequest());

        if (dnsTaskBase.getRequestProperty() != null) {
            dnsTaskBase.getRequestProperty().setExpanded(true);
            expandAll(dnsTaskBase.getRequestProperty());
        }
        if (dnsTaskBase.getResponseProperty() != null){
            dnsTaskBase.getRequestProperty().setExpanded(true);
            expandAll(dnsTaskBase.getRequestProperty());
        }
        dnsTaskBase.setResponseProperty(dnsTaskBase.getResponse());
        dnsTaskBase.setDurationProperty(dnsTaskBase.getDuration());
        dnsTaskBase.setResponseSizeProperty(dnsTaskBase.getByteSizeResponse());
        dnsTaskBase.setQuerySizeProperty(dnsTaskBase.getByteSizeQuery());
    }
}
