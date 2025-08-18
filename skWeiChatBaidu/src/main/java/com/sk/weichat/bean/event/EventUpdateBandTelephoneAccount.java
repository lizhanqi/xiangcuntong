package com.sk.weichat.bean.event;

public class EventUpdateBandTelephoneAccount {
    public String result;
    public String msg;

    public EventUpdateBandTelephoneAccount(String result, String ok) {
        this.result = result;
        this.msg = ok;
    }
}
