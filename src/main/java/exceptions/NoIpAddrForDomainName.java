package exceptions;
/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * */
public class NoIpAddrForDomainName extends Exception{

    public NoIpAddrForDomainName() {
        super("Can not obtain IP address of specified version");
    }
}
