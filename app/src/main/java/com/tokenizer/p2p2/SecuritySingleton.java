package com.tokenizer.p2p2;

import java.security.Key;
import javax.crypto.spec.SecretKeySpec;

import io.jsonwebtoken.SignatureAlgorithm;

public class SecuritySingleton {

    private static SecuritySingleton single_instance = null;

    public Key getKey() {
        return key;
    }

    public String getTokenString() {
        return tokenString;
    }

    public void setTokenString(String value) {

        tokenString = value;
    }

    public SignatureAlgorithm getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public LockerCommand getLockerCommand() {
        return lockerCommand;
    }

    public void setLockerCommand(LockerCommand value) {
        lockerCommand = value;
    }

    private final String keyString = "9qVxRa4e47CRtPf27Zph4ruDyH6wYq8u8eTkTRGYefLJ64mF";
    private Key key;
    private String tokenString;
    private SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;
    private LockerCommand lockerCommand;

    private SecuritySingleton() {
        key = new SecretKeySpec(keyString.getBytes(), signatureAlgorithm.getJcaName());
        tokenString = "";
        lockerCommand = LockerCommand.NONE;
    }

    public static SecuritySingleton getInstance() {
        if (single_instance == null) {
            single_instance = new SecuritySingleton();
        }

        return single_instance;
    }


}
