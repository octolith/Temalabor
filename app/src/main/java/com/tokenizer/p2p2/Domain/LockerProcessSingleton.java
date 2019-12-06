package com.tokenizer.p2p2.Domain;

import com.tokenizer.p2p2.Model.Locker;

import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.spec.SecretKeySpec;

import io.jsonwebtoken.SignatureAlgorithm;

public class LockerProcessSingleton {

    public void setLocker(Locker locker) {
        currentLocker = locker;
        tokenString = locker.getTokenString();
    }

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

    public Locker getLocker() { return currentLocker; }

    private final String keyString = "9qVxRa4e47CRtPf27Zph4ruDyH6wYq8u8eTkTRGYefLJ64mF";
    private Key key;
    private String tokenString;
    private SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;
    private ProcessState processState;
    private Locker currentLocker;

    public PublicKey getServerPublicKey() {
        return serverPublicKey;
    }

    public void setServerPublicKey(PublicKey serverPublicKey) {
        this.serverPublicKey = serverPublicKey;
    }

    private PublicKey serverPublicKey;

    public String getServerPublicKeyString() {
        return serverPublicKeyString;
    }

    public void setServerPublicKeyString(String serverPublicKeyString) {
        this.serverPublicKeyString = serverPublicKeyString;
    }

    private String serverPublicKeyString;
    private String clientPrivateKeyString;

    public String getClientPrivateKeyString() {
        return clientPrivateKeyString;
    }

    public void setClientPrivateKeyString(String clientPrivateKeyString) {
        this.clientPrivateKeyString = clientPrivateKeyString;
    }

    public String getClientPublicKeyString() {
        return clientPublicKeyString;
    }

    public void setClientPublicKeyString(String clientPublicKeyString) {
        this.clientPublicKeyString = clientPublicKeyString;
    }

    public PrivateKey getClientPrivateKey() {
        return clientPrivateKey;
    }

    public void setClientPrivateKey(PrivateKey clientPrivateKey) {
        this.clientPrivateKey = clientPrivateKey;
    }

    public PublicKey getClientPublicKey() {
        return clientPublicKey;
    }

    public void setClientPublicKey(PublicKey clientPublicKey) {
        this.clientPublicKey = clientPublicKey;
    }

    private String clientPublicKeyString;
    private PrivateKey clientPrivateKey;
    private PublicKey clientPublicKey;

    private LockerProcessSingleton() {
        key = new SecretKeySpec(keyString.getBytes(), signatureAlgorithm.getJcaName());
        tokenString = "";
        processState = ProcessState.NONE;
        /*
        clientPrivateKeyString =
            "-----BEGIN RSA PRIVATE KEY-----\n" +
            "MIICXQIBAAKBgQDk5/u0rNk+zn/A9f65JxLq+Y/NmeiVS9GroRzmaCL4BkAqoUs7\n" +
            "k9TT+xPHJcpuNG9V1oyGBvdtOqSuUSvbSa7cbEXw+vdD0WQOQd1fWZMOfOjLYrVR\n" +
            "xohwXtUoOI9Tlw7yXB1I2R0uln1QImw+c6Y8AdWtDExxMfxPbnhTYmTRhQIDAQAB\n" +
            "AoGAPWul1V//1uNv0iXvmFzMrbITDPb8OnsiomvTpltQ7r42n3myByVumQao0unn\n" +
            "/FQjf6PXBut9nph6sa9kFi4aNdcKRMhPAABlBmWF1lNmaqWJh2BAEOCW7D3crh4t\n" +
            "tyu8F0iixXlrCGAvEKlGOkmtFbL85RQn6xtWK8ztOF+lg40CQQDzX/kHmgeOmpxE\n" +
            "Bu3phKsPF3yXNNtb12UEwprI0LDJkOTkp0W6C4NEfhtlodP07HCMdN8t0qJmgui+\n" +
            "aNaqXR3PAkEA8MfdkDpzv+0HGYmyPvEVLl80XG72UAZEvfg7byK7EGk8DkeV59CG\n" +
            "misZp6d/gN/5fUn5+odi7pG19DDkooPkawJBANQyKJoFJXOjwH3boNILF254Juxb\n" +
            "bKr9+ZkV6AkRVCLaz4fBhJz67r/oSBDr8TdKc7MzL2fvkCNbHnzuQcSWnacCQDp7\n" +
            "cRdM+zxMqALN7RtYlxpySVeCJBV/0EaL+nOd7e2ogcu+G2z4uxjLCyorhs6YmBKU\n" +
            "W0E8jQ0BGz66eW33tw0CQQCgLDkVb3ihPw37YD8GQ89ANXB6G1ed2FdOqtKBoJbF\n" +
            "B9QFAGtj3rxs4kgpfl70CuEkY7UX7iDrjCSeep5yuzI3\n" +
            "-----END RSA PRIVATE KEY-----\n";
        clientPublicKeyString =
            "-----BEGIN PUBLIC KEY-----\n" +
            "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDk5/u0rNk+zn/A9f65JxLq+Y/N\n" +
            "meiVS9GroRzmaCL4BkAqoUs7k9TT+xPHJcpuNG9V1oyGBvdtOqSuUSvbSa7cbEXw\n" +
            "+vdD0WQOQd1fWZMOfOjLYrVRxohwXtUoOI9Tlw7yXB1I2R0uln1QImw+c6Y8AdWt\n" +
            "DExxMfxPbnhTYmTRhQIDAQAB\n" +
            "-----END PUBLIC KEY-----\n";
        try {
            clientPrivateKey = RSACipher.stringToPrivate(clientPrivateKeyString);
            clientPublicKey = RSACipher.stringToPublicKey(clientPublicKeyString);
        }
        catch(Exception e) {

        }
        */
    }

    public static LockerProcessSingleton getInstance() {
        if (single_instance == null) {
            single_instance = new LockerProcessSingleton();
        }

        return single_instance;
    }

    public void reserveLocker(String id, String number) {
        currentLocker = new Locker(id, number);
    }

    public Locker releaseLocker() {
        Locker temp = currentLocker;
        currentLocker = null;
        return temp;
    }
}
