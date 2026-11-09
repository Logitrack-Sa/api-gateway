package com.logitrack.gateway.security;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import static org.assertj.core.api.Assertions.assertThat;
class PaymentPathMatcherTest {
 @Test void onlyProviderWebhookIsPublic() {
  var paths=new SecurityPathMatcher();
  assertThat(paths.isPublic("/api/v1/payments/webhooks/mercadopago",HttpMethod.POST)).isTrue();
  assertThat(paths.isPublic("/api/v1/payments/webhooks/mercadopago",HttpMethod.GET)).isFalse();
  assertThat(paths.isPublic("/api/v1/payments/shipments/123/checkout",HttpMethod.POST)).isFalse();
  assertThat(paths.isPublic("/api/v1/payments/shipments/123",HttpMethod.GET)).isFalse();
 }
 @Test void clientsAndAdminCanAccessPaymentsButNotCouriers() {
  var paths=new SecurityPathMatcher();
  for(String role:List.of("ADMIN","CLIENTE","REPARTIDOR")) {
   var user=new AuthenticatedUser(UUID.randomUUID(),Set.of(role),null,null);
   assertThat(paths.isAuthorized(user,"/api/v1/payments/shipments/123",HttpMethod.GET)).isEqualTo(!role.equals("REPARTIDOR"));
  }
 }
}
