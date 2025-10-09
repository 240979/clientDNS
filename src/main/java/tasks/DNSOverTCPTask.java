package tasks;
/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * Methods used from Martin Biolek thesis are marked with comment
 * */
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.NetworkInterface;
import java.net.UnknownHostException;

import enums.APPLICATION_PROTOCOL;
import enums.Q_COUNT;
import enums.TRANSPORT_PROTOCOL;
import exceptions.*;
import javafx.application.Platform;
import tasks.runnables.RequestResultsUpdateRunnable;

/**
 * Class representing protocol DNS using TCP connection
 */
public class DNSOverTCPTask extends DNSTaskBase {

	protected boolean holdConnection;

	public DNSOverTCPTask(boolean recursion, boolean adFlag, boolean caFlag, boolean doFlag, boolean holdConnection, String domain, Q_COUNT[] types,
						  TRANSPORT_PROTOCOL transport_protocol, APPLICATION_PROTOCOL application_protocol,
						  String resolverIP, NetworkInterface netInterface)
			throws IOException, NotValidIPException, NotValidDomainNameException
	{
		super(recursion,adFlag,caFlag,doFlag,domain,types,transport_protocol,application_protocol, resolverIP, netInterface,null);
		this.holdConnection = holdConnection;
		if (!holdConnection)
		{
			if (DNSTaskBase.getTcpConnectionForServer(resolverIP) != null)
			{
				DNSTaskBase.getTcpConnectionForServer(resolverIP).closeAll();
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
		} catch (IOException | TimeoutException | CouldNotUseHoldConnectionException |
				InterfaceDoesNotHaveIPAddressException e) {
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
