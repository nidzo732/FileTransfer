package com.nidzo.filetransfer.transferclasses;


import com.nidzo.filetransfer.JSON.ObjectAttribute;
import com.nidzo.filetransfer.JSON.StringAttribute;
import com.nidzo.filetransfer.Strings;

public class PreSendRequest {
    @StringAttribute(name = Strings.JSON_FILE_NAME)
    private String fileName;
    @ObjectAttribute(name = Strings.JSON_PUBLICKEY)
    private PublicKey publicKey;
    public PreSendRequest()
    {
        publicKey = new PublicKey();
    }
    public PreSendRequest(String fileName, PublicKey publicKey)
    {
        this.fileName=fileName;
        this.publicKey=publicKey;
    }

    public String getFileName() {
        return fileName;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }
}
