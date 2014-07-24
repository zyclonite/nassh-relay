/*
 * nassh-relay - Relay Server for tunneling ssh through a http endpoint
 * 
 * Website: http://relay.wsn.at
 *
 * Copyright 2014   zyclonite    networx
 *                  http://zyclonite.net
 * Developer: Lukas Prettenthaler
 */
package net.zyclonite.nassh.relay.handler;

import io.netty.handler.codec.http.QueryStringDecoder;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import net.zyclonite.nassh.relay.model.Session;
import net.zyclonite.nassh.relay.service.VertxPlatform;
import net.zyclonite.nassh.relay.util.Constants;
import net.zyclonite.nassh.relay.util.NoSuchQueueException;
import net.zyclonite.nassh.relay.util.QueueFactory;
import net.zyclonite.nassh.relay.util.TransferObserver;
import net.zyclonite.nassh.relay.util.TransferQueue;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.ServerWebSocket;
import org.vertx.java.core.http.CaseInsensitiveMultiMap;

/**
 *
 * @author zyclonite
 */
public class ConnectHandler implements Handler<ServerWebSocket> {

    private static final Log LOG = LogFactory.getLog(ConnectHandler.class);

    @Override
    public void handle(final ServerWebSocket ws) {
        ws.setWriteQueueMaxSize(Constants.QUEUEMAXSIZE);
        final MultiMap params = params(ws.uri());
        if (ws.path().equals("/connect") && params.contains("sid") && params.contains("ack") && params.contains("pos")) {
            final UUID sid = UUID.fromString(params.get("sid"));
            final ConcurrentMap<String, Session> map = VertxPlatform.getInstance().getSharedData().getMap(Constants.SESSIONS);
            if(!map.containsKey(sid.toString())){
                ws.reject();
                return;
            }
            final Session session = map.get(sid.toString());
            if(!session.isActive()){
                ws.reject();
                return;
            }
            session.setRead_count(Integer.parseInt(params.get("ack")));
            session.setWrite_count(Integer.parseInt(params.get("pos")));
            final TransferObserver observer = new TransferObserver(session, ws);
            final TransferQueue queue;
            try {
                queue = QueueFactory.getQueue(sid.toString());
            } catch (NoSuchQueueException ex) {
                LOG.warn(ex, ex.fillInStackTrace());
                ws.reject();
                ws.close();
                return;
            }
            if (queue.countObservers() == 0) {
                queue.addObserver(observer);
            }
            final Buffer buffer = queue.poll();
            if (buffer != null) {
                if (!ws.writeQueueFull()) {
                    final Buffer ackbuffer = new Buffer();
                    ackbuffer.setInt(0, session.getWrite_count());
                    ackbuffer.setBuffer(4, buffer);
                    ws.write(ackbuffer);
                }else{
                    ws.pause();
                    queue.add(buffer);
                }
            }
            LOG.debug("connected");
            ws.drainHandler(new VoidHandler() {
                @Override
                public void handle() {
                    ws.resume();
                }
            });
            ws.dataHandler(new Handler<Buffer>() {
                @Override
                public void handle(final Buffer data) {
                    if(!session.isActive()) {
                        ws.close();
                        return;
                    }
                    if (data.length() < 4) {
                        LOG.warn("wrong frame format");
                        return;
                    }
                    session.setWrite_count(session.getWrite_count() + data.length()-4);
                    VertxPlatform.getInstance().getEventBus().publish(session.getHandler(), data.getBuffer(4, data.length()));
                }
            });
            ws.closeHandler(new VoidHandler() {
                @Override
                protected void handle() {
                    queue.deleteObservers();
                    LOG.debug("disconnected");
                }
            });
        } else {
            ws.reject();
        }
    }

    private MultiMap params(final String uri) {
        final QueryStringDecoder queryStringDecoder = new QueryStringDecoder(uri);
        final Map<String, List<String>> prms = queryStringDecoder.parameters();
        final MultiMap params = new CaseInsensitiveMultiMap();
        if (!prms.isEmpty()) {
            for (Map.Entry<String, List<String>> entry : prms.entrySet()) {
                params.add(entry.getKey(), entry.getValue());
            }
        }
        return params;
    }
}
