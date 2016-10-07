/*
 * nassh-relay - Relay Server for tunneling ssh through a http endpoint
 * 
 * Website: https://github.com/zyclonite/nassh-relay
 *
 * Copyright 2014-2016   zyclonite    networx
 *                       http://zyclonite.net
 * Developer: Lukas Prettenthaler
 */
package net.zyclonite.nassh.handler;

import io.netty.handler.codec.http.QueryStringDecoder;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.VoidHandler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.shareddata.LocalMap;
import net.zyclonite.nassh.model.Session;
import net.zyclonite.nassh.util.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 *
 * @author zyclonite
 */
public class ConnectHandler implements Handler<ServerWebSocket> {

    private static Logger logger = LoggerFactory.getLogger(ConnectHandler.class);
    private final Vertx vertx;

    public ConnectHandler(final Vertx vertx) {
        this.vertx = vertx;
    }

    @Override
    public void handle(final ServerWebSocket ws) {
        ws.setWriteQueueMaxSize(Constants.QUEUEMAXSIZE);
        final MultiMap params = params(ws.uri());
        if (ws.path().equals("/connect") && params.contains("sid") && params.contains("ack") && params.contains("pos")) {
            final UUID sid = UUID.fromString(params.get("sid"));
            final LocalMap<String, Session> map = vertx.sharedData().getLocalMap(Constants.SESSIONS);
            final Session session = map.get(sid.toString());
            if(session == null || !session.isActive()){
                ws.reject();
                return;
            }
            session.setRead_count(Integer.parseInt(params.get("ack")));
            session.setWrite_count(Integer.parseInt(params.get("pos")));
            final TransferObserver observer = new TransferObserver(session, ws);
            final TransferQueue queue;
            try {
                queue = QueueFactory.getQueue(sid.toString());
            } catch (final NoSuchQueueException ex) {
                logger.warn(ex, ex.fillInStackTrace());
                ws.reject();
                ws.close();
                return;
            }
            if (queue.countObservers() == 0) {
                queue.addObserver(observer);
            }
            final Buffer buffer = queue.peek();
            if (buffer != null) {
                if (!ws.writeQueueFull()) {
                    final Buffer ackbuffer = Buffer.buffer();
                    ackbuffer.setInt(0, session.getWrite_count());
                    ackbuffer.setBuffer(4, buffer);
                    ws.write(ackbuffer);
                    queue.remove(buffer);
                }else{
                    ws.pause();
                }
            }
            logger.debug("connected");
            ws.drainHandler(new VoidHandler() {
                @Override
                public void handle() {
                    ws.resume();
                }
            });
            ws.handler(data -> {
                if(!session.isActive()) {
                    ws.close();
                    return;
                }
                if (data.length() < 4) {
                    logger.warn("wrong frame format");
                    return;
                }
                session.setWrite_count(session.getWrite_count() + data.length()-4);
                vertx.eventBus().publish(session.getHandler(), data.getBuffer(4, data.length()));
            });
            ws.closeHandler(new VoidHandler() {
                @Override
                protected void handle() {
                    queue.deleteObservers();
                    logger.debug("disconnected");
                }
            });
        } else {
            ws.reject();
        }
    }

    private MultiMap params(final String uri) {
        final QueryStringDecoder queryStringDecoder = new QueryStringDecoder(uri);
        final Map<String, List<String>> prms = queryStringDecoder.parameters();
        final MultiMap params = MultiMap.caseInsensitiveMultiMap();
        if (!prms.isEmpty()) {
            for (Map.Entry<String, List<String>> entry : prms.entrySet()) {
                params.add(entry.getKey(), entry.getValue());
            }
        }
        return params;
    }
}
