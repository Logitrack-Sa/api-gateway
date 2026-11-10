package com.logitrack.gateway.security;

import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import static org.assertj.core.api.Assertions.assertThat;

class CancellationPathMatcherTest {
    @Test void cancellationRequiresClientOrAdminAndRemainsPrivate() {
        var paths = new SecurityPathMatcher();
        String path = "/api/v1/shipments/" + UUID.randomUUID() + "/cancellation";
        assertThat(paths.isPublic(path,HttpMethod.POST)).isFalse();
        for (String role : Set.of("ADMIN","CLIENTE","REPARTIDOR")) {
            var user = new AuthenticatedUser(UUID.randomUUID(),Set.of(role),null,null);
            assertThat(paths.isAuthorized(user,path,HttpMethod.POST)).isEqualTo(!role.equals("REPARTIDOR"));
        }
    }
}
