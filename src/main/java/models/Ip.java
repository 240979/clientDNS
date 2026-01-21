/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * Based on work of Martin Biolek (https://github.com/mbio16/clientDNS)
 * */
package models;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import exceptions.InterfaceDoesNotHaveIPAddressException;
import exceptions.NotValidIPException;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import lombok.Data;
import org.apache.commons.lang.exception.ExceptionUtils;

@Data
public class Ip {
	public static final Logger LOGGER = Logger.getLogger(Ip.class.getName());
	private ArrayList<String> ipv4DnsServers;
	private ArrayList<String> ipv6DnsServers;
	private static final String COMMAND = "powershell.exe $ip=Get-NetIPConfiguration; $ip.'DNSServer' | ForEach-Object -Process {$_.ServerAddresses}";
	private String dohUserInputIp;

	public Ip() {
		try {
			setupArrays();
			parseDnsServersIp();
		} catch (Exception e) {
			LOGGER.severe(ExceptionUtils.getStackTrace(e));
		}
	}

	private void setupArrays() {
		ipv4DnsServers = new ArrayList<String>();
		ipv6DnsServers = new ArrayList<String>();
	}

	private void parseDnsServersIp() throws IOException {
		Process powerShellProcess;
		powerShellProcess = Runtime.getRuntime().exec(COMMAND);
		powerShellProcess.getOutputStream().close();
		String line;
		BufferedReader stdout = new BufferedReader(new InputStreamReader(powerShellProcess.getInputStream()));
		while ((line = stdout.readLine()) != null) {
			if (isIPv4Address(line)) {
				ipv4DnsServers.add(line);
			} else {
				if (isIpv6Address(line) && !line.startsWith("fe") && !ipv6DnsServers.contains(line))
					ipv6DnsServers.add(line);
			}
		}
		stdout.close();
	}

	public static String getIpV6OfDomainName(String domainName) throws UnknownHostException
	{
		InetAddress[] addresses = InetAddress.getAllByName(domainName);
		if (returnFirstInet6Address(addresses) == null)
		{
			return null;
		}
		return returnFirstInet6Address(addresses).getHostAddress();
	}

	private static Inet6Address returnFirstInet6Address(InetAddress[] addresses)
	{
		for (InetAddress address: addresses)
		{
			if (address instanceof Inet6Address)
			{
				return (Inet6Address) address;
			}
		}
		return null;
	}

	public static String getIpV4OfDomainName(String domainName) throws UnknownHostException
	{
		InetAddress[] addresses = InetAddress.getAllByName(domainName);
		if (returnFirstInet4Address(addresses) == null)
		{
			return null;
		}
		return returnFirstInet4Address(addresses).getHostAddress();
	}

	private static InetAddress returnFirstInet4Address(InetAddress[] addresses)
	{
		for (InetAddress address: addresses)
		{
			if (!(address instanceof Inet6Address))
			{
				return (InetAddress) address;
			}
		}
		return null;
	}

	public String getIpv4DnsServer() {
		if (ipv4DnsServers.isEmpty()) {
			return "";
		} else {
			return ipv4DnsServers.getFirst();
		}
	}

	public String getIpv6DnsServer() {
		if (ipv6DnsServers.isEmpty()) {
			return "";
		} else {
			return ipv6DnsServers.getFirst();
		}
	}

	public static boolean isIPv4Address(String stringAddress) {
		try {
			IPAddress ip = new IPAddressString(stringAddress).getAddress();
			return ip.isIPv4();
		} catch (Exception e) {
			return false;
		}
	}

	public static boolean isIpv6Address(String stringAddress) {
		try {
			IPAddress ip = new IPAddressString(stringAddress).getAddress();
			return ip.isIPv6();
		} catch (Exception e) {
			return false;
		}

	}

	public static boolean isIpValid(String stringAddress) {
		boolean a = Ip.isIPv4Address(stringAddress);
		boolean b = Ip.isIpv6Address(stringAddress);
		LOGGER.info("is IP :" + stringAddress + "valid-> " + ((a || b)));
		return (a || b);
	}

	public static String getIpReversed(String stringAddress) throws NotValidIPException {
		if (Ip.isIpValid(stringAddress)) {
			return new IPAddressString(stringAddress).getAddress().toReverseDNSLookupString();
		} else {
			throw new NotValidIPException();
		}
	}

	public static InetAddress getIpAddressFromInterface(NetworkInterface interfaceToSend, String resolverIP)
			throws InterfaceDoesNotHaveIPAddressException {
		//ArrayList<InterfaceAddress> ipAdrresses = (ArrayList<InterfaceAddress>) interfaceToSend.getInterfaceAddresses();
		List<InterfaceAddress> ipAdrresses = interfaceToSend.getInterfaceAddresses();
		for (InterfaceAddress sourceIp : ipAdrresses) {
			String sourceIpString = sourceIp.getAddress().getHostAddress();
			//System.out.println("IP addr: " + sourceIp.getAddress());
			if (Ip.isIpv6Address(resolverIP) && Ip.isIpv6Address(sourceIpString)) {
				return sourceIp.getAddress();
			}
			if (Ip.isIPv4Address(resolverIP) && Ip.isIPv4Address(sourceIpString)) {
				return sourceIp.getAddress();
			}
		}
		throw new InterfaceDoesNotHaveIPAddressException();
	}
}