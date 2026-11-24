package com.shatteredpixel.shatteredpixeldungeon.android;

import android.annotation.TargetApi;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import com.shatteredpixel.shatteredpixeldungeon.lan.SeatCredential;
import com.shatteredpixel.shatteredpixeldungeon.lan.SeatTickets;
import java.io.IOException;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.util.UUID;

@TargetApi(23)
final class AndroidSeatKeys implements SeatTickets.Keys {
    private static KeyStore store() throws GeneralSecurityException,IOException {
        KeyStore ks=KeyStore.getInstance("AndroidKeyStore"); ks.load(null); return ks;
    }
    public SeatCredential create(UUID session) throws GeneralSecurityException,IOException {
        String alias="spd.coop."+session;
        if(store().containsAlias(alias)) return load(session);
        KeyPairGenerator g=KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC,"AndroidKeyStore");
        g.initialize(new KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_SIGN)
                .setAlgorithmParameterSpec(new ECGenParameterSpec("secp256r1"))
                .setDigests(KeyProperties.DIGEST_SHA256).build());
        return new SeatCredential(g.generateKeyPair());
    }
    public SeatCredential load(UUID session) throws GeneralSecurityException,IOException {
        KeyStore ks=store(); String alias="spd.coop."+session;
        if(!ks.isKeyEntry(alias)) throw new IOException("CREDENTIAL");
        return new SeatCredential(new KeyPair(ks.getCertificate(alias).getPublicKey(),(PrivateKey)ks.getKey(alias,null)));
    }
}
