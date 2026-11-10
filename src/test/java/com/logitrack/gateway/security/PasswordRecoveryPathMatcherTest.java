package com.logitrack.gateway.security;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import static org.assertj.core.api.Assertions.assertThat;

class PasswordRecoveryPathMatcherTest {
    @ParameterizedTest
    @ValueSource(strings={"/api/v1/auth/forgot-password","/api/v1/auth/reset-password",
            "/api/auth/forgot-password","/api/auth/reset-password"})
    void exposesOnlyExactPostOperations(String path) {
        var paths = new SecurityPathMatcher();
        assertThat(paths.isPublic(path,HttpMethod.POST)).isTrue();
        assertThat(paths.isPublic(path,HttpMethod.GET)).isFalse();
        assertThat(paths.isPublic(path+"/other",HttpMethod.POST)).isFalse();
    }
}
