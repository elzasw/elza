package cz.tacr.elza.security;

import cz.tacr.elza.exception.SystemException;
import org.apache.commons.codec.digest.MessageDigestAlgorithms;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.security.crypto.codec.Utf8;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Pomocná třída pro vyhodnocení hesla podle staršího alg. Pouze pro zpětnou kompatibilitu.
 */
public class Sha256Support {

    private static final MessageDigest digest;

    static {
        try {
            digest = MessageDigest.getInstance(MessageDigestAlgorithms.SHA_256);
        } catch (NoSuchAlgorithmException e) {
            throw new SystemException("Nebyl nalezen algoritmus pro sha256", e);
        }
    }

    public static String encodePassword(final String password, final String salt) {
        String str = password == null ? "" : password;
        str = salt == null || "".equals(salt) ? str : str + "{" + salt + "}";
        return new String(Hex.encode(digest.digest(Utf8.encode(str))));
    }

}
