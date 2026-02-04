package models;

import enums.Q_COUNT;
import lombok.Getter;
import lombok.Setter;

public class RequestSettings {

    @Getter
    private boolean recursion;

    @Getter
    boolean adFlag;

    @Getter
    boolean cdFlag;

    @Getter
    boolean doFlag;

    @Getter
    @Setter
    String domain;

    @Getter
    Q_COUNT[] types;

    private RequestSettings(RequestSettingsBuilder rsb){
        this.recursion = rsb.recursion;
        this.adFlag = rsb.adFlag;
        this.cdFlag = rsb.cdFlag;
        this.doFlag = rsb.doFlag;
        this.domain = rsb.domain;
        this.types = rsb.types;
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

        public RequestSettingsBuilder(){
            this.recursion = false;
            this.adFlag = false;
            this.cdFlag = false;
            this.doFlag = false;
            this.domain = null;
            this.types = null;
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
