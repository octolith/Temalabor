package hu.bme.iit.nfc.lockers.Domain;

import hu.bme.iit.nfc.lockers.Model.Locker;

import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.spec.SecretKeySpec;

import io.jsonwebtoken.SignatureAlgorithm;

public class LockerProcess {

    public void setLocker(Locker locker) {
        currentLocker = locker;
    }

    private static LockerProcess single_instance = null;

    public Key getKey() {
        return key;
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

    private LockerProcess() {
        key = new SecretKeySpec(keyString.getBytes(), signatureAlgorithm.getJcaName());
        processState = ProcessState.NONE;
    }

    public static LockerProcess getInstance() {
        if (single_instance == null) {
            single_instance = new LockerProcess();
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
