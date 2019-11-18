package com.tokenizer.p2p2;

import java.security.Key;
import javax.crypto.spec.SecretKeySpec;

import io.jsonwebtoken.SignatureAlgorithm;

public class LockerProcessSingleton {

    private static LockerProcessSingleton single_instance = null;

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

    public ProcessState getProcessState() {
        return processState;
    }

    public void setProcessState(ProcessState value) {
        processState = value;
    }

    public Locker getReservedLocker() { return reservedLocker; }

    private final String keyString = "9qVxRa4e47CRtPf27Zph4ruDyH6wYq8u8eTkTRGYefLJ64mF";
    private Key key;
    private String tokenString;
    private SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;
    private ProcessState processState;
    private Locker reservedLocker;

    private LockerProcessSingleton() {
        key = new SecretKeySpec(keyString.getBytes(), signatureAlgorithm.getJcaName());
        tokenString = "";
        processState = ProcessState.NONE;
    }

    public static LockerProcessSingleton getInstance() {
        if (single_instance == null) {
            single_instance = new LockerProcessSingleton();
        }

        return single_instance;
    }

    public void reserveLocker(String id, String number) {
        reservedLocker = new Locker(id, number);
    }

    public Locker releaseLocker() {
        Locker temp = reservedLocker;
        reservedLocker = null;
        return temp;
    }
}
