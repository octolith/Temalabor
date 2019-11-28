package com.tokenizer.p2p2;

import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.widget.Toast;

import java.util.Arrays;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

import java.security.Key;

public class CardService extends HostApduService {
    private static final String TAG = "CardService";
    // AID for our loyalty card service.
    private static final String SAMPLE_LOYALTY_CARD_AID = "F0CDB82BD7BC06";
    // ISO-DEP command HEADER for selecting an AID.
    // Format: [Class | Instruction | Parameter 1 | Parameter 2]
    private static final String SELECT_APDU_HEADER = "00A40400";
    // "OK" status word sent in response to SELECT AID command (0x9000)
    private static final byte[] SELECT_OK_SW = HexStringToByteArray("9000");
    // "UNKNOWN" status word sent in response to invalid APDU command (0x0000)
    private static final byte[] UNKNOWN_CMD_SW = HexStringToByteArray("0000");
    private static final byte[] SELECT_APDU = BuildSelectApdu(SAMPLE_LOYALTY_CARD_AID);

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

        String apduHexString = ByteArrayToHexString(commandApdu);

        LockerProcessSingleton lockerProcessInstance = LockerProcessSingleton.getInstance();

        String outgoingJws = "";

        Key key = lockerProcessInstance.getKey();

        byte[] commandSent;

