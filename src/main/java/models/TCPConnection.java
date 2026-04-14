/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * Based on work of Martin Biolek (https://github.com/mbio16/clientDNS)
 * */
package models;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.util.logging.Logger;

import exceptions.CouldNotUseHoldConnectionException;
import exceptions.InterfaceDoesNotHaveIPAddressException;
import exceptions.TimeoutException;
import org.apache.commons.lang.exception.ExceptionUtils;

import static tasks.DNSTaskBase.TIME_OUT_MILLIS;

public class TCPConnection {
	private InetAddress destinationIp;
	private Socket socket;
	private OutputStream outputStream;
	private NetworkInterface netInterface;
	private InputStream inputStream;
	private static final int DNS_PORT = 53;
	private byte[] responseMessage;
	protected static Logger LOGGER = Logger.getLogger(TCPConnection.class.getName());

	public TCPConnection(InetAddress ip) {
		this.destinationIp = ip;
		responseMessage = null;
	}

	public byte[] send(byte[] messagesAsBytes, InetAddress ip, boolean closeConnection, NetworkInterface netInterface)
			throws TimeoutException, IndexOutOfBoundsException, IOException, CouldNotUseHoldConnectionException,
			InterfaceDoesNotHaveIPAddressException {
		this.netInterface = netInterface;
		LOGGER.info("Socket:" + (socket==null ? "empty socket slot" : socket.toString()));
		if (socket == null) {
			connect();
		}
		if (!socket.getInetAddress().equals(ip)) {
			closeAll();
			this.destinationIp = ip;
			connect();
		}
		if (socket.isClosed() || !socket.isConnected()) {
			socket = null;
			connect();
		}
		try {
			sendAndReceive(messagesAsBytes);
		} catch (CouldNotUseHoldConnectionException e) {
			// server closed the connection on its side, reconnect and retry once
			connect();
			sendAndReceive(messagesAsBytes);
		}
		if (closeConnection)
			closeAll();
		return responseMessage;
	}

	private void connect() throws IOException, InterfaceDoesNotHaveIPAddressException {
		try {
			LOGGER.info("Net Interface: " + netInterface.getDisplayName());
			socket = new Socket(destinationIp, DNS_PORT,
					Ip.getIpAddressFromInterface(netInterface, destinationIp.getHostAddress()), 0);
            socket.setSoTimeout(TIME_OUT_MILLIS);
			outputStream = socket.getOutputStream();
			inputStream = socket.getInputStream();
		} catch (IndexOutOfBoundsException e) {
			throw new InterfaceDoesNotHaveIPAddressException();
		}
	}

	public void closeAll() throws IOException {
		if (socket != null && (socket.isConnected() || !socket.isClosed())) {
			LOGGER.info("Closing sockets");
			inputStream.close();
			outputStream.close();
			socket.close();
		}
	}

	/*
	* Added by Patricia Ramosova
	* */
	public boolean isClosed()
	{
		return socket == null || socket.isClosed();
	}

	private void sendAndReceive(byte[] messagesAsBytes)
			throws CouldNotUseHoldConnectionException, TimeoutException {
		try {
			outputStream.write(messagesAsBytes);
			// dns message has first two bytes which is the length of the rest of the
			// message
			byte[] sizeReceived = inputStream.readNBytes(2);
            if (sizeReceived.length < 2) {
                LOGGER.warning("Received size is not 16 bits");
                throw new CouldNotUseHoldConnectionException(); // connection closed prematurely
            }
			UInt16 messageSize = new UInt16().loadFromBytes(sizeReceived[0], sizeReceived[1]);
			// based on size get the dns message itself
			responseMessage = inputStream.readNBytes(messageSize.getValue());
            LOGGER.info("Expected: " + messageSize.getValue() + " got: " + responseMessage.length);
		} catch (ArrayIndexOutOfBoundsException e) {
			//System.out.println(e.toString());
			LOGGER.warning(ExceptionUtils.getStackTrace(e));
			/*
			closeAll();*/
			throw new CouldNotUseHoldConnectionException();
		} catch (IOException e) {
			throw new TimeoutException();
		}
	}

}
