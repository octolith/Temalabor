package hu.bme.iit.nfc.lockers.Domain;

import android.content.Intent;
import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import hu.bme.iit.nfc.lockers.Database.LockerDatabase;
import hu.bme.iit.nfc.lockers.Model.Locker;
import hu.bme.iit.nfc.lockers.Utils.AppExecutors;

import java.util.Arrays;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

import java.security.Key;
import java.util.List;

public class CardService extends HostApduService {
    // ISO-DEP command HEADER for selecting an AID.
    // Format: [Class | Instruction | Parameter 1 | Parameter 2]
    private static final String SELECT_APDU_HEADER = "00A40400";
    // "OK" status word sent in response to SELECT AID command (0x9000)
    private static final byte[] SELECT_OK_SW = HexStringToByteArray("9000");
    private String fullIncomingData = "";
    private String outgoingDataFull = "";
    private List<String> outgoingDataPart;
    private int totalOutgoingDataParts = 0;
    private int currentOutgoingDataPart = 0;
    private RSACipher rsaCipher;
    private LockerDatabase lockerDatabase;
    private LocalBroadcastManager localBroadcastManager;

    /**
     * Called if the connection to the NFC card is lost, in order to let the application know the
     * cause for the disconnection (either a lost link, or another AID being selected by the
     * reader).
     *
     * @param reason Either DEACTIVATION_LINK_LOSS or DEACTIVATION_DESELECTED
     */
    @Override
    public void onDeactivated(int reason) { }

    /**
     * This method will be called when a command APDU has been received from a remote device. A
     * response APDU can be provided directly by returning a byte-array in this method. In general
     * response APDUs must be sent as quickly as possible, given the fact that the user is likely
     * holding his device over an NFC reader when this method is called.
     *
     * <p class="note">If there are multiple services that have registered for the same AIDs in
     * their meta-data entry, you will only get called if the user has explicitly selected your
     * service, either as a default or just for the next tap.
     *
     * <p class="note">This method is running on the main thread of your application. If you
     * cannot return a response APDU immediately, return null and use the {@link
     * #sendResponseApdu(byte[])} method later.
     *
     * @param commandApdu The APDU received from the remote device
     * @param extras A bundle containing extra data. May be null.
     * @return a byte-array containing the response APDU, or null if no response APDU can be sent
     * at this point.
     */

