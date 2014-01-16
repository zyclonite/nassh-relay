/*
 * nassh-relay - Relay Server for tunneling ssh through a http endpoint
 * 
 * Website: http://relay.wsn.at
 *
 * Copyright 2014   zyclonite    networx
 *                  http://zyclonite.net
 * Developer: Lukas Prettenthaler
 */
package net.zyclonite.nassh.relay.util;

import java.util.Arrays;
import java.util.List;

/**
 *
 * @author zyclonite
 */
public class Constants {
    public final static String SESSIONS = "map.sessions";
    public final static String SESSIONCOOKIE = "JSESSIONID";
    public final static List<String> origins = Arrays.asList("chrome-extension://okddffdblfhhnmhodogpojmfkjmhinfp", "chrome-extension://pnhechapfaindjhompbnflcldabbghjo");
}
