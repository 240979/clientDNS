package tasks.runnables;
/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * */
import tasks.DNSTaskBase;

import java.util.logging.Logger;
/**
 * Class extends Runnable and is used to stop progress bar after encountering error
 */
public class StopProgressBar implements Runnable{
    DNSTaskBase dnsTaskBase = null;
    private String reason;
    protected Logger LOGGER = Logger.getLogger(ProgressUpdateRunnable.class.getName());

    public StopProgressBar(DNSTaskBase dnsTaskBase, String reason)
    {
        this.dnsTaskBase = dnsTaskBase;
        this.reason = reason;
    }

    @Override
    public void run() {
        dnsTaskBase.getController().getSendButton().setText(dnsTaskBase.getController().getButtonText());
        dnsTaskBase.getProgressBar().setProgress(0);
        dnsTaskBase.getController().showAlert(reason);
    }
}
