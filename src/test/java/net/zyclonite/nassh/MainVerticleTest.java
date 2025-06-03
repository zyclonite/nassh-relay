package net.zyclonite.nassh;

import io.vertx.core.Vertx;
import io.vertx.core.internal.VertxInternal;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
public class MainVerticleTest {
    @Test
    void resolverTest(Vertx vertx, VertxTestContext testContext) {
        ((VertxInternal)vertx).nameResolver()
            .resolve("google.com")
            .andThen(ar -> {
            if(ar.succeeded()) {
                var inetAddress = ar.result();
                System.out.println(inetAddress);
            } else {
                ar.cause().printStackTrace();
            }
            testContext.completeNow();
        });
    }
}
