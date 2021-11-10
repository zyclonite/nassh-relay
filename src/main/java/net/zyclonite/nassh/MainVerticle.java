/*
 * nassh-relay - Relay Server for tunneling ssh through a http endpoint
 *
 * Website: https://github.com/zyclonite/nassh-relay
 *
 * Copyright 2014-2020   zyclonite    networx
 *                       http://zyclonite.net
 * Developer: Lukas Prettenthaler
 */
package net.zyclonite.nassh;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.SelfSignedCertificate;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.CorsHandler;
import net.zyclonite.nassh.handler.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@SuppressWarnings("unused")
public class MainVerticle extends AbstractVerticle {
    private static final Logger logger = LogManager.getLogger();
    private HttpServer server;

    @Override
    public void start(final Promise<Void> startPromise) {
        final JsonObject webserviceConfig = config().getJsonObject("webservice");
        if (webserviceConfig.containsKey("hostname")) {
            logger.warn(() -> "webservice.hostname will be deprecated in future releases, please use webservice.host instead");
            webserviceConfig.put("host", webserviceConfig.getString("hostname", webserviceConfig.getString("host")));
        }
        final HttpServerOptions options = new HttpServerOptions(webserviceConfig);
        if (options.isSsl() && options.getKeyCertOptions() == null) {
            logger.warn(() -> "no certificate configured, creating self-signed");
            final SelfSignedCertificate certificate = SelfSignedCertificate.create();
            options.setKeyCertOptions(certificate.keyCertOptions());
            options.setTrustOptions(certificate.trustOptions());
        }
        server = vertx.createHttpServer(options);
        final Router router = Router.router(vertx);
        router.route().handler(CorsHandler
            .create(".*")
            .allowCredentials(true)
        );
        router.get("/cookie").handler(new CookieHandler(config().getJsonObject("application").copy().put("auth", config().getJsonObject("google-sso"))));
        router.post("/cookie").handler(new CookiePostHandler(vertx, new JsonObject().put("auth", config().getJsonObject("google-sso"))));
        router.get("/proxy").handler(new ProxyHandler(vertx, config()));
        router.get("/write").handler(new WriteHandler(vertx));
        router.get("/read").handler(new ReadHandler(vertx));
        server.requestHandler(router);
        server.webSocketHandler(new ConnectHandler(vertx));
        server.listen(result -> {
                if (result.succeeded()) {
                    logger.info(() -> "nassh-relay listening on port " + result.result().actualPort());
                    startPromise.complete();
                } else {
                    startPromise.fail(result.cause());
                }
            }
        );
    }

    @Override
    public void stop(final Promise<Void> stopPromise) {
        logger.debug(() -> "stopped");
        if (server != null) {
            server.close(complete -> stopPromise.complete());
        } else {
            stopPromise.complete();
        }
    }
}
