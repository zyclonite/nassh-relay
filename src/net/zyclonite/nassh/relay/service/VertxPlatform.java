/*
 * nassh-relay - Relay Server for tunneling ssh through a http endpoint
 * 
 * Website: http://relay.wsn.at
 *
 * Copyright 2014   zyclonite    networx
 *                  http://zyclonite.net
 * Developer: Lukas Prettenthaler
 */
package net.zyclonite.nassh.relay.service;

import java.net.MalformedURLException;
import java.net.URL;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.net.NetClient;
import org.vertx.java.core.shareddata.SharedData;
import org.vertx.java.platform.PlatformLocator;
import org.vertx.java.platform.PlatformManager;

/**
 *
 * @author zyclonite
 */
public class VertxPlatform implements Handler<AsyncResult<String>> {

    private static final Log LOG = LogFactory.getLog(VertxPlatform.class);
    private final PlatformManager mgr;
    private static final VertxPlatform instance;

    static {
        instance = new VertxPlatform();
    }

    private VertxPlatform() {
        mgr = PlatformLocator.factory.createPlatformManager();
        init();
    }

    private void init() {
        mgr.deployVerticle("net.zyclonite.nassh.relay.verticle.WebService", null, getClassPathAsURLArray(), 1, null, this);
    }

    public SharedData getSharedData() {
        return mgr.vertx().sharedData();
    }

    public EventBus getEventBus() {
        return mgr.vertx().eventBus();
    }

    public NetClient createNetClient() {
        return mgr.vertx().createNetClient();
    }

    public void stop() {
        mgr.stop();
    }

    private static URL[] getClassPathAsURLArray() {
        String classPath = System.getProperty("java.class.path");
        String[] splitClassPath = classPath.split(";");
        URL[] classPathAsURLArray = new URL[splitClassPath.length];
        for (int i = 0; i < splitClassPath.length; i++) {
            try {
                classPathAsURLArray[i] = new URL("file:///" + splitClassPath[i].replace('\\', '/'));
            } catch (MalformedURLException ex) {
                LOG.warn(ex, ex.fillInStackTrace());
                classPathAsURLArray = null;
            }
        }
        return classPathAsURLArray;
    }

    public static VertxPlatform getInstance() {
        return instance;
    }

    @Override
    public void handle(AsyncResult<String> done) {
        if(done.succeeded()){
            LOG.info("WebService deployed");
        }else{
            LOG.error("WebService NOT deployed " + done.result());
        }
    }
}
