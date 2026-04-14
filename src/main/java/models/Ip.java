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
	private static final String PS_COMMAND = "powershell.exe $ip=Get-NetIPConfiguration; $ip.'DNSServer' | ForEach-Object -Process {$_.ServerAddresses}";
    private static final String[] BASH_COMMAND = new String[]{
            "sh", "-c",
            "resolvectl dns 2>/dev/null | grep -oE '([0-9]{1,3}\\.){3}[0-9]{1,3}|([0-9a-fA-F]{0,4}:){2,7}[0-9a-fA-F]{0,4}'"
    };
    private static final String OS = System.getProperty("os.name").toLowerCase();
    private static final boolean IS_WINDOWS = OS.contains("win");
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
		ipv4DnsServers = new ArrayList<>();
		ipv6DnsServers = new ArrayList<>();
	}

	private void parseDnsServersIp() throws IOException {
		Process process;
        if(IS_WINDOWS){
            // process = Runtime.getRuntime().exec(PS_COMMAND);
			process = new ProcessBuilder(PS_COMMAND.split(" ")).start();
        } else {
			process = new ProcessBuilder(BASH_COMMAND).start();
        }
		process.getOutputStream().close();
        try (BufferedReader stdout = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = stdout.readLine()) != null) {
                line = line.trim(); // Trim once at the start
                if (isIPv4Address(line)) {
                    ipv4DnsServers.add(line);
                } else if (isIpv6Address(line) && !line.startsWith("fe") && !ipv6DnsServers.contains(line)) {
                    ipv6DnsServers.add(line);
                }
            }
        } // Auto-closes stdout with try-with-resources
	}

	public static String getIpV6OfDomainName(String domainName) throws UnknownHostException {
		InetAddress[] addresses = InetAddress.getAllByName(domainName);
		InetAddress address = returnFirstInet6Address(addresses);
		if (address == null)
		{
			return null;
		}
		return address.getHostAddress();
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
		InetAddress address = returnFirstInet4Address(addresses);
		if (address == null)
		{
			return null;
		}
		return address.getHostAddress();
	}

	private static InetAddress returnFirstInet4Address(InetAddress[] addresses)
	{
		for (InetAddress address: addresses)
		{
			if (!(address instanceof Inet6Address))
			{
				return address;
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

		List<InterfaceAddress> ipAddresses = interfaceToSend.getInterfaceAddresses();
		InetAddress fallbackLLA = null;

		for (InterfaceAddress sourceIp : ipAddresses) {
			String sourceIpString = sourceIp.getAddress().getHostAddress();
			if (Ip.isIpv6Address(resolverIP) && Ip.isIpv6Address(sourceIpString)) {
				if (sourceIp.getAddress().isLinkLocalAddress()) {
					fallbackLLA = sourceIp.getAddress();
				} else {
					return sourceIp.getAddress();
				}
			}
			if (Ip.isIPv4Address(resolverIP) && Ip.isIPv4Address(sourceIpString)) {
				return sourceIp.getAddress();
			}
		}
		if (fallbackLLA != null) {
			LOGGER.info("No global IPv6 address found, falling back to link-local: " + fallbackLLA.getHostAddress());
			return fallbackLLA;
		}
		throw new InterfaceDoesNotHaveIPAddressException();
	}
}