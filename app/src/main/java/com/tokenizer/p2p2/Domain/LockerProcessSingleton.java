package com.tokenizer.p2p2.Domain;

import com.tokenizer.p2p2.Model.Locker;

import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;

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
        clientPrivateKeyString =
                "-----BEGIN RSA PRIVATE KEY-----\n" +
                "MIICXAIBAAKBgQCnaZKTztrOpHVYJ5AtZRgkcK5SLItcNEguCeweCHUjBnF9gvQA\n" +
                "g6jn5fzucnxRZlMv3Pkl37LSS/CAxlkM9udJtGmtaRVEAf4gvRST6/1hTZqdxS1m\n" +
                "dzxKjHscwK7ljypl7zoxz3mEsTjEjv5E2uEDUveEuUqSfcC41X6eZJHHCwIDAQAB\n" +
                "AoGBAJGMdcVP6GgUD3lPHEh7RhwYY1ZKJ/dH9SEhu0j5LW4AIhdvDNiNnwHOsLJW\n" +
                "ksLqZZSNV4eYDw5Ku5QZ3j4pQzcfxvfjX1WjhcT4if7fYYEdErvs4w1mhUgezGEd\n" +
                "gBAWFHvMgLmD95qm4xlqOVR034A9XWBzvrlaQAAg9e9B+8CBAkEA/eGutvPDbcdl\n" +
                "ihDpP7rrVvNhJ49zOft0AZVzD666AGqcxMLHVHp6eW/Di+iMQXJ2aUR5Wrf0kNSZ\n" +
                "UoaogpGaoQJBAKjPLtKp1Qin86EmhEmurRJSVVHX4ZDdPoJk6bUqMv0DTC9qVRT0\n" +
                "v405diDuyDKv3TdZNoPJgclCl8r490aoDisCQF1gLApODYrK06W8Io22CeipCwqp\n" +
                "0A97VGdguDkEYpTUoWQc34NKqNERMjK7DRrlJngBH0Emd2TtTJb1v/SF6QECQBDt\n" +
                "ldhyR3aywO+sGR/6cEiiEQRADdKlHRueWwFb1fkhV4Z5t0Z+tKbI2Fu9Fj5e/GQM\n" +
                "gjb9AhLYprgn90QSnjkCQH7dK7eGbFewFvAUbQ4yCXhOd9FygE/QDooxBxSVDXlX\n" +
                "abZNmeS8qf/eb+isKHuRKNAMZErj/21ey731qfipNmI=\n" +
                "-----END RSA PRIVATE KEY-----";
        clientPublicKeyString =
                "-----BEGIN PUBLIC KEY-----\n" +
                "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCnaZKTztrOpHVYJ5AtZRgkcK5S\n" +
                "LItcNEguCeweCHUjBnF9gvQAg6jn5fzucnxRZlMv3Pkl37LSS/CAxlkM9udJtGmt\n" +
                "aRVEAf4gvRST6/1hTZqdxS1mdzxKjHscwK7ljypl7zoxz3mEsTjEjv5E2uEDUveE\n" +
                "uUqSfcC41X6eZJHHCwIDAQAB\n" +
                "-----END PUBLIC KEY-----";
        try {
            clientPrivateKey = RSACipher.stringToPrivate(clientPrivateKeyString);
            clientPublicKey = RSACipher.stringToPublicKey(clientPublicKeyString);
        }
        catch(Exception e) {

        }

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
