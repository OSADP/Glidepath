package com.leidos.glidepath.dvi.domain;


public class OperatingSpeedResponse extends AjaxResponse {

    String operatingSpeed;

    public OperatingSpeedResponse(boolean result, String serverMessage, String operatingSpeed)   {
        super(result, serverMessage);
        this.operatingSpeed = operatingSpeed;
    }

    public String getOperatingSpeed()   {
        return operatingSpeed;
    }
}
