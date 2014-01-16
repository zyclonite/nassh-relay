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

import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.gson.Gson;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import net.zyclonite.nassh.relay.handler.CookiePostHandler;
import net.zyclonite.nassh.relay.handler.ConnectHandler;
import net.zyclonite.nassh.relay.handler.CookieHandler;
import net.zyclonite.nassh.relay.handler.ProxyHandler;
import net.zyclonite.nassh.relay.handler.ReadHandler;
import net.zyclonite.nassh.relay.handler.WriteHandler;
import net.zyclonite.nassh.relay.util.AppConfig;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.platform.Verticle;

/**
 *
 * @author zyclonite
 */
public class WebService extends Verticle {

    private static final Log LOG = LogFactory.getLog(WebService.class);
    public static final HttpTransport TRANSPORT = new NetHttpTransport();
    public static final GsonFactory JSON_FACTORY = new GsonFactory();
    public static final Gson GSON = new Gson();
    public static final GoogleClientSecrets clientSecrets;

    static {
        try {
            final Reader reader = new FileReader("client_secrets.json");
            clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, reader);
        } catch (IOException e) {
            throw new Error("No client_secrets.json found", e);
        }
    }
    public static final String CLIENT_ID = clientSecrets.getWeb().getClientId();
    public static final String CLIENT_SECRET = clientSecrets.getWeb().getClientSecret();
    public static final String APPLICATION_NAME = "nassh-relay-authentication-provider";

    @Override
    public void start() {
        final AppConfig config = AppConfig.getInstance();
        final HttpServer server = vertx.createHttpServer();
        RouteMatcher routeMatcher = new RouteMatcher();
        routeMatcher.get("/cookie", new CookieHandler());
        routeMatcher.post("/cookie", new CookiePostHandler());
        routeMatcher.get("/proxy", new ProxyHandler());
        routeMatcher.get("/write", new WriteHandler());
        routeMatcher.get("/read", new ReadHandler());
        server.requestHandler(routeMatcher);
        server.websocketHandler(new ConnectHandler());
        server.listen(config.getInteger("webservice.webport", 8080), config.getString("webservice.hostname", "localhost"));
        LOG.info("WebService initialized");
    }
}
