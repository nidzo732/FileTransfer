package com.nidzo.filetransfer.transferclasses;

import com.nidzo.filetransfer.JSON.ObjectAttribute;
import com.nidzo.filetransfer.JSON.StringAttribute;
import com.nidzo.filetransfer.Strings;

public class PairingRequest {
    @StringAttribute(name = Strings.JSON_NAME)
    private String name;
    @StringAttribute(name = Strings.JSON_GUID)
    private String guid;
    @ObjectAttribute(name = Strings.JSON_PUBLICKEY)
    private PublicKey publicKey;
    public PairingRequest()
    {
        publicKey = new PublicKey();
    }
    public PairingRequest(String name, String guid, PublicKey publicKey)
    {
        this.name=name;
        this.guid=guid;
        this.publicKey = publicKey;
    }

    public String getName() {
        return name;
    }

    public String getGuid() {
        return guid;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }
}
