package com.ohadr.crypto.service;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;






import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.log4j.Logger;

import com.ohadr.crypto.exception.CryptoException;
import com.ohadr.crypto.interfaces.CryptoProvider;
import com.ohadr.crypto.interfaces.KeyHive;


public class DefaultCryptoProvider implements CryptoProvider
{
	private static final Logger logger = Logger.getLogger(DefaultCryptoProvider.class);

	private static final String SYMMETRIC_ALGORITHM = "AES/CBC/PKCS5Padding";
	private static final int SYMMETRIC_KEY_LENGTH = 256;

	private static final String ASYMMETRIC_ALGORITHM = "DSA";
	private static final String ASSYMETRIC_SIGNATURE_ALGORITHM = "SHA256withDSA";
	private static final int ASYMMETRIC_KEY_SIZE = 1024;

	public static final String KEYSTORE_TYPE = "JCEKS";

	private static final String ASYMMETRIC_KEY_NAME = "WatchDox_DSA";

	private static final byte[] ZERO_IV = new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

	private final KeyStore keyStore;
	private final Map<KeyHive, Key> keys = null;
	private PrivateKey privateKey;
	private Certificate certificate;

	public DefaultCryptoProvider(String keystoreFile, String keystorePassword)
	{
		try
		{
/*			Security.addProvider(new BouncyCastleProvider());

			// Wait intil bouncy castle provider is loaded - to preven exceptions later on
			for (int i=0; i<10; i++){
				try {
					Cipher.getInstance("RSA/None/OAEPWithSHA1AndMGF1Padding", BouncyCastleProvider.PROVIDER_NAME);
					break;
				} catch(NoSuchAlgorithmException e){
					logger.info("Waiting for Bouncy Castel to load...");
					Thread.sleep(5000);
					continue;
				}
			}
*/			
			keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
			logger.info("Using keystore " + keystoreFile);
			loadMasterKeys(keystoreFile, keystorePassword);
		}
		catch (Exception e)
		{
			throw new CryptoException("Failed initializing keystore from file " + keystoreFile, e);
		}
	}

	
	/**
	 * 
	 * @param alias
	 * @param password
	 * @return
	 * @throws NoSuchAlgorithmException
	 * /
	private KeyWithCertificate generateAndSaveAssymetricKeys(String alias, char[] password) throws NoSuchAlgorithmException
	{
		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(ASYMMETRIC_ALGORITHM);
		keyPairGenerator.initialize(ASYMMETRIC_KEY_SIZE);
		KeyPair keys = keyPairGenerator.generateKeyPair();

		String domainName = "watchdox.com";

		X500Name issuer = new X500Name("CN=" + domainName + ", OU=None, O=None L=None, C=None");
		BigInteger serial = BigInteger.valueOf(Math.abs(new SecureRandom().nextInt()));
		Date notBefore = new Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 30);
		Date notAfter = new Date(System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 365 * 30));
		X500Name subject = new X500Name("CN=" + domainName + ", OU=None, O=None L=None, C=None");
		JcaX509v3CertificateBuilder v3CertGen = new JcaX509v3CertificateBuilder(issuer, serial, notBefore, notAfter,
		    subject, keys.getPublic());

		// TODO test me !

		try
		{
			Certificate selfSignedCertificate = new JcaX509CertificateConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME)
			                                                                     .getCertificate(v3CertGen.build(new JcaContentSignerBuilder(
			                                                                         ASSYMETRIC_SIGNATURE_ALGORITHM).setProvider(BouncyCastleProvider.PROVIDER_NAME)
			                                                                                                        .build(keys.getPrivate())));
			keyStore.setKeyEntry(alias, keys.getPrivate(), password, new java.security.cert.Certificate[] {selfSignedCertificate});

			return new KeyWithCertificate(keys.getPrivate(), selfSignedCertificate);
		}
		catch (GeneralSecurityException e)
		{
			// Shouldn't happen since we've just created the key
			throw new CryptoException("Failed generating self-signed certificate for the server", e);
		}
		catch (OperatorCreationException e)
		{
			throw new CryptoException("Failed generating self-signed certificate for the server", e);
		}
	}*/

