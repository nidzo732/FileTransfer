package com.nidzo.filetransfer.transferclasses;

import com.nidzo.filetransfer.JSON.StringAttribute;
import com.nidzo.filetransfer.Strings;
import com.nidzo.filetransfer.cryptography.CryptographyException;
import com.nidzo.filetransfer.cryptography.Signatures;

public class PublicKey {
    @StringAttribute(name = Strings.JSON_PUBLICKEY)
    private String key;
    @StringAttribute(name = Strings.JSON_SIGNATURE)
    private String signature;
    public PublicKey()
    {

    }
    public PublicKey(String key, String signingSecret) throws CryptographyException {
        this.key=key;
        signature = Signatures.generateSignature(key, signingSecret);
    }
    public boolean verifySignature(String signingSecret) throws CryptographyException {
        return Signatures.verifySignature(getKey(), signingSecret, signature);
    }

    public String getKey() {
        return key;
    }
}
