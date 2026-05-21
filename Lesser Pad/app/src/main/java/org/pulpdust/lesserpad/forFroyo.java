package org.pulpdust.lesserpad;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import android.annotation.TargetApi;
import android.util.Base64;

@TargetApi(8)
public class forFroyo {

	private static final String MAGIC_V2 = "LP2";
	private static final int PBKDF2_ITERS = 100000;
	private static final int KEY_BITS = 256;
	private static final int SALT_BYTES = 16;
	private static final int GCM_IV_BYTES = 12;
	private static final int GCM_TAG_BITS = 128;

	public String doEncrypt(String rawPass, String text)
	throws GeneralSecurityException, UnsupportedEncodingException {
		SecureRandom rng = new SecureRandom();
		byte[] salt = new byte[SALT_BYTES];
		rng.nextBytes(salt);
		byte[] iv = new byte[GCM_IV_BYTES];
		rng.nextBytes(iv);
		SecretKey k = deriveKeyV2(rawPass, salt);
		Cipher cp = Cipher.getInstance("AES/GCM/NoPadding");
		cp.init(Cipher.ENCRYPT_MODE, k, new GCMParameterSpec(GCM_TAG_BITS, iv));
		byte[] ct = cp.doFinal(text.getBytes("UTF-8"));
		return MAGIC_V2 + "\n"
			+ Base64.encodeToString(salt, Base64.NO_WRAP) + "\n"
			+ Base64.encodeToString(iv, Base64.NO_WRAP) + "\n"
			+ Base64.encodeToString(ct, Base64.NO_WRAP);
	}

	public String doDecrypt(String rawPass, String etxt)
	throws GeneralSecurityException, UnsupportedEncodingException {
		if (etxt != null && etxt.startsWith(MAGIC_V2 + "\n")){
			return decryptV2(rawPass, etxt);
		}
		return decryptLegacy(rawPass, etxt);
	}

	private String decryptV2(String rawPass, String etxt)
	throws GeneralSecurityException, UnsupportedEncodingException {
		String[] parts = etxt.split("\n", 4);
		if (parts.length < 4) throw new GeneralSecurityException("bad format");
		byte[] salt = Base64.decode(parts[1], Base64.DEFAULT);
		byte[] iv = Base64.decode(parts[2], Base64.DEFAULT);
		byte[] ct = Base64.decode(parts[3], Base64.DEFAULT);
		SecretKey k = deriveKeyV2(rawPass, salt);
		Cipher cp = Cipher.getInstance("AES/GCM/NoPadding");
		cp.init(Cipher.DECRYPT_MODE, k, new GCMParameterSpec(GCM_TAG_BITS, iv));
		return new String(cp.doFinal(ct), "UTF-8");
	}

	private String decryptLegacy(String rawPass, String etxt)
	throws GeneralSecurityException, UnsupportedEncodingException {
		String[] cont = etxt.split("\n{1,}");
		IvParameterSpec ivps = new IvParameterSpec(Base64.decode(cont[0], Base64.DEFAULT));
		SecretKeySpec sks = new SecretKeySpec(legacyKeyBytes(rawPass), "AES");
		Cipher cp = Cipher.getInstance("AES/CBC/PKCS5Padding");
		cp.init(Cipher.DECRYPT_MODE, sks, ivps);
		byte[] base = Base64.decode(cont[1], Base64.DEFAULT);
		return new String(Base64.decode(cp.doFinal(base, 0, base.length), Base64.DEFAULT), "UTF-8");
	}

	private static SecretKey deriveKeyV2(String rawPass, byte[] salt)
	throws GeneralSecurityException {
		SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
		KeySpec ks = new PBEKeySpec(rawPass.toCharArray(), salt, PBKDF2_ITERS, KEY_BITS);
		return new SecretKeySpec(f.generateSecret(ks).getEncoded(), "AES");
	}

	private static byte[] legacyKeyBytes(String rawPass)
	throws GeneralSecurityException, UnsupportedEncodingException {
		byte[] dg = MessageDigest.getInstance("SHA-512").digest(rawPass.getBytes("UTF-8"));
		StringBuilder sb = new StringBuilder(128);
		for (byte b : dg) sb.append(String.format("%02x", b & 0xff));
		return sb.substring(0, 32).getBytes("UTF-8");
	}
}
