package enums;
/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * */
public enum C {

    NOT_CONFLICT(false), CONFLICT(true);

    public boolean code;

    C(boolean code) {
        this.code = code;
    }

    public static C getTypeByCode(boolean code)
    {
        for (C e : C.values())
        {
            if (e.code == code)
                return e;
        }
        return null;
    }
}
