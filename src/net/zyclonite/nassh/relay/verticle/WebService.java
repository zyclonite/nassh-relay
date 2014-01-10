/*
 * nassh-relay - Relay Server for tunneling ssh through a http endpoint
 * 
 * Website: http://relay.wsn.at
 *
 * Copyright 2014   zyclonite    networx
 *                  http://zyclonite.net
 * Developer: Lukas Prettenthaler
 */
package net.zyclonite.nassh.relay.verticle;

import net.zyclonite.nassh.relay.handler.ConnectHandler;
import net.zyclonite.nassh.relay.handler.CookieHandler;
import net.zyclonite.nassh.relay.handler.ProxyHandler;
import net.zyclonite.nassh.relay.handler.ReadHandler;
import net.zyclonite.nassh.relay.handler.WriteHandler;
import net.zyclonite.nassh.relay.util.AppConfig;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.net.NetClient;
import org.vertx.java.core.shareddata.SharedData;
import org.vertx.java.platform.Verticle;

/**
 *
 * @author zyclonite
 */
public class WebService extends Verticle {

    private static final Log LOG = LogFactory.getLog(WebService.class);

    @Override
    public void start() {
        final AppConfig config = AppConfig.getInstance();
        final HttpServer server = vertx.createHttpServer();
        RouteMatcher routeMatcher = new RouteMatcher();
        routeMatcher.get("/cookie", new CookieHandler());
        routeMatcher.get("/proxy", new ProxyHandler());
        routeMatcher.get("/write", new WriteHandler());
        routeMatcher.get("/read", new ReadHandler());
        server.requestHandler(routeMatcher);
        server.websocketHandler(new ConnectHandler());
        server.listen(config.getInteger("webservice.webport", 8080));
        LOG.debug("WebService initialized");
    }

    public long setTimer(final long timeout, final Handler<Long> handler) {
        return vertx.setTimer(timeout, handler);
    }

    public long setPeriodic(final long timeout, final Handler<Long> handler) {
        return vertx.setPeriodic(timeout, handler);
    }

    public void cancelTimer(final long timerid) {
        vertx.cancelTimer(timerid);
    }

    public NetClient createNetClient() {
        return vertx.createNetClient();
    }

    public EventBus getEventBus() {
        return vertx.eventBus();
    }
    
    public SharedData getSharedData() {
        return vertx.sharedData();
    }
}
