package com.rydr.common.util;
import org.apache.commons.codec.binary.Base64;
import javax.crypto.Cipher;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;

/**
 * RSA helper.
 *
 * <p>The key pair is supplied externally through the {@code rydr.rsa.public-key} /
 * {@code rydr.rsa.private-key} system properties, or the {@code RYDR_RSA_PUBLIC_KEY} /
 * {@code RYDR_RSA_PRIVATE_KEY} environment variables. A private key must never be committed
 * to source control.
 *
 * <p>When nothing is configured an ephemeral key pair is generated so the demo still runs;
 * anything encrypted with it is unreadable after a restart.
 */
public class RSAEncrypt {
	
	// Used to store randomly generated public and private keys
	private static final Map<Integer, String> keyMap = new HashMap<Integer, String>();  
	
	static {
		String publicKeyString = resolve("rydr.rsa.public-key", "RYDR_RSA_PUBLIC_KEY");
		String privateKeyString = resolve("rydr.rsa.private-key", "RYDR_RSA_PRIVATE_KEY");

		if (publicKeyString.isBlank() || privateKeyString.isBlank()) {
			// Nothing configured: generate a throw-away pair rather than shipping a fixed secret
			try {
				genKeyPair();
			} catch (NoSuchAlgorithmException e) {
				throw new IllegalStateException("RSA key pair generation is not available", e);
			}
		} else {
			// Save public and private keys to Map
			// 0 represents public key, 1 represents private key
			keyMap.put(0,publicKeyString);
			keyMap.put(1,privateKeyString);
		}
	}

	/**
	 * Read a setting from a system property first, then from the environment.
	 *
	 * @return the configured value, never {@code null}
	 */
	private static String resolve(String propertyName, String envName) {
		String value = System.getProperty(propertyName);
		if (value != null && !value.isBlank()) {
			return value;
		}
		value = System.getenv(envName);
		return value == null ? "" : value;
	}


	/**
	 * Randomly generate key pair
	 * @throws NoSuchAlgorithmException
	 */
	public static void genKeyPair() throws NoSuchAlgorithmException {
		// KeyPairGenerator class is used to generate public and private key pairs, based on RSA algorithm
		KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
		// Initialize key pair generator, key size is 96-1024 bits
		keyPairGen.initialize(1024,new SecureRandom());
		// Generate a key pair, saved in keyPair
		KeyPair keyPair = keyPairGen.generateKeyPair();
		// Get private key
		RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
		// Get public key
		RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
		String publicKeyString = new String(Base64.encodeBase64(publicKey.getEncoded()));
		// Get private key string
		String privateKeyString = new String(Base64.encodeBase64((privateKey.getEncoded())));
		// Save public and private keys to Map
		// 0 represents public key, 1 represents private key
		keyMap.put(0,publicKeyString);
		keyMap.put(1,privateKeyString);
	}
	/**
	 * RSA public key encryption
	 *
	 * @param str
	 *            string to encrypt
	 * @param publicKey
	 *            public key
	 * @return ciphertext
	 * @throws Exception
	 *             exception during encryption
	 */
	public static String encrypt( String str, String publicKey ) throws Exception{
		// Base64 encoded public key
		byte[] decoded = Base64.decodeBase64(publicKey);
		RSAPublicKey pubKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));
		// RSA encryption
		Cipher cipher = Cipher.getInstance("RSA");
		cipher.init(Cipher.ENCRYPT_MODE, pubKey);
		String outStr = Base64.encodeBase64String(cipher.doFinal(str.getBytes("UTF-8")));
		return outStr;
	}

	/**
	 * RSA private key decryption
	 *
	 * @param str
	 *            encrypted string
	 * @param privateKey
	 *            private key
	 * @return plaintext
	 * @throws Exception
	 *             exception during decryption
	 */
	public static String decrypt(String str, String privateKey) throws Exception{
		// Base64 decode the encrypted string
		byte[] inputByte = Base64.decodeBase64(str.getBytes("UTF-8"));
		// Base64 encoded private key
		byte[] decoded = Base64.decodeBase64(privateKey);
        RSAPrivateKey priKey = (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decoded));
		// RSA decryption
		Cipher cipher = Cipher.getInstance("RSA");
		cipher.init(Cipher.DECRYPT_MODE, priKey);
		String outStr = new String(cipher.doFinal(inputByte));
		return outStr;
	}

	public static void main(String[] args) throws Exception {
		// Generate public and private keys
//		genKeyPair();
		// String to encrypt
		String message = "Beijing Mashibing";
		System.out.println("Randomly generated public key: " + keyMap.get(0));
		System.out.println("Randomly generated private key: " + keyMap.get(1));
		String messageEn = encrypt(message,keyMap.get(0));
		System.out.println(message + "\tEncrypted string: " + messageEn);
		String messageDe = decrypt(messageEn,keyMap.get(1));
		System.out.println("Restored string: " + messageDe);
	}

}