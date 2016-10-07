/*
 * nassh-relay - Relay Server for tunneling ssh through a http endpoint
 * 
 * Website: https://github.com/zyclonite/nassh-relay
 *
 * Copyright 2014-2016   zyclonite    networx
 *                       http://zyclonite.net
 * Developer: Lukas Prettenthaler
 */
package net.zyclonite.nassh.util;

import java.util.Arrays;
import java.util.List;

/**
 *
 * @author zyclonite
 */
public class Constants {
    public final static int QUEUEMAXSIZE = 1000;
    public final static String SESSIONS = "map.sessions";
    public final static String SESSIONCOOKIE = "JSESSIONID";
    public final static List<String> ORIGINS = Arrays.asList("chrome-extension://okddffdblfhhnmhodogpojmfkjmhinfp", "chrome-extension://pnhechapfaindjhompbnflcldabbghjo");
}
