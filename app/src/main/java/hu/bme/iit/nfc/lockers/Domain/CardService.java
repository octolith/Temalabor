package hu.bme.iit.nfc.lockers.Domain;

import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import hu.bme.iit.nfc.lockers.Database.LockerDatabase;
import hu.bme.iit.nfc.lockers.Model.Locker;
import hu.bme.iit.nfc.lockers.Utils.AppExecutors;

import java.util.Arrays;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
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
    // BEGIN_INCLUDE(processCommandApdu)
    @Override
    public byte[] processCommandApdu(byte[] commandApdu, Bundle extras) {
        LockerProcessSingleton lockerProcessInstance = LockerProcessSingleton.getInstance();
        lockerDatabase = LockerDatabase.getInstance(getApplicationContext());
        Key key = lockerProcessInstance.getKey();

        rsaCipher = RSACipher.getInstance();

        String apduHexString = ByteArrayToHexString(commandApdu);
        byte[] commandSent = null;

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
            // adatmennyiség meghatározása
            int currentIncomingDataPart = Character.getNumericValue(partIncomingData.charAt(0));
            int totalIncomingDataParts = Character.getNumericValue(partIncomingData.charAt(1));
            // lezáró nulla levágása
            partIncomingData = partIncomingData.substring(2, partIncomingData.length());

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
                            .setSigningKey(key)         // (2)
                            .parseClaimsJws(fullIncomingData);      // (3)

                    // we can safely trust the JWT
                    Toast.makeText(this.getApplicationContext(),
                            "JWTS valid",
                            Toast.LENGTH_SHORT).show();

                    String incomingSubject = incomingJwts.getBody().getSubject();
                    String incomingId;
                    String incomingAudience;
                    String incomingPublicKey;
                    String incomingTest;
                    Log.i("INCOMINGJWTS", incomingJwts.getBody().toString());
                    switch (lockerProcessInstance.getProcessState()) {
                        case NONE:
                            outgoingJwts = Jwts.builder().setSubject("failed").signWith(key, lockerProcessInstance.getSignatureAlgorithm()).compact();
                            Toast.makeText(this.getApplicationContext(),
                                    "Failure",
                                    Toast.LENGTH_SHORT).show();
                            break;
                        case STARTINGRESERVE:
                            switch (incomingSubject) {
                                case "challenge":
                                    Toast.makeText(this.getApplicationContext(),
                                            "CHALLENGE",
                                            Toast.LENGTH_SHORT).show();
                                    incomingPublicKey = incomingJwts.getBody().getIssuer();
                                    Log.w("SERVERPUBLICKEY", incomingPublicKey);
                                    try {
                                        lockerProcessInstance.setServerPublicKey(rsaCipher.stringToPublicKey(incomingPublicKey));
                                        lockerProcessInstance.setServerPublicKeyString(incomingPublicKey);
                                    }
                                    catch (Exception e) {
                                        Log.w("PUBLICKEYCONVERSION", "FAILED");
                                        Toast.makeText(this.getApplicationContext(),
                                                "Public key conversion failure",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                    outgoingJwts = Jwts.builder().setSubject("no")
                                            .setIssuer(rsaCipher.getPublicKey("pkcs8-pem"))
                                            .signWith(key, lockerProcessInstance.getSignatureAlgorithm())
                                            .compact();
                                    Log.w("RESERVECHALLENGE", "NO");
                                    lockerProcessInstance.setProcessState(ProcessState.CHALLENGERESERVE);
                                    break;
                                default:
                                    outgoingJwts = Jwts.builder().setSubject("failed").signWith(key, lockerProcessInstance.getSignatureAlgorithm()).compact();
                                    lockerProcessInstance.setProcessState(ProcessState.NONE);
                                    break;
                            }
                            break;
                        case CHALLENGERESERVE:
                            switch (incomingSubject) {
                                case "select":
                                    outgoingJwts = Jwts.builder().setSubject("reserve")
                                            .signWith(key, lockerProcessInstance.getSignatureAlgorithm())
                                            .compact();
                                    lockerProcessInstance.setProcessState(ProcessState.RESERVING);
                                    Log.w("RESERVINGLOCKER", "Reserving locker");
                                    break;
                                default:
                                    Log.w("RESERVINGLOCKER", "FAILED, " + incomingSubject);
                                    outgoingJwts = Jwts.builder().setSubject("failed").signWith(key, lockerProcessInstance.getSignatureAlgorithm()).compact();
                                    lockerProcessInstance.setProcessState(ProcessState.NONE);
                                    break;
                            }
                            break;
                        case RESERVING:
                            switch (incomingSubject) {
                                case "success":
                                    incomingId = incomingJwts.getBody().getId();
                                    Log.w("ENCODEDDTOKEN", incomingId);
                                    String decoded = "";

                                    /*
                                    String base64encodedenrcyptedtest = rsaCipher.encrypt("asd1234");
                                    String decodedtest = rsaCipher.decrypt(base64encodedenrcyptedtest);
                                    Log.e("DECODEDTEST", decodedtest);
                                    */

                                    try {
                                        decoded = rsaCipher.decrypt(incomingId);
                                        Log.w("DECRYPTEDTOKEN", decoded);
                                    }
                                    catch (Exception e) {
                                        Log.w("DECODEERROR", "Decode unsuccessful");
                                        Toast.makeText(this.getApplicationContext(),
                                                "Decode error",
                                                Toast.LENGTH_SHORT).show();
                                        Log.e("EXCEPTION", "exception", e);
                                        throw e;
                                    }
                                    incomingAudience = incomingJwts.getBody().getAudience();
                                    final Locker tempLocker = new Locker(decoded, incomingAudience);
                                    AppExecutors.getInstance().diskIO().execute(new Runnable() {
                                        @Override
                                        public void run() {
                                            lockerDatabase.lockerDao().insertAll(tempLocker);
                                        }
                                    });

                                    lockerProcessInstance.setProcessState(ProcessState.SAVING);
                                    String encoded = "";
                                    try {
                                        encoded = rsaCipher.encrypt(decoded, lockerProcessInstance.getServerPublicKey());
                                        Log.w("ENCODESUCCESS", "Encode successful");
                                    }
                                    catch (Exception e) {
                                        Log.w("ENCODEERROR", "Encode unsuccessful");
                                        Toast.makeText(this.getApplicationContext(),
                                                "Encode error",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                    Log.w("ENCODEDTOKEN", encoded);
                                    outgoingJwts = Jwts.builder()
                                            .setSubject("success")
                                            .setId(encoded)
                                            .setAudience(incomingAudience)
                                            .signWith(key, lockerProcessInstance.getSignatureAlgorithm()).compact();
                                    break;
                                default:
                                    outgoingJwts = Jwts.builder().setSubject("failed").signWith(key, lockerProcessInstance.getSignatureAlgorithm()).compact();
                                    Log.w("RESERVEUNSUCCESSFUL", "Reserving locker");
                                    lockerProcessInstance.setProcessState(ProcessState.NONE);
                                    break;
                            }
                            break;
                        case SAVING:
                            switch (incomingSubject) {
                                case "done":
                                    outgoingJwts = Jwts.builder().setSubject("done").signWith(key, lockerProcessInstance.getSignatureAlgorithm()).compact();
                                    Toast.makeText(this.getApplicationContext(),
                                            "Success! Locker reserved.",
                                            Toast.LENGTH_SHORT).show();
                                    lockerProcessInstance.setProcessState(ProcessState.DONE);
                                    break;
                                default:
                                    outgoingJwts = Jwts.builder().setSubject("done").signWith(key, lockerProcessInstance.getSignatureAlgorithm()).compact();
                                    lockerProcessInstance.setProcessState(ProcessState.NONE);
                                    break;
                            }
                            break;
                        case DONE:
                            switch (incomingSubject) {
                                case "done":
                                    outgoingJwts = Jwts.builder().setSubject("done").signWith(key, lockerProcessInstance.getSignatureAlgorithm()).compact();
                                    lockerProcessInstance.setProcessState(ProcessState.DONE);
                                    break;
                                default:
                                    outgoingJwts = Jwts.builder().setSubject("done").signWith(key, lockerProcessInstance.getSignatureAlgorithm()).compact();
                                    lockerProcessInstance.setProcessState(ProcessState.NONE);
                                    break;
                            }
                            break;
                        case STARTINGOPEN:
                            switch (incomingSubject) {
                                case "challenge":
                                    Toast.makeText(this.getApplicationContext(),
                                            "CHALLENGE",
                                            Toast.LENGTH_SHORT).show();
                                    incomingPublicKey = incomingJwts.getBody().getIssuer();
                                    Log.w("SERVERPUBLICKEY", incomingPublicKey);
                                    try {
                                        lockerProcessInstance.setServerPublicKey(rsaCipher.stringToPublicKey(incomingPublicKey));
                                        lockerProcessInstance.setServerPublicKeyString(incomingPublicKey);
                                    }
                                    catch (Exception e) {
                                        Log.w("PUBLICKEYCONVERSION", "FAILED");
                                        Toast.makeText(this.getApplicationContext(),
                                                "Public key conversion failure",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                    outgoingJwts = Jwts.builder()
                                            .setSubject("yes")
                                            .setIssuer(rsaCipher.getPublicKey("pkcs8-pem"))
                                            .setAudience(lockerProcessInstance.getLocker().getNumber())
                                            .signWith(key, lockerProcessInstance.getSignatureAlgorithm()).compact();
                                    lockerProcessInstance.setProcessState(ProcessState.CHALLENGEOPEN);
                                    Log.w("OPENCHALLENGE", "YES");
                                    break;
                                default:
                                    outgoingJwts = Jwts.builder().setSubject("failed").signWith(key, lockerProcessInstance.getSignatureAlgorithm()).compact();
                                    lockerProcessInstance.setProcessState(ProcessState.NONE);
                                    break;
                            }
                            break;
                        case CHALLENGEOPEN:
                            switch (incomingSubject) {
                                case "decode":
                                    incomingTest = incomingJwts.getBody().getIssuer();

                                    String decoded = "";
                                    try {
                                        decoded = rsaCipher.decrypt(incomingTest);
                                    } catch (Exception e) {
                                        Toast.makeText(this.getApplicationContext(),
                                                "Decode error",
                                                Toast.LENGTH_SHORT).show();
                                        Log.e("OPENDECRYPTERROR", "exception", e);
                                        throw e;
                                    }
                                    String encoded = "";
                                    try {
                                        encoded = rsaCipher.encrypt(decoded, lockerProcessInstance.getServerPublicKey());
                                        //encoded = encoded.substring(0, encoded.length() - 1);
                                    } catch (Exception e) {
                                        Toast.makeText(this.getApplicationContext(),
                                                "Encode error",
                                                Toast.LENGTH_SHORT).show();
                                        Log.e("OPENENCRYPTERROR", "exception", e);
                                        throw e;
                                    }
                                    outgoingJwts = Jwts.builder()
                                            .setSubject("response")
                                            .setIssuer(encoded)
                                            .signWith(key, lockerProcessInstance.getSignatureAlgorithm()).compact();
                                    lockerProcessInstance.setProcessState(ProcessState.CHALLENGEOPENSUCCESS);
                                    Toast.makeText(this.getApplicationContext(),
                                            "Crypto response sent",
                                            Toast.LENGTH_LONG).show();
                                    break;
                                default:
                                    outgoingJwts = Jwts.builder().setSubject("failed").signWith(key, lockerProcessInstance.getSignatureAlgorithm()).compact();
                                    lockerProcessInstance.setProcessState(ProcessState.NONE);
                                    break;
                            }
                            break;
                        case CHALLENGEOPENSUCCESS:
                            switch (incomingSubject) {
                                case "select":
                                    String encoded = "";
                                    try {
                                        encoded = rsaCipher.encrypt(lockerProcessInstance.getLocker().getTokenString(), lockerProcessInstance.getServerPublicKey());
                                        //encoded = encoded.substring(0, encoded.length() - 1);
                                    }
                                    catch (Exception e) {
                                        Toast.makeText(this.getApplicationContext(),
                                                "Encode error",
                                                Toast.LENGTH_SHORT).show();
                                        Log.e("OPENENCRYPTERROR", "exception", e);
                                        throw e;
                                    }
                                    outgoingJwts = Jwts.builder()
                                            .setSubject("open")
                                            .setId(encoded)
                                            .setAudience(lockerProcessInstance.getLocker().getNumber())
                                            .signWith(key, lockerProcessInstance.getSignatureAlgorithm()).compact();
                                    lockerProcessInstance.setProcessState(ProcessState.OPENING);
                                    Toast.makeText(this.getApplicationContext(),
                                            "Opening locker " + lockerProcessInstance.getLocker().getTokenString(),
                                            Toast.LENGTH_LONG).show();
                                    break;
                                default:
                                    outgoingJwts = Jwts.builder().setSubject("failed").signWith(key, lockerProcessInstance.getSignatureAlgorithm()).compact();
                                    lockerProcessInstance.setProcessState(ProcessState.NONE);
                                    break;
                            }
                            break;
                        case OPENING:
                            switch (incomingSubject) {
                                case "success":
                                    outgoingJwts = Jwts.builder().setSubject("acknowledged")
                                            .signWith(key, lockerProcessInstance.getSignatureAlgorithm()).compact();
                                    lockerProcessInstance.setProcessState(ProcessState.SAVING);
                                    Toast.makeText(this.getApplicationContext(),
                                            "Opened locker",
                                            Toast.LENGTH_LONG).show();
                                    break;
                                default:
                                    outgoingJwts = Jwts.builder().setSubject("failed").signWith(key, lockerProcessInstance.getSignatureAlgorithm()).compact();
                                    lockerProcessInstance.setProcessState(ProcessState.NONE);
                                    break;
                            }
                            break;
                        case STARTINGCLOSE:
                            switch (incomingSubject) {
                                case "challenge":
                                    Toast.makeText(this.getApplicationContext(),
                                            "CHALLENGE",
                                            Toast.LENGTH_SHORT).show();
                                    incomingPublicKey = incomingJwts.getBody().getIssuer();
                                    Log.w("SERVERPUBLICKEY", incomingPublicKey);
                                    try {
                                        lockerProcessInstance.setServerPublicKey(rsaCipher.stringToPublicKey(incomingPublicKey));
                                        lockerProcessInstance.setServerPublicKeyString(incomingPublicKey);
                                    }
                                    catch (Exception e) {
                                        Log.w("PUBLICKEYCONVERSION", "FAILED");
                                        Toast.makeText(this.getApplicationContext(),
                                                "Public key conversion failure",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                    outgoingJwts = Jwts.builder()
                                            .setSubject("yes")
                                            .setIssuer(rsaCipher.getPublicKey("pkcs8-pem"))
                                            .setAudience(lockerProcessInstance.getLocker().getNumber())
                                            .signWith(key, lockerProcessInstance.getSignatureAlgorithm()).compact();
                                    lockerProcessInstance.setProcessState(ProcessState.CHALLENGECLOSE);
                                    Log.w("CLOSECHALLENGE", "YES");
                                    break;
                                default:
                                    outgoingJwts = Jwts.builder().setSubject("failed").signWith(key, lockerProcessInstance.getSignatureAlgorithm()).compact();
                                    lockerProcessInstance.setProcessState(ProcessState.NONE);
                                    break;
                            }
                            break;
                        case CHALLENGECLOSE:
                            switch (incomingSubject) {
                                case "decode":
                                    incomingTest = incomingJwts.getBody().getIssuer();

                                    String decoded = "";
                                    try {
                                        decoded = rsaCipher.decrypt(incomingTest);
                                    } catch (Exception e) {
                                        Toast.makeText(this.getApplicationContext(),
                                                "Decode error",
                                                Toast.LENGTH_SHORT).show();
                                        Log.e("CLOSEDECRYPTERROR", "exception", e);
                                        throw e;
                                    }
                                    String encoded = "";
                                    try {
                                        encoded = rsaCipher.encrypt(decoded, lockerProcessInstance.getServerPublicKey());
                                        //encoded = encoded.substring(0, encoded.length() - 1);
                                    } catch (Exception e) {
                                        Toast.makeText(this.getApplicationContext(),
                                                "Encode error",
                                                Toast.LENGTH_SHORT).show();
                                        Log.e("CLOSEENCRYPTERROR", "exception", e);
                                        throw e;
                                    }
                                    outgoingJwts = Jwts.builder()
                                            .setSubject("response")
                                            .setIssuer(encoded)
                                            .signWith(key, lockerProcessInstance.getSignatureAlgorithm()).compact();
                                    lockerProcessInstance.setProcessState(ProcessState.CHALLENGECLOSESUCCESS);
                                    Toast.makeText(this.getApplicationContext(),
                                            "Crypto response sent",
                                            Toast.LENGTH_LONG).show();
                                    break;
                                default:
                                    outgoingJwts = Jwts.builder().setSubject("failed").signWith(key, lockerProcessInstance.getSignatureAlgorithm()).compact();
                                    lockerProcessInstance.setProcessState(ProcessState.NONE);
                                    break;
                            }
                            break;
                        case CHALLENGECLOSESUCCESS:
                            switch (incomingSubject) {
                                case "select":
                                    String encoded = "";
                                    try {
                                        encoded = rsaCipher.encrypt(lockerProcessInstance.getLocker().getTokenString(), lockerProcessInstance.getServerPublicKey());
                                        //encoded = encoded.substring(0, encoded.length() - 1);
                                    }
                                    catch (Exception e) {
                                        Toast.makeText(this.getApplicationContext(),
                                                "Encode error",
                                                Toast.LENGTH_SHORT).show();
                                        Log.e("CLOSEENCRYPTERROR", "exception", e);
                                        throw e;
                                    }
                                    outgoingJwts = Jwts.builder()
                                            .setSubject("close")
                                            .setId(encoded)
                                            .setAudience(lockerProcessInstance.getLocker().getNumber())
                                            .signWith(key, lockerProcessInstance.getSignatureAlgorithm()).compact();
                                    lockerProcessInstance.setProcessState(ProcessState.CLOSING);
                                    Toast.makeText(this.getApplicationContext(),
                                            "Closing locker " + lockerProcessInstance.getLocker().getTokenString(),
                                            Toast.LENGTH_LONG).show();
                                    break;
                                default:
                                    outgoingJwts = Jwts.builder().setSubject("failed").signWith(key, lockerProcessInstance.getSignatureAlgorithm()).compact();
                                    lockerProcessInstance.setProcessState(ProcessState.NONE);
                                    break;
                            }
                            break;
                        case CLOSING:
                            switch (incomingSubject) {
                                case "success":
                                    outgoingJwts = Jwts.builder().setSubject("acknowledged")
                                            .signWith(key, lockerProcessInstance.getSignatureAlgorithm()).compact();
                                    lockerProcessInstance.setProcessState(ProcessState.SAVING);
                                    Toast.makeText(this.getApplicationContext(),
                                            "Closed locker",
                                            Toast.LENGTH_LONG).show();
                                    break;
                                default:
                                    outgoingJwts = Jwts.builder().setSubject("failed")
                                            .signWith(key, lockerProcessInstance.getSignatureAlgorithm()).compact();
                                    lockerProcessInstance.setProcessState(ProcessState.NONE);
                                    break;
                            }
                            break;
                        case STARTINGRELEASE:
                            switch (incomingSubject) {
                                case "challenge":
                                    Toast.makeText(this.getApplicationContext(),
                                            "CHALLENGE",
                                            Toast.LENGTH_SHORT).show();
                                    incomingPublicKey = incomingJwts.getBody().getIssuer();
                                    Log.w("SERVERPUBLICKEY", incomingPublicKey);
                                    try {
                                        lockerProcessInstance.setServerPublicKey(rsaCipher.stringToPublicKey(incomingPublicKey));
                                        lockerProcessInstance.setServerPublicKeyString(incomingPublicKey);
                                    }
                                    catch (Exception e) {
                                        Log.w("PUBLICKEYCONVERSION", "FAILED");
                                        Toast.makeText(this.getApplicationContext(),
                                                "Public key conversion failure",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                    outgoingJwts = Jwts.builder()
                                            .setSubject("yes")
                                            .setIssuer(rsaCipher.getPublicKey("pkcs8-pem"))
                                            .setAudience(lockerProcessInstance.getLocker().getNumber())
                                            .signWith(key, lockerProcessInstance.getSignatureAlgorithm()).compact();
                                    lockerProcessInstance.setProcessState(ProcessState.CHALLENGERELEASE);
                                    Log.w("RELEASECHALLENGE", "YES");
                                    break;
                                default:
                                    outgoingJwts = Jwts.builder().setSubject("failed").signWith(key, lockerProcessInstance.getSignatureAlgorithm()).compact();
                                    lockerProcessInstance.setProcessState(ProcessState.NONE);
                                    break;
                            }
                            break;
                        case CHALLENGERELEASE:
                            switch (incomingSubject) {
                                case "decode":
                                    incomingTest = incomingJwts.getBody().getIssuer();

                                    String decoded = "";
                                    try {
                                        decoded = rsaCipher.decrypt(incomingTest);
                                    } catch (Exception e) {
                                        Toast.makeText(this.getApplicationContext(),
                                                "Decode error",
                                                Toast.LENGTH_SHORT).show();
                                        Log.e("RELEASEDECRYPTERROR", "exception", e);
                                        throw e;
                                    }
                                    String encoded = "";
                                    try {
                                        encoded = rsaCipher.encrypt(decoded, lockerProcessInstance.getServerPublicKey());
                                        //encoded = encoded.substring(0, encoded.length() - 1);
                                    } catch (Exception e) {
                                        Toast.makeText(this.getApplicationContext(),
                                                "Encode error",
                                                Toast.LENGTH_SHORT).show();
                                        Log.e("RELEASEENCRYPTERROR", "exception", e);
                                        throw e;
                                    }
                                    outgoingJwts = Jwts.builder()
                                            .setSubject("response")
                                            .setIssuer(encoded)
                                            .signWith(key, lockerProcessInstance.getSignatureAlgorithm()).compact();
                                    lockerProcessInstance.setProcessState(ProcessState.CHALLENGERELEASESUCCESS);
                                    Toast.makeText(this.getApplicationContext(),
                                            "Crypto response sent",
                                            Toast.LENGTH_LONG).show();
                                    break;
                                default:
                                    outgoingJwts = Jwts.builder().setSubject("failed").signWith(key, lockerProcessInstance.getSignatureAlgorithm()).compact();
                                    lockerProcessInstance.setProcessState(ProcessState.NONE);
                                    break;
                            }
                            break;
                        case CHALLENGERELEASESUCCESS:
                            switch (incomingSubject) {
                                case "select":
                                    String encoded = "";
                                    try {
                                        encoded = rsaCipher.encrypt(lockerProcessInstance.getLocker().getTokenString(), lockerProcessInstance.getServerPublicKey());
                                        //encoded = encoded.substring(0, encoded.length() - 1);
                                    }
                                    catch (Exception e) {
                                        Toast.makeText(this.getApplicationContext(),
                                                "Encode error",
                                                Toast.LENGTH_SHORT).show();
                                        Log.e("RELEASEENCRYPTERROR", "exception", e);
                                        throw e;
                                    }
                                    outgoingJwts = Jwts.builder()
                                            .setSubject("close")
                                            .setId(encoded)
                                            .setAudience(lockerProcessInstance.getLocker().getNumber())
                                            .signWith(key, lockerProcessInstance.getSignatureAlgorithm()).compact();
                                    lockerProcessInstance.setProcessState(ProcessState.RELEASING);
                                    Toast.makeText(this.getApplicationContext(),
                                            "Releasing locker " + lockerProcessInstance.getLocker().getTokenString(),
                                            Toast.LENGTH_LONG).show();
                                    break;
                                default:
                                    outgoingJwts = Jwts.builder().setSubject("failed").signWith(key, lockerProcessInstance.getSignatureAlgorithm()).compact();
                                    lockerProcessInstance.setProcessState(ProcessState.NONE);
                                    break;
                            }
                            break;
                        case RELEASING:
                            switch (incomingSubject) {
                                case "success":
                                    outgoingJwts = Jwts.builder().setSubject("acknowledged").signWith(key, lockerProcessInstance.getSignatureAlgorithm()).compact();
                                    AppExecutors.getInstance().diskIO().execute(new Runnable() {
                                        @Override
                                        public void run() {
                                            lockerDatabase.lockerDao().delete(LockerProcessSingleton.getInstance().getLocker());
                                        }
                                    });

                                    lockerProcessInstance.setProcessState(ProcessState.SAVING);
                                    Toast.makeText(this.getApplicationContext(),
                                            "Released locker",
                                            Toast.LENGTH_LONG).show();
                                    break;
                                default:
                                    outgoingJwts = Jwts.builder().setSubject("failed").signWith(key, lockerProcessInstance.getSignatureAlgorithm()).compact();
                                    lockerProcessInstance.setProcessState(ProcessState.NONE);
                                    break;
                            }
                            break;
                        default:
                            outgoingJwts = Jwts.builder().setSubject("failed").signWith(key, lockerProcessInstance.getSignatureAlgorithm()).compact();
                            lockerProcessInstance.setProcessState(ProcessState.NONE);
                            Toast.makeText(this.getApplicationContext(),
                                    "Error: inconsistent state",
                                    Toast.LENGTH_LONG).show();
                            break;
                    }
                }
                catch (Exception ex) {     // (4)
                    outgoingJwts = Jwts.builder().setSubject("failed").signWith(key, lockerProcessInstance.getSignatureAlgorithm()).compact();
                    lockerProcessInstance.setProcessState(ProcessState.NONE);
                    Toast.makeText(this.getApplicationContext(),
                            "JWTS: EXCEPTION",
                            Toast.LENGTH_LONG).show();
                    // we *cannot* use the JWT as intended by its creator
                }
                finally {
                    lockerProcessInstance.setTokenString(outgoingJwts);
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
        Log.w("COMMANDSENT", new String(commandSent));
        return commandSent;
    }
    // END_INCLUDE(processCommandApdu)

    /**
     * Build APDU for SELECT AID command. This command indicates which service a reader is
     * interested in communicating with. See ISO 7816-4.
     *
     * @param aid Application ID (AID) to select
     * @return APDU for SELECT AID command
     */
    public static byte[] BuildSelectApdu(String aid) {
        // Format: [CLASS | INSTRUCTION | PARAMETER 1 | PARAMETER 2 | LENGTH | DATA]
        return HexStringToByteArray(SELECT_APDU_HEADER + String.format("%02X",
                aid.length() / 2) + aid);
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
