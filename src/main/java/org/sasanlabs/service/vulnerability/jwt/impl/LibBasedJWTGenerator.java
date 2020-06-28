package org.sasanlabs.service.vulnerability.jwt.impl;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.util.Base64URL;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.text.ParseException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.sasanlabs.service.exception.ExceptionStatusCodeEnum;
import org.sasanlabs.service.exception.ServiceApplicationException;
import org.sasanlabs.service.vulnerability.jwt.IJWTTokenGenerator;
import org.sasanlabs.service.vulnerability.jwt.bean.JWTUtils;
import org.springframework.stereotype.Component;

/**
 * Creates JWT token based on multiple libraries.
 *
 * @author KSASAN preetkaran20@gmail.com
 */
@Component
public class LibBasedJWTGenerator implements IJWTTokenGenerator {
    /**
     * Signs token using provided secretKey based on the provided algorithm. This method only
     * handles signing of token using HS*(Hmac + Sha*) based algorithm <br>
     *
     * <p>Note: This method adds custom java based implementation of HS* algorithm and doesn't use
     * any library like Nimbus+JOSE or JJWT and reason for this is, libraries are having validations
     * related to Key sizes and they doesn't allow weak keys so for signing token using weak keys
     * (for finding vulnerabilities in web applications that are using old implementations or custom
     * implementations) is not possible therefore added this custom implementation for HS*
     * algorithms.
     *
     * <p>
     *
     * @param token
     * @param secretKey
     * @param algorithm
     * @return
     * @throws JWTExtensionValidationException
     * @throws UnsupportedEncodingException
     */
    private String getBase64EncodedHMACSignedToken(byte[] token, byte[] secretKey, String algorithm)
            throws ServiceApplicationException, UnsupportedEncodingException {
        try {
            if (JWTUtils.JWT_HMAC_ALGO_TO_JAVA_ALGORITHM_MAPPING.containsKey(algorithm)) {
                Mac hmacSHA =
                        Mac.getInstance(
                                JWTUtils.JWT_HMAC_ALGO_TO_JAVA_ALGORITHM_MAPPING.get(algorithm));
                SecretKeySpec hmacSecretKey = new SecretKeySpec(secretKey, hmacSHA.getAlgorithm());
                hmacSHA.init(hmacSecretKey);
                byte[] tokenSignature = hmacSHA.doFinal(token);
                String base64EncodedSignature =
                        JWTUtils.getBase64UrlSafeWithoutPaddingEncodedString(tokenSignature);
                return base64EncodedSignature;
            } else {
                throw new ServiceApplicationException(
                        ExceptionStatusCodeEnum.SYSTEM_ERROR,
                        algorithm + " is not a supported HMAC algorithm.");
            }
        } catch (InvalidKeyException | NoSuchAlgorithmException | IOException e) {
            throw new ServiceApplicationException(
                    "Exception occurred while Signing token: " + JWTUtils.getString(token),
                    e,
                    ExceptionStatusCodeEnum.SYSTEM_ERROR);
        }
    }

    @Override
    public String getHMACSignedJWTToken(String tokenToBeSigned, byte[] key, String algorithm)
            throws UnsupportedEncodingException, ServiceApplicationException {
        return tokenToBeSigned
                + JWTUtils.JWT_TOKEN_PERIOD_CHARACTER
                + getBase64EncodedHMACSignedToken(
                        tokenToBeSigned.getBytes("UTF-8"), key, algorithm);
    }

    @Override
    public String getJWTToken_RS256(String tokenToBeSigned, PrivateKey privateKey)
            throws ServiceApplicationException {
        RSASSASigner rsassaSigner = new RSASSASigner(privateKey);
        String[] jwtParts = tokenToBeSigned.split(JWTUtils.JWT_TOKEN_PERIOD_CHARACTER_REGEX, -1);
        try {
            return tokenToBeSigned
                    + JWTUtils.JWT_TOKEN_PERIOD_CHARACTER
                    + rsassaSigner.sign(
                            JWSHeader.parse(Base64URL.from(jwtParts[0])),
                            JWTUtils.getBytes(tokenToBeSigned));
        } catch (UnsupportedEncodingException | JOSEException | ParseException e) {
            throw new ServiceApplicationException(
                    ExceptionStatusCodeEnum.SYSTEM_ERROR,
                    "Exception occurred while Signing token: " + tokenToBeSigned,
                    e);
        }
    }
}
