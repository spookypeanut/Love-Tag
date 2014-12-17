package uk.co.spookypeanut.lovetag;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by hbush on 16/12/14.
 */
public class Md5Maker {
    public String encode(String to_be_encoded) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(to_be_encoded.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < messageDigest.length; i++) {
                String h = Integer.toHexString(0xFF & messageDigest[i]);
                while (h.length() < 2)
                    h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }
}
