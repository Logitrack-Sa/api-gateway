package com.logitrack.gateway;

import com.logitrack.gateway.security.JwtSecurityProperties;
import com.logitrack.gateway.security.JwtTokenValidator;
import com.logitrack.gateway.security.RsaPublicKeyProvider;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.core.io.DefaultResourceLoader;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class JwtTokenContractTest {
    private static KeyPair key;
    private static JwtTokenValidator validator;

    @BeforeAll static void configure() throws Exception {
        var generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        key = generator.generateKeyPair();
        var properties = new JwtSecurityProperties();
        properties.setKeyId("integration-key");
        var provider = new RsaPublicKeyProvider(properties, new DefaultResourceLoader());
        provider.setPublicKey((RSAPublicKey) key.getPublic());
        validator = new JwtTokenValidator(provider, properties);
    }

    enum Invalid { NO_EXPIRATION, NO_ISSUER, WRONG_ISSUER, WRONG_AUDIENCE, FUTURE, WRONG_KID, NO_KID }

    @ParameterizedTest @EnumSource(Invalid.class)
    void rejectsSignedTokensOutsideTheSecurityContract(Invalid invalid) throws Exception {
        var claims = claims();
        String kid = "integration-key";
        switch (invalid) {
            case NO_EXPIRATION -> claims.expirationTime(null);
            case NO_ISSUER -> claims.issuer(null);
            case WRONG_ISSUER -> claims.issuer("another-issuer");
            case WRONG_AUDIENCE -> claims.audience("another-service");
            case FUTURE -> claims.notBeforeTime(Date.from(Instant.now().plusSeconds(300)));
            case WRONG_KID -> kid = "another-key";
            case NO_KID -> kid = null;
        }
        String token = sign(claims.build(), kid);
        assertThatIllegalArgumentException().isThrownBy(() -> validator.validateAndExtractUser(token));
    }

    @Test void acceptsTheIssuerContract() throws Exception {
        var user = validator.validateAndExtractUser(sign(claims().build(), "integration-key"));
        assertThat(user.isAdmin()).isTrue();
    }

    @Test void doesNotReplaceAnUnavailableConfiguredKeyWithATestKey() {
        var properties = new JwtSecurityProperties();
        properties.setPublicKeyLocation("classpath:missing-public-key.pem");
        var provider = new RsaPublicKeyProvider(properties, new DefaultResourceLoader());
        assertThatIllegalStateException().isThrownBy(provider::init);
    }

    private static JWTClaimsSet.Builder claims() {
        return new JWTClaimsSet.Builder().subject(UUID.randomUUID().toString())
                .issuer("https://security-service").audience("shipment-service")
                .expirationTime(Date.from(Instant.now().plusSeconds(300)))
                .claim("roles", List.of("ADMIN"));
    }

    private static String sign(JWTClaimsSet claims, String kid) throws Exception {
        var token = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(kid).build(), claims);
        token.sign(new RSASSASigner(key.getPrivate()));
        return token.serialize();
    }
}
