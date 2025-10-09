package enums;
/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * */
public enum T {

    NOT_TENTATIVE(false), TENTATIVE(true);

    public boolean code;

    T(boolean code) {
        this.code = code;
    }

    public static T getTypeByCode(boolean code)
    {
        for (T e : T.values())
        {
            if (e.code == code)
                return e;
        }
        return null;
    }
}
