package org.sasanlabs;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class SasanSecurityTesting {
	
	public static void main(String[] args) throws NoSuchAlgorithmException {
		// TODO Auto-generated method stub
		byte[] key = new byte[20];
		SecureRandom sc = SecureRandom.getInstanceStrong();
		sc.setSeed(125);
		sc.nextBytes(key);
		System.out.println(Base64.getEncoder().encodeToString(key).toString());
	}

}
