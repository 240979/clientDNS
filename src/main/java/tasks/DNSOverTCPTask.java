package tasks;
/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * Methods used from Martin Biolek thesis are marked with comment
 * */
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;

import exceptions.*;
import javafx.application.Platform;
import models.ConnectionSettings;
import models.RequestSettings;
import tasks.runnables.RequestResultsUpdateRunnable;

/**
 * Class representing protocol DNS using TCP connection
 */
public class DNSOverTCPTask extends DNSTaskBase {

	protected boolean holdConnection;

	public DNSOverTCPTask(RequestSettings requestSettings, ConnectionSettings connectionSettings) throws IOException, NotValidDomainNameException, NotValidIPException {
		super(requestSettings, connectionSettings, null);
		this.holdConnection = connectionSettings.isHoldConnection();
		if (!holdConnection)
		{
			if (DNSTaskBase.getTcpConnectionForServer(connectionSettings.getResolverIP()) != null)
			{
				DNSTaskBase.getTcpConnectionForServer(connectionSettings.getResolverIP()).closeAll();
			}
		}
	}

	/*
	 * Body of method taken from Martin Biolek thesis and modified
	 * */
	@Override
	protected void sendData() throws TimeoutException, NotValidDomainNameException, NotValidIPException, UnsupportedEncodingException, InterruptedException, QueryIdNotMatchException, UnknownHostException {
		try {
			// start measuring time
			setStartTime(System.nanoTime());
			// setup TCP connection to DNS server
			if (DNSTaskBase.getTcpConnectionForServer(resolver) == null) {
				DNSTaskBase.setTcpConnectionForServer(resolver,resolver);
			} else if(DNSTaskBase.getTcpConnectionForServer(resolver).isClosed()) {
				DNSTaskBase.setTcpConnectionForServer(resolver,resolver);
			}
			// send data and store them by method setReceiveReply
			setReceiveReply(DNSTaskBase.getTcpConnectionForServer(resolver).send(getMessageAsBytes(), getIp(), isCloseConnection(), getInterfaceToSend()));
			setWasSend(true);
			// stop measuring time
			setStopTime(System.nanoTime());
			// calculate duration of whole DNS request including setup of TCP connection
			setDuration(calculateDuration());
			setMessagesSent(1);
		} catch (IOException
				 | TimeoutException
				 | CouldNotUseHoldConnectionException
				 | InterfaceDoesNotHaveIPAddressException e) {
			if (!massTesting){
				Platform.runLater(()->{
				controller.getSendButton().setText(controller.getButtonText());
				controller.getProgressBar().setProgress(0);
			});
			}
			throw new TimeoutException();
		}
	}

	@Override
	protected void updateProgressUI()
	{
	}

	@Override
	protected void updateResultUI() {
		// updating UI with DNS request results
		Platform.runLater(new RequestResultsUpdateRunnable(this));
	}

	@Override
	public void stopExecution(){
		DNSTaskBase.terminateAllTcpConnections();
	}
}
