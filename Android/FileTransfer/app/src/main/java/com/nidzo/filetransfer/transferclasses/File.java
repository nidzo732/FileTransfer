package com.nidzo.filetransfer.transferclasses;

import com.nidzo.filetransfer.JSON.StringAttribute;
import com.nidzo.filetransfer.cryptography.AES;
import com.nidzo.filetransfer.cryptography.CryptographyException;
import com.nidzo.filetransfer.Strings;

public class File {
    @StringAttribute(name = Strings.JSON_FILE_CONTENTS)
    private String contents;
    @StringAttribute(name = Strings.JSON_FILE_NAME)
    private String fileName;
    public File(){}
    public File(String fileName, byte[] fileContents, String aesKey) throws CryptographyException {
        contents = AES.encrypt(aesKey, fileContents);
        this.fileName = fileName;
    }
    public byte[] getFileContents(String aesKey) throws CryptographyException {
        return AES.decrypt(aesKey, contents);
    }

    public String getFileName() {
        return fileName;
    }
}
