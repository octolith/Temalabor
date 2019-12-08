package hu.bme.iit.nfc.lockers.Domain;

import android.app.Activity;

import hu.bme.iit.nfc.lockers.Model.Locker;

import java.security.Key;
import java.security.PublicKey;

import javax.crypto.spec.SecretKeySpec;

import io.jsonwebtoken.SignatureAlgorithm;

public class LockerProcess {

    private static LockerProcess instance = null;

    public void setLocker(Locker locker) {
        currentLocker = locker;
    }

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

    private LockerProcess() {
        key = new SecretKeySpec(keyString.getBytes(), signatureAlgorithm.getJcaName());
        processState = ProcessState.NONE;
    }

    public static LockerProcess getInstance() {
        if (instance == null) {
            instance = new LockerProcess();
        }

        return instance;
    }

}