    @Override
    public byte[] processCommandApdu(byte[] commandApdu, Bundle extras) {
        localBroadcastManager = LocalBroadcastManager.getInstance(CardService.this);
        LockerProcess lockerProcessInstance = LockerProcess.getInstance();
        lockerDatabase = LockerDatabase.getInstance(getApplicationContext());
        Key jwtKey = lockerProcessInstance.getKey();

        rsaCipher = RSACipher.getInstance();

        String apduHexString = ByteArrayToHexString(commandApdu);
        byte[] commandSent;

        String partIncomingData = new String(commandApdu);

        // 1. eset: SELECT APDU
        if(apduHexString.startsWith(SELECT_APDU_HEADER)) {
            commandSent = ConcatArrays(SELECT_OK_SW, HexStringToByteArray("00"));
            Log.w("SELECTAPDU", "Select APDU");
        }
        // 2. eset: kimenő stream, OK válasz jött
        else if(partIncomingData.substring(0, 2).equals("OK")) {
            Log.w("OUTGOINGSTREAM", outgoingDataFull);
            commandSent = ConcatArrays(Integer.toString(totalOutgoingDataParts).getBytes(), Integer.toString(currentOutgoingDataPart).getBytes());
            commandSent = ConcatArrays(commandSent, outgoingDataPart.get(currentOutgoingDataPart-1).getBytes());
            commandSent = ConcatArrays(commandSent, HexStringToByteArray("00"));
            currentOutgoingDataPart++;
        }
        // 3. eset: bejövő stream, valami adat jött
        else {
            try {
                // adatmennyiség meghatározása
                int currentIncomingDataPart = Character.getNumericValue(partIncomingData.charAt(0));
                int totalIncomingDataParts = Character.getNumericValue(partIncomingData.charAt(1));
                partIncomingData = partIncomingData.substring(2);

                Log.w("CURRENTINCOMINGPART", Integer.toString(currentIncomingDataPart));
                Log.w("TOTALINCOMINGPARTS", Integer.toString(totalIncomingDataParts));
                Log.w("INCOMINGSTREAM", partIncomingData);

                // az első szeletnél a fullDatát ürítem
                if (currentIncomingDataPart == 1) {
                    fullIncomingData = "";
                }
                // út közben csak hozzáfűzöm a fullDatához és küldök egy OK-t
                if (currentIncomingDataPart < totalIncomingDataParts) {
                    fullIncomingData += partIncomingData;
                    commandSent = ConcatArrays("OK".getBytes(), HexStringToByteArray("00"));
                }
                // egyébként hozzáfűzöm a fullDatához és előállítom a JWTS-t
                else {
                    // a végén is kell még az adatot fűzni
                    fullIncomingData += partIncomingData;

                    Log.w("FULLINCOMINGDATA", fullIncomingData);

                    Jws<Claims> incomingJwts;
                    String outgoingJwts = "";

                    try {
                        incomingJwts = Jwts.parser()         // (1)
                                .setSigningKey(jwtKey)         // (2)
                                .parseClaimsJws(fullIncomingData);      // (3)

                        // we can safely trust the JWT
                        Log.i("JWTS", "Valid");

                        String incomingSubject = incomingJwts.getBody().getSubject();
                        String incomingId;
                        String incomingAudience;
                        String incomingPublicKey;
                        String incomingTest;

                        Log.i("JWTS", incomingJwts.getBody().toString());

                        switch (lockerProcessInstance.getProcessState()) {

                            case NONE:
                                outgoingJwts = Jwts.builder().setSubject("failed")
                                        .signWith(jwtKey, lockerProcessInstance.getSignatureAlgorithm())
                                        .compact();
                                localBroadcastManager.sendBroadcast(new Intent("hu.bme.iit.nfc.lockers.nfcactivity.close"));
                                break;

                            case STARTINGRESERVE:
                                if(incomingSubject.equals("challenge")) {
                                    incomingPublicKey = incomingJwts.getBody().getIssuer();
                                    Log.d("ServerKey", incomingPublicKey);
                                    try {
                                        lockerProcessInstance.setServerPublicKey(RSACipher.stringToPublicKey(incomingPublicKey));
                                    } catch (Exception ex) {
                                        Log.d("ServerKey", "Conversion failed", ex);
                                        throw ex;
                                    }
                                    outgoingJwts = Jwts.builder().setSubject("no")
                                            .setIssuer(rsaCipher.getPublicKey("pkcs8-pem"))
                                            .signWith(jwtKey, lockerProcessInstance.getSignatureAlgorithm())
                                            .compact();
                                    lockerProcessInstance.setProcessState(ProcessState.CHALLENGERESERVE);
                                }
                                else {
                                        outgoingJwts = Jwts.builder().setSubject("failed").signWith(jwtKey, lockerProcessInstance.getSignatureAlgorithm()).compact();
                                        lockerProcessInstance.setProcessState(ProcessState.NONE);
                                }
                                break;
                            case CHALLENGERESERVE:
                                if(incomingSubject.equals("select")) {
                                        outgoingJwts = Jwts.builder().setSubject("reserve")
                                                .signWith(jwtKey, lockerProcessInstance.getSignatureAlgorithm())
                                                .compact();
                                        lockerProcessInstance.setProcessState(ProcessState.RESERVING);
                                        Log.d("Reserve", "Reserving");
                                }
                                else {
                                        Log.e("Reserve", "Failed, " + incomingSubject);
                                        outgoingJwts = Jwts.builder().setSubject("failed").signWith(jwtKey, lockerProcessInstance.getSignatureAlgorithm()).compact();
                                        lockerProcessInstance.setProcessState(ProcessState.NONE);
                                }
                                break;
                            case RESERVING:
                                if(incomingSubject.equals("success")) {
                                        incomingId = incomingJwts.getBody().getId();
                                        Log.w("Decrypt", incomingId);
                                        String decrypted;
                                        try {
                                            decrypted = rsaCipher.decrypt(incomingId);
                                            Log.d("Decrypt", "Token: " + decrypted);
                                        }
                                        catch (Exception ex) {
                                            Log.e("Decrypt", "Decrypt unsuccessful", ex);
                                            throw ex;
                                        }

                                        incomingAudience = incomingJwts.getBody().getAudience();
                                        final Locker tempLocker = new Locker(decrypted, incomingAudience);
                                        AppExecutors.getInstance().diskIO().execute(() -> lockerDatabase.lockerDao().insertAll(tempLocker));

                                        lockerProcessInstance.setProcessState(ProcessState.SAVING);

                                        String encrypted;
                                        try {
                                            encrypted = rsaCipher.encrypt(decrypted, lockerProcessInstance.getServerPublicKey());
                                            Log.d("Encrypt", "Successful");
                                        }
                                        catch (Exception ex) {
                                            Log.e("Encrypt", "Unsuccessful", ex);
                                            throw ex;
                                        }
                                        Log.d("Encrypt", "Token: " + encrypted);
                                        outgoingJwts = Jwts.builder()
                                                .setSubject("success")
                                                .setId(encrypted)
                                                .setAudience(incomingAudience)
                                                .signWith(jwtKey, lockerProcessInstance.getSignatureAlgorithm()).compact();
                                }
                                else {
                                        outgoingJwts = Jwts.builder().setSubject("failed")
                                                .signWith(jwtKey, lockerProcessInstance.getSignatureAlgorithm())
                                                .compact();
                                        Log.w("Reserve", "Unsuccessful");
                                        lockerProcessInstance.setProcessState(ProcessState.NONE);
                                }
                                break;

                            case SAVING:
                                if(incomingSubject.equals("done")) {
                                        outgoingJwts = Jwts.builder().setSubject("done")
                                                .signWith(jwtKey, lockerProcessInstance.getSignatureAlgorithm())
                                                .compact();
                                        Toast.makeText(this.getApplicationContext(),
                                                "Success!",
                                                Toast.LENGTH_SHORT).show();
                                        localBroadcastManager.sendBroadcast(new Intent("hu.bme.iit.nfc.lockers.nfcactivity.close"));
                                        lockerProcessInstance.setProcessState(ProcessState.DONE);
                                }
                                else {
                                        outgoingJwts = Jwts.builder().setSubject("done")
                                                .signWith(jwtKey, lockerProcessInstance.getSignatureAlgorithm())
                                                .compact();
                                        lockerProcessInstance.setProcessState(ProcessState.NONE);
                                }
                                break;
                            case DONE:
                                if(incomingSubject.equals("done")) {
                                        outgoingJwts = Jwts.builder().setSubject("done").signWith(jwtKey, lockerProcessInstance.getSignatureAlgorithm()).compact();
                                        lockerProcessInstance.setProcessState(ProcessState.DONE);
                                        localBroadcastManager.sendBroadcast(new Intent("hu.bme.iit.nfc.lockers.nfcactivity.close"));
                                }
                                else {
                                        outgoingJwts = Jwts.builder().setSubject("done").signWith(jwtKey, lockerProcessInstance.getSignatureAlgorithm()).compact();
                                        lockerProcessInstance.setProcessState(ProcessState.NONE);
                                }
                                break;
                            case STARTINGOPEN:
                                if(incomingSubject.equals("challenge")) {
                                        incomingPublicKey = incomingJwts.getBody().getIssuer();
                                        Log.d("ServerKey", incomingPublicKey);
                                        try {
                                            lockerProcessInstance.setServerPublicKey(RSACipher.stringToPublicKey(incomingPublicKey));
                                        }
                                        catch (Exception ex) {
                                            Log.d("ServerKey", "Conversion failed", ex);
                                            throw ex;
                                        }
                                        outgoingJwts = Jwts.builder()
                                                .setSubject("yes")
                                                .setIssuer(rsaCipher.getPublicKey("pkcs8-pem"))
                                                .setAudience(lockerProcessInstance.getLocker().getNumber())
                                                .signWith(jwtKey, lockerProcessInstance.getSignatureAlgorithm()).compact();
                                        lockerProcessInstance.setProcessState(ProcessState.CHALLENGEOPEN);
                                        Log.w("OPENCHALLENGE", "YES");
                                }
                                else {
                                        outgoingJwts = Jwts.builder().setSubject("failed").signWith(jwtKey, lockerProcessInstance.getSignatureAlgorithm()).compact();
                                        lockerProcessInstance.setProcessState(ProcessState.NONE);
                                }
                                break;
                            case CHALLENGEOPEN:
                                if(incomingSubject.equals("decode")) {
                                        incomingTest = incomingJwts.getBody().getIssuer();
                                        String decrypted;
                                        try {
                                            decrypted = rsaCipher.decrypt(incomingTest);
                                        }
                                        catch (Exception ex) {
                                            Log.e("Decrypt", "Unsuccessful", ex);
                                            throw ex;
                                        }
                                        String encrypted;
                                        try {
                                            encrypted = rsaCipher.encrypt(decrypted, lockerProcessInstance.getServerPublicKey());
                                        }
                                        catch (Exception ex) {
                                            Log.e("Enrypt", "Unsuccessful", ex);
                                            throw ex;
                                        }
                                        outgoingJwts = Jwts.builder()
                                                .setSubject("response")
                                                .setIssuer(encrypted)
                                                .signWith(jwtKey, lockerProcessInstance.getSignatureAlgorithm())
                                                .compact();
                                        lockerProcessInstance.setProcessState(ProcessState.CHALLENGEOPENSUCCESS);
                                }
                                else {
                                        outgoingJwts = Jwts.builder().setSubject("failed").signWith(jwtKey, lockerProcessInstance.getSignatureAlgorithm()).compact();
                                        lockerProcessInstance.setProcessState(ProcessState.NONE);
                                }
                                break;
                            case CHALLENGEOPENSUCCESS:
                                if(incomingSubject.equals("select")) {
                                        String encrypted;
                                        try {
                                            encrypted = rsaCipher.encrypt(
                                                    lockerProcessInstance.getLocker().getTokenString(),
                                                    lockerProcessInstance.getServerPublicKey());
                                        }
                                        catch (Exception ex) {
                                            Log.e("Encrypt", "unsuccessful", ex);
                                            throw ex;
                                        }
                                        outgoingJwts = Jwts.builder()
                                                .setSubject("open")
                                                .setId(encrypted)
                                                .setAudience(lockerProcessInstance.getLocker().getNumber())
                                                .signWith(jwtKey, lockerProcessInstance.getSignatureAlgorithm())
                                                .compact();
                                        lockerProcessInstance.setProcessState(ProcessState.OPENING);
                                }
                                else {
                                        outgoingJwts = Jwts.builder().setSubject("failed")
                                                .signWith(jwtKey, lockerProcessInstance.getSignatureAlgorithm())
                                                .compact();
                                        lockerProcessInstance.setProcessState(ProcessState.NONE);
                                }
                                break;
                            case OPENING:
                                if(incomingSubject.equals("success")) {
                                        outgoingJwts = Jwts.builder().setSubject("acknowledged")
                                                .signWith(jwtKey, lockerProcessInstance.getSignatureAlgorithm())
                                                .compact();
                                        lockerProcessInstance.setProcessState(ProcessState.SAVING);
                                }
                                else {
                                        outgoingJwts = Jwts.builder().setSubject("failed")
                                                .signWith(jwtKey, lockerProcessInstance.getSignatureAlgorithm())
                                                .compact();
                                        lockerProcessInstance.setProcessState(ProcessState.NONE);
                                }
                                break;
                            case STARTINGCLOSE:
                                if(incomingSubject.equals("challenge")) {
                                        incomingPublicKey = incomingJwts.getBody().getIssuer();
                                        Log.d("ServerKey", incomingPublicKey);
                                        try {
                                            lockerProcessInstance.setServerPublicKey(RSACipher.stringToPublicKey(incomingPublicKey));
                                        }
                                        catch (Exception ex) {
                                            Log.d("ServerKey", "Conversion failed", ex);
                                            throw ex;
                                        }
                                        outgoingJwts = Jwts.builder()
                                                .setSubject("yes")
                                                .setIssuer(rsaCipher.getPublicKey("pkcs8-pem"))
                                                .setAudience(lockerProcessInstance.getLocker().getNumber())
                                                .signWith(jwtKey, lockerProcessInstance.getSignatureAlgorithm())
                                                .compact();
                                        lockerProcessInstance.setProcessState(ProcessState.CHALLENGECLOSE);
                                        Log.w("CLOSECHALLENGE", "YES");
                                }
                                else {
                                        outgoingJwts = Jwts.builder().setSubject("failed")
                                                .signWith(jwtKey, lockerProcessInstance.getSignatureAlgorithm())
                                                .compact();
                                        lockerProcessInstance.setProcessState(ProcessState.NONE);
                                }
                                break;
                            case CHALLENGECLOSE:
                                if(incomingSubject.equals("decode")) {
                                        incomingTest = incomingJwts.getBody().getIssuer();
                                        String decrypted;
                                        try {
                                            decrypted = rsaCipher.decrypt(incomingTest);
                                        }
                                        catch (Exception ex) {
                                            Log.e("Decrypt", "Unsuccessful", ex);
                                            throw ex;
                                        }
                                        String encrypted;
                                        try {
                                            encrypted = rsaCipher.encrypt(decrypted, lockerProcessInstance.getServerPublicKey());
                                        }
                                        catch (Exception ex) {
                                            Log.e("Enrypt", "Unsuccessful", ex);
                                            throw ex;
                                        }
                                        outgoingJwts = Jwts.builder()
                                                .setSubject("response")
                                                .setIssuer(encrypted)
                                                .signWith(jwtKey, lockerProcessInstance.getSignatureAlgorithm())
                                                .compact();
                                        lockerProcessInstance.setProcessState(ProcessState.CHALLENGECLOSESUCCESS);
                                }
                                else {
                                        outgoingJwts = Jwts.builder().setSubject("failed")
                                                .signWith(jwtKey, lockerProcessInstance.getSignatureAlgorithm())
                                                .compact();
                                        lockerProcessInstance.setProcessState(ProcessState.NONE);
                                }
                                break;
                            case CHALLENGECLOSESUCCESS:
                                if(incomingSubject.equals("select")) {
                                        String encrypted;
                                        try {
                                            encrypted = rsaCipher.encrypt(
                                                    lockerProcessInstance.getLocker().getTokenString(),
                                                    lockerProcessInstance.getServerPublicKey());
                                        }
                                        catch (Exception ex) {
                                            Log.e("Encrypt", "unsuccessful", ex);
                                            throw ex;
                                        }
                                        outgoingJwts = Jwts.builder()
                                                .setSubject("close")
                                                .setId(encrypted)
                                                .setAudience(lockerProcessInstance.getLocker().getNumber())
                                                .signWith(jwtKey, lockerProcessInstance.getSignatureAlgorithm()).compact();
                                        lockerProcessInstance.setProcessState(ProcessState.CLOSING);
                                }
                                else {
                                        outgoingJwts = Jwts.builder().setSubject("failed").signWith(jwtKey, lockerProcessInstance.getSignatureAlgorithm()).compact();
                                        lockerProcessInstance.setProcessState(ProcessState.NONE);
                                }
                                break;
                            case CLOSING:
                                if(incomingSubject.equals("success")) {
                                        outgoingJwts = Jwts.builder().setSubject("acknowledged")
                                                .signWith(jwtKey, lockerProcessInstance.getSignatureAlgorithm()).compact();
                                        lockerProcessInstance.setProcessState(ProcessState.SAVING);
                                }
                                else {
                                        outgoingJwts = Jwts.builder().setSubject("failed")
                                                .signWith(jwtKey, lockerProcessInstance.getSignatureAlgorithm()).compact();
                                        lockerProcessInstance.setProcessState(ProcessState.NONE);
                                }
                                break;
                            case STARTINGRELEASE:
                                if(incomingSubject.equals("challenge")) {
                                        incomingPublicKey = incomingJwts.getBody().getIssuer();
                                        Log.d("ServerKey", incomingPublicKey);
                                        try {
                                            lockerProcessInstance.setServerPublicKey(RSACipher.stringToPublicKey(incomingPublicKey));
                                        }
                                        catch (Exception ex) {
                                            Log.d("ServerKey", "Conversion failed", ex);
                                            throw ex;
                                        }
                                        outgoingJwts = Jwts.builder()
                                                .setSubject("yes")
                                                .setIssuer(rsaCipher.getPublicKey("pkcs8-pem"))
                                                .setAudience(lockerProcessInstance.getLocker().getNumber())
                                                .signWith(jwtKey, lockerProcessInstance.getSignatureAlgorithm()).compact();
                                        lockerProcessInstance.setProcessState(ProcessState.CHALLENGERELEASE);
                                        Log.w("RELEASECHALLENGE", "YES");
                                }
                                else {
                                        outgoingJwts = Jwts.builder().setSubject("failed").signWith(jwtKey, lockerProcessInstance.getSignatureAlgorithm()).compact();
                                        lockerProcessInstance.setProcessState(ProcessState.NONE);
                                }
                                break;
                            case CHALLENGERELEASE:
                                if(incomingSubject.equals("decode")) {
                                        incomingTest = incomingJwts.getBody().getIssuer();
                                        String decrypted;
                                        try {
                                            decrypted = rsaCipher.decrypt(incomingTest);
                                        }
                                        catch (Exception ex) {
                                            Log.e("Decrypt", "Unsuccessful", ex);
                                            throw ex;
                                        }
                                        String encrypted;
                                        try {
                                            encrypted = rsaCipher.encrypt(decrypted, lockerProcessInstance.getServerPublicKey());
                                        }
                                        catch (Exception ex) {
                                            Log.e("Enrypt", "Unsuccessful", ex);
                                            throw ex;
                                        }
                                        outgoingJwts = Jwts.builder()
                                                .setSubject("response")
                                                .setIssuer(encrypted)
                                                .signWith(jwtKey, lockerProcessInstance.getSignatureAlgorithm())
                                                .compact();
                                        lockerProcessInstance.setProcessState(ProcessState.CHALLENGERELEASESUCCESS);
                                }
                                else {
                                    outgoingJwts = Jwts.builder().setSubject("failed").signWith(jwtKey, lockerProcessInstance.getSignatureAlgorithm()).compact();
                                    lockerProcessInstance.setProcessState(ProcessState.NONE);
                                }
                                break;
                            case CHALLENGERELEASESUCCESS:
                                if(incomingSubject.equals("select")) {
                                        String encrypted;
                                        try {
                                            encrypted = rsaCipher.encrypt(lockerProcessInstance.getLocker().getTokenString(), lockerProcessInstance.getServerPublicKey());
                                        }
                                        catch (Exception ex) {
                                            Log.e("Encrypt", "unsuccessful", ex);
                                            throw ex;
                                        }
                                        outgoingJwts = Jwts.builder()
                                                .setSubject("release")
                                                .setId(encrypted)
                                                .setAudience(lockerProcessInstance.getLocker().getNumber())
                                                .signWith(jwtKey, lockerProcessInstance.getSignatureAlgorithm()).compact();
                                        lockerProcessInstance.setProcessState(ProcessState.RELEASING);
                                }
                                else {
                                    outgoingJwts = Jwts.builder().setSubject("failed").signWith(jwtKey, lockerProcessInstance.getSignatureAlgorithm()).compact();
                                    lockerProcessInstance.setProcessState(ProcessState.NONE);
                                }
                                break;
                            case RELEASING:
                                if(incomingSubject.equals("success")) {
                                        outgoingJwts = Jwts.builder().setSubject("acknowledged").signWith(jwtKey, lockerProcessInstance.getSignatureAlgorithm()).compact();
                                        AppExecutors.getInstance().diskIO().execute(() -> lockerDatabase.lockerDao().delete(LockerProcess.getInstance().getLocker()));

                                        lockerProcessInstance.setProcessState(ProcessState.SAVING);
                                }
                                else {
                                    outgoingJwts = Jwts.builder().setSubject("failed").signWith(jwtKey, lockerProcessInstance.getSignatureAlgorithm()).compact();
                                    lockerProcessInstance.setProcessState(ProcessState.NONE);
                                }
                                break;
                            default:
                                outgoingJwts = Jwts.builder().setSubject("failed").signWith(jwtKey, lockerProcessInstance.getSignatureAlgorithm()).compact();
                                lockerProcessInstance.setProcessState(ProcessState.NONE);
                                Log.e("State", "Inconsistent");
                                break;
                        }
                    }
                    catch(JwtException jex) {
                        outgoingJwts = Jwts.builder().setSubject("failed").signWith(jwtKey, lockerProcessInstance.getSignatureAlgorithm()).compact();
                        lockerProcessInstance.setProcessState(ProcessState.NONE);
                    }
                    catch (Exception ex) {     // (4)
                        outgoingJwts = Jwts.builder().setSubject("failed").signWith(jwtKey, lockerProcessInstance.getSignatureAlgorithm()).compact();
                        lockerProcessInstance.setProcessState(ProcessState.NONE);
                        Toast.makeText(this.getApplicationContext(),
                                "Failed, try again!",
                                Toast.LENGTH_LONG).show();
                    }
                    finally {
                        outgoingDataFull = outgoingJwts;
                        Iterable<String> pieces = Splitter.fixedLength(220).split(outgoingDataFull);
                        outgoingDataPart = Lists.newArrayList(pieces);
                        totalOutgoingDataParts = outgoingDataPart.size();
                        currentOutgoingDataPart = 1;
                        commandSent = ConcatArrays(Integer.toString(totalOutgoingDataParts).getBytes(), Integer.toString(currentOutgoingDataPart).getBytes());
                        commandSent = ConcatArrays(commandSent, outgoingDataPart.get(currentOutgoingDataPart-1).getBytes());
                        commandSent = ConcatArrays(commandSent, HexStringToByteArray("00"));
                        currentOutgoingDataPart++;
                    }
                }
            }
            catch(Exception ex) {
                Log.e("CardServiceException", "Exception during command processing", ex);
                Toast.makeText(this.getApplicationContext(),
                        "Failed, try again!",
                        Toast.LENGTH_SHORT).show();
                commandSent = HexStringToByteArray("0000");
            }
        }
        Log.w("COMMANDSENT", new String(commandSent));
        return commandSent;
    }

