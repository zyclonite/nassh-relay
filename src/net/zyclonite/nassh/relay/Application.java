/*
 * nassh-relay - Relay Server for tunneling ssh through a http endpoint
 * 
 * Website: http://relay.wsn.at
 *
 * Copyright 2014   zyclonite    networx
 *                  http://zyclonite.net
 * Developer: Lukas Prettenthaler
 */
package net.zyclonite.nassh.relay;

import java.util.concurrent.CountDownLatch;
import net.zyclonite.nassh.relay.service.VertxPlatform;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.xml.DOMConfigurator;
import org.apache.logging.julbridge.JULLog4jBridge;

/**
 *
 * @author zyclonite
 */
public class Application {

    private static final Log LOG = LogFactory.getLog(Application.class);
    private final CountDownLatch stopLatch = new CountDownLatch(1);
    private final VertxPlatform platform;
    private final Object sync = new Object();

    public Application() {
        platform = VertxPlatform.getInstance();
        platform.deployVerticle("net.zyclonite.nassh.relay.verticle.WebService");
    }

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        JULLog4jBridge.assimilate();
        DOMConfigurator.configureAndWatch(System.getProperty("user.dir") + "/log4j.xml", 60000);
        final Application main = new Application();
        LOG.info("Application started");
        main.addShutdownHook();
        main.block();
    }

    private void block() {
        while (true) {
            try {
                stopLatch.await();
                break;
            } catch (InterruptedException e) {
                //Ignore
            }
        }
    }

    private void unblock() {
        stopLatch.countDown();
    }

    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                LOG.info("Application shutting down...");
                synchronized (sync) {
                }
                platform.stop();
            }
        });
    }
}
