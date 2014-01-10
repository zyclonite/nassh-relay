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

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author zyclonite
 */
public class AppConfig extends XMLConfiguration {

    private static final Log LOG = LogFactory.getLog(AppConfig.class);
    private static AppConfig instance;
    private static final String configFile = "config.xml";

    static {
        instance = new AppConfig(configFile);
    }

    private AppConfig(final String fileName) {
        super();
        this.setReloadingStrategy(new FileChangedReloadingStrategy());
        this.setDelimiterParsingDisabled(true);
        init(fileName);
    }

    private void init(final String fileName) {
        setFileName(fileName);
        try {
            load();
            LOG.info("Configuration loaded");
        } catch (ConfigurationException ex) {
            LOG.error("Configuration not loaded!");
        }
    }

    public static AppConfig getInstance() {
        return instance;
    }
}
