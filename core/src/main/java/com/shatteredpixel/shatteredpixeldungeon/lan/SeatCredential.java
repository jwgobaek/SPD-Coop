package com.shatteredpixel.shatteredpixeldungeon.lan;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.ECPublicKey;
import java.security.spec.*;
import java.util.UUID;

/** Private client identity. Enroll its public key once before roster lock. */
public final class SeatCredential {
    private final KeyPair keys;
    private SeatCredential(KeyPair keys) { this.keys = keys; }

    public static SeatCredential generate() throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        return new SeatCredential(generator.generateKeyPair());
    }

    public String enroll(LanClient client) throws IOException {
        return client.request("ENROLL "+hex(keys.getPublic().getEncoded()));
    }

    /** Call on a fresh negotiated connection. The UI must approve a changed recovery epoch. */
    public String reconnect(LanClient client,UUID epoch,int seat,long player) throws IOException {
        String challenge = client.request("CHALLENGE "+epoch+" "+seat+" "+player);
        if (!challenge.startsWith("NONCE ")) return challenge;
        String nonce = challenge.substring(6);
        if (!nonce.matches("[0-9a-f]{64}")) throw new IOException("Invalid challenge");
        try {
            Signature signature = Signature.getInstance("SHA256withECDSA");
            signature.initSign(keys.getPrivate());
            signature.update(message(client.session,epoch,seat,player,nonce));
            return client.request("PROVE "+hex(signature.sign()));
        } catch (GeneralSecurityException invalid) { throw new IOException("Cannot sign challenge",invalid); }
    }

    static byte[] message(String session,UUID epoch,int seat,long player,String nonce) {
        return ("SPD-RESUME-1 "+session+" "+epoch+" "+seat+" "+player+" "+nonce).getBytes(StandardCharsets.US_ASCII);
    }

    static PublicKey verifier(String encoded) throws GeneralSecurityException {
        byte[] bytes = bytes(encoded,128);
        ECPublicKey key = (ECPublicKey)KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(bytes));
        // Accept only the named-curve P-256 SubjectPublicKeyInfo encoding (no arbitrary curve parameters).
        if (key.getParams().getCurve().getField().getFieldSize()!=256 ||
                !encoded.startsWith("3059301306072a8648ce3d020106082a8648ce3d03010703420004") || bytes.length!=91)
            throw new InvalidKeyException("Unsupported credential");
        return key;
    }

    static byte[] bytes(String text,int max) throws InvalidKeyException {
        if (text.length()<2 || text.length()>max*2 || text.length()%2!=0 || !text.matches("[0-9a-f]+"))
            throw new InvalidKeyException("Invalid encoding");
        byte[] b = new byte[text.length()/2];
        for (int i=0;i<b.length;i++) b[i]=(byte)Integer.parseInt(text.substring(i*2,i*2+2),16);
        return b;
    }

    static String hex(byte[] bytes) {
        StringBuilder b = new StringBuilder(bytes.length*2);
        for(byte x:bytes) b.append(Character.forDigit((x&255)>>>4,16)).append(Character.forDigit(x&15,16));
        return b.toString();
    }

    /** Store only in the application's private credential storage, separate from diagnostics. */
    public void writePrivate(OutputStream stream) throws IOException {
        DataOutputStream out = new DataOutputStream(stream);
        out.writeUTF(hex(keys.getPublic().getEncoded()));
        out.writeUTF(hex(keys.getPrivate().getEncoded()));
        out.flush();
    }

    public static SeatCredential readPrivate(InputStream stream) throws IOException {
        try {
            DataInputStream in = new DataInputStream(stream);
            PublicKey pub = verifier(in.readUTF());
            PrivateKey secret = KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(bytes(in.readUTF(),256)));
            return new SeatCredential(new KeyPair(pub,secret));
        } catch (GeneralSecurityException invalid) { throw new IOException("Invalid private credential",invalid); }
    }
}