        if(apduHexString.startsWith("00A40400")) {
            /*Toast.makeText(this.getApplicationContext(),
                    "APDU select received: " + apduHexString,
                    Toast.LENGTH_LONG).show();*/
            commandSent = ConcatArrays(SELECT_OK_SW, HexStringToByteArray("00"));
        }
        else {
            String data = new String(commandApdu);
            /*Toast.makeText(this.getApplicationContext(),
                    "Received: " + data,
                    Toast.LENGTH_LONG).show();*/
            data = data.substring(0, data.length()-1);

            Jws<Claims> incomingJws;

            try {
                incomingJws = Jwts.parser()         // (1)
                        .setSigningKey(key)         // (2)
                        .parseClaimsJws(data);      // (3)

                // we can safely trust the JWT

                String incomingSubject = incomingJws.getBody().getSubject();
                String incomingId;
                String incomingAudience;
                switch (lockerProcessInstance.getProcessState()) {
                    case NONE:
                        outgoingJws = Jwts.builder().setSubject("failed").signWith(key, lockerProcessInstance.getSignatureAlgorithm()).compact();
                        Toast.makeText(this.getApplicationContext(),
                                "Failure",
                                Toast.LENGTH_LONG).show();
                        break;
                    case STARTINGRESERVE:
                        switch (incomingSubject) {
                            case "select":
                                outgoingJws = Jwts.builder().setSubject("reserve").signWith(key, lockerProcessInstance.getSignatureAlgorithm()).compact();
                                lockerProcessInstance.setProcessState(ProcessState.RESERVING);
                                Toast.makeText(this.getApplicationContext(),
                                        "Reserving locker",
                                        Toast.LENGTH_LONG).show();
                                break;
                            default:
                                outgoingJws = Jwts.builder().setSubject("failed").signWith(key, lockerProcessInstance.getSignatureAlgorithm()).compact();
                                lockerProcessInstance.setProcessState(ProcessState.NONE);
                                break;
                        }
                        break;
                    case RESERVING:
                        switch (incomingSubject) {
                            case "success":
                                incomingId = incomingJws.getBody().getId();
                                incomingAudience = incomingJws.getBody().getAudience();
                                lockerProcessInstance.setProcessState(ProcessState.SAVING);
                                lockerProcessInstance.reserveLocker(incomingId, incomingAudience);
                                outgoingJws = Jwts.builder()
                                        .setSubject("success")
                                        .setId(incomingId)
                                        .setAudience(incomingAudience)
                                        .signWith(key, lockerProcessInstance.getSignatureAlgorithm()).compact();
                                Toast.makeText(this.getApplicationContext(),
                                        "Reserved locker " + incomingAudience,
                                        Toast.LENGTH_LONG).show();
                                break;
                            default:
                                outgoingJws = Jwts.builder().setSubject("failed").signWith(key, lockerProcessInstance.getSignatureAlgorithm()).compact();
                                lockerProcessInstance.setProcessState(ProcessState.NONE);
                                break;
                        }
                        break;
                    case SAVING:
                        switch (incomingSubject) {
                            case "done":
                                outgoingJws = Jwts.builder().setSubject("done").signWith(key, lockerProcessInstance.getSignatureAlgorithm()).compact();
                                lockerProcessInstance.setProcessState(ProcessState.DONE);
                                break;
                            default:
                                outgoingJws = Jwts.builder().setSubject("done").signWith(key, lockerProcessInstance.getSignatureAlgorithm()).compact();
                                lockerProcessInstance.setProcessState(ProcessState.NONE);
                                break;
                        }
                        break;
                    case DONE:
                        switch (incomingSubject) {
                            case "done":
                                outgoingJws = Jwts.builder().setSubject("done").signWith(key, lockerProcessInstance.getSignatureAlgorithm()).compact();
                                lockerProcessInstance.setProcessState(ProcessState.DONE);
                                break;
                            default:
                                outgoingJws = Jwts.builder().setSubject("done").signWith(key, lockerProcessInstance.getSignatureAlgorithm()).compact();
                                lockerProcessInstance.setProcessState(ProcessState.NONE);
                                break;
                        }
                        break;
                    case STARTINGOPEN:
                        switch (incomingSubject) {
                            case "select":
                                outgoingJws = Jwts.builder()
                                        .setSubject("open")
                                        .setId(lockerProcessInstance.getReservedLocker().getTokenString())
                                        .setAudience(lockerProcessInstance.getReservedLocker().getNumber())
                                        .signWith(key, lockerProcessInstance.getSignatureAlgorithm()).compact();
                                lockerProcessInstance.setProcessState(ProcessState.OPENING);
                                Toast.makeText(this.getApplicationContext(),
                                        "Opening locker " + lockerProcessInstance.getReservedLocker().getTokenString(),
                                        Toast.LENGTH_LONG).show();
                                break;
                            default:
                                outgoingJws = Jwts.builder().setSubject("failed").signWith(key, lockerProcessInstance.getSignatureAlgorithm()).compact();
                                lockerProcessInstance.setProcessState(ProcessState.NONE);
                                break;
                        }
                        break;
                    case OPENING:
                        switch (incomingSubject) {
                            case "success":
                                outgoingJws = Jwts.builder().setSubject("acknowledged").signWith(key, lockerProcessInstance.getSignatureAlgorithm()).compact();
                                lockerProcessInstance.setProcessState(ProcessState.SAVING);
                                Toast.makeText(this.getApplicationContext(),
                                        "Opened locker",
                                        Toast.LENGTH_LONG).show();
                                break;
                            default:
                                outgoingJws = Jwts.builder().setSubject("failed").signWith(key, lockerProcessInstance.getSignatureAlgorithm()).compact();
                                lockerProcessInstance.setProcessState(ProcessState.NONE);
                                break;
                        }
                        break;
                    case STARTINGCLOSE:
                        switch (incomingSubject) {
                            case "select":
                                outgoingJws = Jwts.builder()
                                        .setSubject("close")
                                        .setId(lockerProcessInstance.getReservedLocker().getTokenString())
                                        .setAudience(lockerProcessInstance.getReservedLocker().getNumber())
                                        .signWith(key, lockerProcessInstance.getSignatureAlgorithm()).compact();
                                lockerProcessInstance.setProcessState(ProcessState.CLOSING);
                                Toast.makeText(this.getApplicationContext(),
                                        "Closing locker " + lockerProcessInstance.getReservedLocker().getTokenString(),
                                        Toast.LENGTH_LONG).show();
                                break;
                            default:
                                outgoingJws = Jwts.builder().setSubject("failed").signWith(key, lockerProcessInstance.getSignatureAlgorithm()).compact();
                                lockerProcessInstance.setProcessState(ProcessState.NONE);
                                break;
                        }
                        break;
                    case CLOSING:
                        switch (incomingSubject) {
                            case "success":
                                outgoingJws = Jwts.builder().setSubject("acknowledged").signWith(key, lockerProcessInstance.getSignatureAlgorithm()).compact();
                                lockerProcessInstance.setProcessState(ProcessState.SAVING);
                                Toast.makeText(this.getApplicationContext(),
                                        "Closed locker",
                                        Toast.LENGTH_LONG).show();
                                break;
                            default:
                                outgoingJws = Jwts.builder().setSubject("failed").signWith(key, lockerProcessInstance.getSignatureAlgorithm()).compact();
                                lockerProcessInstance.setProcessState(ProcessState.NONE);
                                break;
                        }
                        break;
                    case STARTINGRELEASE:
                        switch (incomingSubject) {
                            case "select":
                                outgoingJws = Jwts.builder()
                                        .setSubject("release")
                                        .setId(lockerProcessInstance.getReservedLocker().getTokenString())
                                        .setAudience(lockerProcessInstance.getReservedLocker().getNumber())
                                        .signWith(key, lockerProcessInstance.getSignatureAlgorithm()).compact();
                                lockerProcessInstance.setProcessState(ProcessState.RELEASING);
                                break;
                            default:
                                outgoingJws = Jwts.builder().setSubject("failed").signWith(key, lockerProcessInstance.getSignatureAlgorithm()).compact();
                                lockerProcessInstance.setProcessState(ProcessState.NONE);
                                break;
                        }
                        break;
                    case RELEASING:
                        switch (incomingSubject) {
                            case "success":
                                outgoingJws = Jwts.builder().setSubject("acknowledged").signWith(key, lockerProcessInstance.getSignatureAlgorithm()).compact();
                                lockerProcessInstance.releaseLocker();
                                lockerProcessInstance.setProcessState(ProcessState.SAVING);
                                Toast.makeText(this.getApplicationContext(),
                                        "Released locker",
                                        Toast.LENGTH_LONG).show();
                                break;
                            default:
                                outgoingJws = Jwts.builder().setSubject("failed").signWith(key, lockerProcessInstance.getSignatureAlgorithm()).compact();
                                lockerProcessInstance.setProcessState(ProcessState.NONE);
                                break;
                        }
                        break;
                    default:
                        outgoingJws = Jwts.builder().setSubject("failed").signWith(key, lockerProcessInstance.getSignatureAlgorithm()).compact();
                        lockerProcessInstance.setProcessState(ProcessState.NONE);
                        Toast.makeText(this.getApplicationContext(),
                                "Error: inconsistent state",
                                Toast.LENGTH_LONG).show();
                        break;
                }
            } catch (JwtException ex) {     // (4)
                outgoingJws = Jwts.builder().setSubject("failed").signWith(key, lockerProcessInstance.getSignatureAlgorithm()).compact();
                lockerProcessInstance.setProcessState(ProcessState.NONE);
                Toast.makeText(this.getApplicationContext(),
                        "JWTS: EXCEPTION",
                        Toast.LENGTH_LONG).show();
                // we *cannot* use the JWT as intended by its creator
            }

            lockerProcessInstance.setTokenString(outgoingJws);

            /*Toast.makeText(this.getApplicationContext(),
                    "JWTS: " + outgoingJws,
                    Toast.LENGTH_LONG).show();*/
        }

        commandSent = ConcatArrays(outgoingJws.getBytes(), HexStringToByteArray("00"));
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
