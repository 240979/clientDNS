/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * Based on work of Martin Biolek (https://github.com/mbio16/clientDNS)
 * */
package enums;

public enum TC
{
    TRUNCATED((boolean) true), NON_TRUNCATED((boolean) false);

    public boolean code;

    private TC(boolean code)
    {
        this.code = code;
    }

    public static TC getTypeByCode(boolean code)
    {
        for (TC e : TC.values())
        {
            if (e.code == code)
                return e;
        }
        return null;
    }

}
