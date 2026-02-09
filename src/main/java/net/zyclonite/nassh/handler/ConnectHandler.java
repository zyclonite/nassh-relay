/*
 * nassh-relay - Relay Server for tunneling ssh through a http endpoint
 *
 * Website: https://github.com/zyclonite/nassh-relay
 *
 * Copyright 2014-2020   zyclonite    networx
 *                       http://zyclonite.net
 * Developer: Lukas Prettenthaler
 */
package net.zyclonite.nassh.handler;

import io.netty.handler.codec.http.QueryStringDecoder;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.ServerWebSocket;
import net.zyclonite.nassh.model.Session;
import net.zyclonite.nassh.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;

/**
 * @author zyclonite
 */
public class ConnectHandler implements Handler<ServerWebSocket> {

    private static final Logger logger = LogManager.getLogger();
    private final Vertx vertx;

    public ConnectHandler(final Vertx vertx) {
        this.vertx = vertx;
    }

    @Override
    public void handle(final ServerWebSocket ws) {
        ws.setWriteQueueMaxSize(Constants.QUEUEMAXSIZE);
        var params = params(ws.uri());
        if (ws.path().equals("/connect") && params.contains("sid") && params.contains("ack") && params.contains("pos")) {
            var sid = UUID.fromString(params.get("sid"));
            var map = vertx.sharedData().<String, Session>getLocalMap(Constants.SESSIONS);
            var session = map.get(sid.toString());
            if (session == null || session.isInactive()) {
                ws.close();
                return;
            }
            session.setRead_count(Integer.parseInt(params.get("ack")));
            session.setWrite_count(Integer.parseInt(params.get("pos")));
            var observer = new TransferObserver(session, ws);
            final TransferQueue queue;
            try {
                queue = QueueFactory.getQueue(sid.toString());
            } catch (final NoSuchQueueException ex) {
                logger.warn(() -> ex);
                ws.close();
                return;
            }
            if (queue.countObservers() == 0) {
                queue.addObserver(observer);
            }
            var buffer = queue.peek();
            if (buffer != null) {
                if (!ws.writeQueueFull()) {
                    var ackbuffer = Buffer.buffer();
                    ackbuffer.setInt(0, session.getWrite_count());
                    ackbuffer.setBuffer(4, buffer);
                    ws.write(ackbuffer);
                    queue.remove(buffer);
                } else {
                    ws.pause();
                }
            }
            logger.debug(() -> "connected");
            ws.drainHandler(_ -> ws.resume());
            ws.handler(data -> {
                if (session.isInactive()) {
                    ws.close();
                    return;
                }
                if (data.length() < 4) {
                    logger.warn(() -> "wrong frame format");
                    return;
                }
                session.setWrite_count(session.getWrite_count() + data.length() - 4);
                vertx.eventBus().publish(session.getHandler(), data.getBuffer(4, data.length()));
            });
            ws.closeHandler(_ -> {
                queue.deleteObservers();
                logger.debug(() -> "disconnected");
            });
        } else {
            ws.close();
        }
    }

    private MultiMap params(final String uri) {
        var queryStringDecoder = new QueryStringDecoder(uri);
        var prms = queryStringDecoder.parameters();
        var params = MultiMap.caseInsensitiveMultiMap();
        if (!prms.isEmpty()) {
            for (var entry : prms.entrySet()) {
                params.add(entry.getKey(), entry.getValue());
            }
        }
        return params;
    }
}
