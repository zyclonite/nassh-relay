package net.zyclonite.nassh;

import io.vertx.core.Deployable;
import io.vertx.launcher.application.HookContext;
import io.vertx.launcher.application.VertxApplication;
import io.vertx.launcher.application.VertxApplicationHooks;

import java.util.function.Supplier;

public class Application {
    public static void main(String[] args) {
        VertxApplication vertxApplication = new VertxApplication(args, new VertxApplicationHooks() {
            @Override
            public void beforeStartingVertx(HookContext context) {
                context.vertxOptions().setPreferNativeTransport(true);
            }

            @Override
            public Supplier<? extends Deployable> verticleSupplier() {
                return MainVerticle::new;
            }
        });
        vertxApplication.launch();
    }
}
