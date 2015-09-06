package com.nidzo.filetransfer.transferclasses;


import com.nidzo.filetransfer.JSON.StringAttribute;
import com.nidzo.filetransfer.Strings;

public class PreSendRequestNoCrypt {
    @StringAttribute(name = Strings.JSON_FILE_NAME)
    private String fileName;
    public PreSendRequestNoCrypt()
    {

    }
    public PreSendRequestNoCrypt(String fileName)
    {
        this.fileName=fileName;
    }

    public String getFileName() {
        return fileName;
    }
}