	private void loadMasterKeys(String fileName, String password) throws NoSuchAlgorithmException,
	                                                             KeyStoreException,
	                                                             CertificateException,
	                                                             FileNotFoundException,
	                                                             IOException
	{
		boolean keystoreModified = false;
		KeyGenerator keyGen = null;
		try
		{
			keyStore.load(new FileInputStream(fileName), password.toCharArray());
		}
		catch (FileNotFoundException e)
		{
			logger.info("Keystore file does not exist; Will try to create a new one");
			keyStore.load(null, null);
		}

		// Load Symmetric keys
/*		for (KeyHive hive : KeyHive.values())
		{
			Key key = null;
			String keyAlias = "WatchDox_" + hive.toString();
			char[] keyPassword = (password + "__" + keyAlias).toCharArray();
			try
			{
				key = keyStore.getKey(keyAlias, keyPassword);
			}
			catch (UnrecoverableKeyException e)
			{
				 This key does not exist; Will be created by the next if statement 
			}
			if (key == null)
			{
				if (keyGen == null)
				{
					keyGen = KeyGenerator.getInstance("AES");
					keyGen.init(SYMMETRIC_KEY_LENGTH);
				}
				logger.info("Creating NEW symmetric key: " + keyAlias);
				key = keyGen.generateKey();
				keyStore.setKeyEntry(keyAlias, key, keyPassword, null);
				keystoreModified = true;
			}
			else
			{
				logger.info("Loaded symmetric key: " + keyAlias);
			}
			keys.put(hive, key);
		}*/

		// Load asymmetric keys
		char[] keyPassword = (password + "__" + ASYMMETRIC_KEY_NAME).toCharArray();
		try
		{
			privateKey = (PrivateKey) keyStore.getKey(ASYMMETRIC_KEY_NAME, keyPassword);
			certificate = keyStore.getCertificate(ASYMMETRIC_KEY_NAME);
		}
		catch (UnrecoverableKeyException e)
		{
			privateKey = null;
		}

		if ((privateKey == null) || (certificate == null))
		{
			logger.info("Creating NEW asymmetric keypair: " + ASYMMETRIC_KEY_NAME);
//			KeyWithCertificate keys = generateAndSaveAssymetricKeys(ASYMMETRIC_KEY_NAME, keyPassword);
//			privateKey = keys.getPrivateKey();
//			certificate = keys.getCertificate();
//			keystoreModified = true;
		}
		else
		{
			logger.info("Loaded asymmetric key-pair: " + ASYMMETRIC_KEY_NAME);
		}

		if (keystoreModified)
		{
			// We loaded some keys, we need to update the keystore
			keyStore.store(new FileOutputStream(fileName), password.toCharArray());
		}

	}

//TODO: needed?
/*	private Key getSeeded256BitKey(KeyHive hive, String seed)
	{
		try
		{
			MessageDigest md = MessageDigest.getInstance("SHA256");
			byte[] digest = md.digest(seed.getBytes());
			Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
			Key masterKey = keys.get(hive);
			cipher.init(Cipher.ENCRYPT_MODE, masterKey, new IvParameterSpec(ZERO_IV));
			byte[] generatedKey = cipher.doFinal(digest);
			return new SecretKeySpec(generatedKey, "AES");
		}
		catch (GeneralSecurityException e)
		{
			throw new CryptoException("Failed to create an extractable 256 bit key", e);
		}
	}
*/
	
	public Key getSeededKey(ImmutablePair<KeyHive, String> keyParams)
	{
		Key masterKey = keys.get(keyParams.getLeft());
		if (masterKey.getEncoded().length == 16)
		{
			if (keyParams.getLeft() == KeyHive.SYSTEM)
			{
				// System hive did not support seeding, so we just return the master key itself.
				return masterKey;
			}

			// Old, 128-bit keys. We will use the old algorithm
			try
			{
				Cipher cipher = javax.crypto.Cipher.getInstance("AES");
				cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, masterKey);
				byte[] encrypted = cipher.doFinal(keyParams.getRight().replaceAll("-", "").getBytes());
				if (encrypted.length > 16)
				{
					encrypted = Arrays.copyOfRange(encrypted, 0, 16);
				}
				return new SecretKeySpec(encrypted, "AES");
			}
			catch (GeneralSecurityException e)
			{
				throw new CryptoException("Failed to create an extractable 256 bit key", e);
			}
		}
		else
		{
			// New, 256-bit keys. We use a standard hashing function for generating the seeded key.
			return 
					//getSeeded256BitKey(keyParams.getLeft(), keyParams.getRight());
					null;
		}
	}

	public Cipher getCipher(Key key, int cryptMode) throws InvalidKeyException
	{
		Cipher cipher = null;
		try
		{
			if (key.getEncoded().length > 16)
			{
				cipher = javax.crypto.Cipher.getInstance(SYMMETRIC_ALGORITHM);
			}
			else
			{
				cipher = javax.crypto.Cipher.getInstance("AES");
			}
		}
		catch (GeneralSecurityException e)
		{
			throw new CryptoException("Cipher creation failed", e);
		}
		try
		{
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] ivContent = md.digest(key.getEncoded());
			cipher.init(cryptMode, key, new IvParameterSpec(ivContent));
		}
		catch (InvalidAlgorithmParameterException e)
		{
			throw new CryptoException("Cipher creation failed", e);
		}
		catch (NoSuchAlgorithmException e)
		{
			throw new CryptoException("Cipher creation failed", e);
		}
		return cipher;
	}
}
