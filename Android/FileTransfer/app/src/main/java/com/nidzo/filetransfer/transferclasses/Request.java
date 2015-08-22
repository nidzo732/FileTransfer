package com.nidzo.filetransfer.transferclasses;

import com.nidzo.filetransfer.JSON.StringAttribute;
import com.nidzo.filetransfer.Strings;

public class Request {
    @StringAttribute(name = Strings.JSON_REQUEST_TYPE)
    private String type;
    @StringAttribute(name = Strings.JSON_REQUEST_DATA)
    private String data;
    @StringAttribute(name = Strings.JSON_GUID)
    private String senderGuid;
    public Request(){}
    public Request(String type, String data, String guid)
    {
        this.type=type;
        this.data=data;
        this.senderGuid=guid;
    }

    public String getType() {
        return type;
    }

    public String getData() {
        return data;
    }

    public String getSenderGuid() {
        return senderGuid;
    }
}
