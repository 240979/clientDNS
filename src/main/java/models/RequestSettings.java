/*
 * Created by 240979
 * Based on: https://github.com/xramos00/DNS_client
 *           https://github.com/mbio16/clientDNS
 */
package models;

import enums.Q_COUNT;
import lombok.Getter;

public class RequestSettings {

    @Getter
    private final boolean recursion;

    @Getter
    boolean adFlag;

    @Getter
    boolean cdFlag;

    @Getter
    boolean doFlag;

    @Getter
    String domain;

    @Getter
    Q_COUNT[] types;

    @Getter
    boolean isGet;

    private RequestSettings(RequestSettingsBuilder rsb){
        this.recursion = rsb.recursion;
        this.adFlag = rsb.adFlag;
        this.cdFlag = rsb.cdFlag;
        this.doFlag = rsb.doFlag;
        this.domain = rsb.domain;
        this.types = rsb.types;
        this.isGet = rsb.isGet;
    }

    public Header getHeader(){
        return new Header(recursion, adFlag, types.length, doFlag, cdFlag);
    }
    public static class RequestSettingsBuilder{
        private boolean recursion;
        boolean adFlag;
        boolean cdFlag;
        boolean doFlag;
        String domain;
        Q_COUNT[] types;
        boolean isGet;

        public RequestSettingsBuilder(){
            this.recursion = false;
            this.adFlag = false;
            this.cdFlag = false;
            this.doFlag = false;
            this.isGet = false;
            this.domain = null;
            this.types = null;
        }
        public RequestSettingsBuilder(RequestSettings rs) {
            this.recursion = rs.recursion;
            this.adFlag = rs.adFlag;
            this.cdFlag = rs.cdFlag;
            this.doFlag = rs.doFlag;
            this.domain = rs.domain;
            this.types = rs.types;
            this.isGet = rs.isGet;
        }

        public RequestSettings build() {
            return new RequestSettings(this);
        }

        public RequestSettingsBuilder recursion(boolean recursion){
            this.recursion = recursion;
            return this;
        }
        public RequestSettingsBuilder adFlag(boolean adFlag){
            this.adFlag = adFlag;
            return this;
        }
        public RequestSettingsBuilder cdFlag(boolean cdFlag){
            this.cdFlag = cdFlag;
            return this;
        }
        public RequestSettingsBuilder doFlag(boolean doFlag){
            this.doFlag = doFlag;
            return this;
        }
        public RequestSettingsBuilder isGet(boolean isGet){
            this.isGet = isGet;
            return this;
        }
        public RequestSettingsBuilder domain(String domain){
            this.domain = domain;
            return this;
        }
        public RequestSettingsBuilder types(Q_COUNT[] types){
            this.types = types;
            return this;
        }
    }
}
