package com.moriba.skultem.infrastructure.push;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.Signature;
import java.util.Arrays;
import java.util.Base64;

import org.junit.jupiter.api.Test;

class WebPushCryptoTest {

    // RFC 8291 Appendix A - the worked example, byte for byte.
    @Test
    void encryptsTheRfc8291Example() throws Exception {
        byte[] plaintext = "When I grow up, I want to be a watermelon".getBytes(StandardCharsets.UTF_8);
        byte[] asPrivate = WebPushCrypto.b64("yfWPiYE-n46HLnH0KqZOF1fJJU3MYrct3AELtAQ-oRw");
        byte[] asPublic = WebPushCrypto.b64("BP4z9KsN6nGRTbVYI_c7VJSPQTBtkgcy27mlmlMoZIIgDll6e3vCYLocInmYWAmS6TlzAC8wEqKK6PBru3jl7A8");
        byte[] uaPublic = WebPushCrypto.b64("BCVxsr7N_eNgVRqvHtD0zTZsEc6-VV-JvLexhqUzORcxaOzi6-AYWXvTBHm4bjyPjs7Vd8pZGH6SRpkNtoIAiw4");
        byte[] auth = WebPushCrypto.b64("BTBZMqHH6r4Tts7J_aSIgg");
        byte[] salt = WebPushCrypto.b64("DGv6ra1nlYgDCS1FRnbzlw");

        KeyPair sender = new KeyPair(WebPushCrypto.publicKey(asPublic), WebPushCrypto.privateKey(asPrivate));
        byte[] body = WebPushCrypto.encrypt(plaintext, uaPublic, auth, sender, salt);

        assertThat(WebPushCrypto.b64url(body)).isEqualTo(
                "DGv6ra1nlYgDCS1FRnbzlwAAEABBBP4z9KsN6nGRTbVYI_c7VJSPQTBtkgcy27mlmlMoZIIgDll6e3vCYLocInmYWAmS6TlzAC8wEqKK6PBru3jl7A_yl95bQpu6cVPTpK4Mqgkf1CXztLVBSt2Ks3oZwbuwXPXLWyouBWLVWGNWQexSgSxsj_Qulcy4a-fN");
    }

    @Test
    void vapidHeaderIsASignedJwtForThePushServiceOrigin() throws Exception {
        String[] keys = WebPushCrypto.generateVapidKeys();
        String header = WebPushCrypto.vapidAuthorization("https://fcm.googleapis.com/fcm/send/abc", "mailto:support@skultem.space", keys[0], keys[1]);

        assertThat(header).startsWith("vapid t=").endsWith(", k=" + keys[0]);
        String jwt = header.substring("vapid t=".length(), header.indexOf(", k="));
        String[] parts = jwt.split("\\.");
        String claims = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        assertThat(claims).contains("\"aud\":\"https://fcm.googleapis.com\"").contains("\"sub\":\"mailto:support@skultem.space\"");

        Signature verifier = Signature.getInstance("SHA256withECDSAinP1363Format");
        verifier.initVerify(WebPushCrypto.publicKey(WebPushCrypto.b64(keys[0])));
        verifier.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));
        assertThat(verifier.verify(Base64.getUrlDecoder().decode(parts[2]))).isTrue();
        assertThat(Arrays.asList(keys[0].length(), keys[1].length())).containsExactly(87, 43);
    }
}
