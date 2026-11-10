package com.logitrack.gateway.security;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import static org.assertj.core.api.Assertions.assertThat;
class PublicChatPathMatcherTest {
    @Test void onlyPublicMessagePostAllowsAnonymousRequests() {
        var paths=new SecurityPathMatcher();
        assertThat(paths.isPublic("/api/v1/chat/public/message",HttpMethod.POST)).isTrue();
        assertThat(paths.isPublic("/api/v1/chat/public/message",HttpMethod.GET)).isFalse();
        assertThat(paths.isPublic("/api/v1/chat/stream",HttpMethod.GET)).isFalse();
        assertThat(paths.isPublic("/api/v1/chat/public/other",HttpMethod.POST)).isFalse();
    }
}