    /**
     * Utility method to convert a byte array to a hexadecimal string.
     *
     * @param bytes Bytes to convert
     * @return String, containing hexadecimal representation.
     */
    public static String ByteArrayToHexString(byte[] bytes) {
        final char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
        char[] hexChars = new char[bytes.length * 2]; // Each byte has two hex characters (nibbles)
        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF; // Cast bytes[j] to int, treating as unsigned value
            hexChars[j * 2] = hexArray[v >>> 4]; // Select hex character from upper nibble
            hexChars[j * 2 + 1] = hexArray[v & 0x0F]; // Select hex character from lower nibble
        }
        return new String(hexChars);
    }

    /**
     * Utility method to convert a hexadecimal string to a byte string.
     *
     * <p>Behavior with input strings containing non-hexadecimal characters is undefined.
     *
     * @param s String containing hexadecimal characters to convert
     * @return Byte array generated from input
     * @throws IllegalArgumentException if input length is incorrect
     */
    public static byte[] HexStringToByteArray(String s) throws IllegalArgumentException {
        int len = s.length();
        if (len % 2 == 1) {
            throw new IllegalArgumentException("Hex string must have even number of characters");
        }
        byte[] data = new byte[len / 2]; // Allocate 1 byte per 2 hex characters
        for (int i = 0; i < len; i += 2) {
            // Convert each character into a integer (base-16), then bit-shift into place
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    /**
     * Utility method to concatenate two byte arrays.
     * @param first First array
     * @param rest Any remaining arrays
     * @return Concatenated copy of input arrays
     */
    public static byte[] ConcatArrays(byte[] first, byte[]... rest) {
        int totalLength = first.length;
        for (byte[] array : rest) {
            totalLength += array.length;
        }
        byte[] result = Arrays.copyOf(first, totalLength);
        int offset = first.length;
        for (byte[] array : rest) {
            System.arraycopy(array, 0, result, offset, array.length);
            offset += array.length;
        }
        return result;
    }
}
