package com.nidzo.filetransfer.transferclasses;

import com.nidzo.filetransfer.JSON.StringAttribute;
import com.nidzo.filetransfer.Strings;

public class Peer {
    @StringAttribute(name = Strings.JSON_NAME)
    private String name;
    @StringAttribute(name = Strings.JSON_GUID)
    private String guid;
    @StringAttribute(name = Strings.JSON_PUBLICKEY)
    private String publicKey;
    @StringAttribute(name = Strings.JSON_PRIVATEKEY)
    private String myPrivateKey;
    @StringAttribute(name = "SHAREDPASSWORD")
    private String sharedPassword;
    private String IP;

    public Peer(){}
    public Peer(String guid, String name)
    {
        this.name=name;
        this.guid=guid;
    }
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGuid() {
        return guid;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public String getMyPrivateKey() {
        return myPrivateKey;
    }

    public String getSharedPassword() {
        return sharedPassword;
    }

    public String getIP() {
        return IP;
    }

    public void setIP(String IP) {
        this.IP = IP;
    }
    public void pair(String sharedPassword, String publicKey, String myPrivateKey)
    {
        this.sharedPassword=sharedPassword;
        this.publicKey=publicKey;
        this.myPrivateKey=myPrivateKey;
    }
    public void unpair()
    {
        sharedPassword=publicKey=myPrivateKey=null;
    }
}
