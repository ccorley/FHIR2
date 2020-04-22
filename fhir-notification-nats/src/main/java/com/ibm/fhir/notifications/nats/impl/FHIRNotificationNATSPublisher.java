/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.fhir.notifications.nats.impl;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.streaming.AckHandler;
import io.nats.streaming.NatsStreaming;
import io.nats.streaming.Options;
import io.nats.streaming.StreamingConnection;
import io.nats.streaming.StreamingConnectionFactory;

import com.ibm.fhir.notification.FHIRNotificationEvent;
import com.ibm.fhir.notification.FHIRNotificationService;
import com.ibm.fhir.notification.FHIRNotificationSubscriber;
import com.ibm.fhir.notification.exception.FHIRNotificationException;
import com.ibm.fhir.notification.util.FHIRNotificationUtil;

/**
 * This class implements the FHIR server notification service via a NATS channel.
 */
public class FHIRNotificationNATSPublisher implements FHIRNotificationSubscriber {
    private static final Logger log = Logger.getLogger(FHIRNotificationNATSPublisher.class.getName());
    private static FHIRNotificationService service = FHIRNotificationService.getInstance();

    private StreamingConnection sc = null;
    private AckHandler acb = null;
    private String channelName = null;

    // "Hide" the default constructor.
    protected FHIRNotificationNATSPublisher() {
    }

    public FHIRNotificationNATSPublisher(String clusterId, String channelName, String clientId, String servers, Properties tlsProps) {
        log.entering(this.getClass().getName(), "constructor");
        try {
            init(clusterId, channelName, clientId, servers, tlsProps);
        } finally {
            log.exiting(this.getClass().getName(), "constructor");
        }
    }

    /**
     * Performs any required initialization to allow us to publish events to the channel.
     */
    private void init(String clusterId, String channelName, String clientId, String servers, Properties tlsProps) {
        log.entering(this.getClass().getName(), "init");

        SSLContext ctx = null;

        try {
            this.channelName = channelName;
            if (log.isLoggable(Level.FINER)) {
                log.finer("ClusterId: " + clusterId);
                log.finer("Channel name: " + channelName);
                log.finer("ClientId: " + clientId);
                log.finer("Servers: " + servers);
            }

            // Make sure that the properties file contains the expected properties.
            if (clusterId == null || channelName == null || clientId == null || servers == null || servers.length() == 0) {
                throw new IllegalStateException("Config property missing from the NATS connection properties.");
            }

            if (Boolean.parseBoolean(tlsProps.getProperty("useTLS"))) {
                // Make sure that the tls properties are set.
                if (tlsProps.getProperty("truststore") == null || tlsProps.getProperty("truststore-pw") == null || 
                    tlsProps.getProperty("keystore") == null || tlsProps.getProperty("keystore-pw") == null) {
                    throw new IllegalStateException("TLS config property missing from the NATS connection properties.");
                }

                ctx = SSLUtils.createSSLContext(tlsProps);
            }

            // Create the NATS client connection options
            io.nats.client.Options.Builder builder = new io.nats.client.Options.Builder();
            builder.maxReconnects(-1);
            builder.connectionName(channelName);
            builder.servers(servers.split(","));
            if (ctx != null) {
                builder.sslContext(ctx);
            }
            io.nats.client.Options natsOptions = builder.build();

            // Create the NATS connection and the streaming connection
            Connection nc = Nats.connect(natsOptions);
            Options streamingOptions = new Options.Builder().natsConn(nc).build();
            sc = NatsStreaming.connect(clusterId, clientId, streamingOptions);

            // Create the publish callback
            acb = new AckHandler() {
                @Override
                public void onAck(String nuid, Exception ex) {
                    log.finer("Received ACK for guid: " + nuid);
                    if (ex != null) {
                        log.log(Level.SEVERE, "Error in server ack for guid " + nuid + ": " + ex.getMessage(), ex);
                    }
                }
            };

            // Register this NATS implementation as a "subscriber" with our Notification Service.
            // This means that our "notify" method will be called when the server publishes an event.
            service.subscribe(this);
            log.info("Initialized NATS publisher for channel '" + channelName + "' using servers: " + servers + ".");
        } catch (Throwable t) {
            String msg = "Caught exception while initializing NATS publisher.";
            log.log(Level.SEVERE, msg, t);
            throw new IllegalStateException(msg, t);
        } finally {
            log.exiting(this.getClass().getName(), "init");
        }
    }

    /**
     * Performs any necessary "shutdown" logic to disconnect from the channel.
     */
    public void shutdown() {
        log.entering(this.getClass().getName(), "shutdown");

        try {
            if (log.isLoggable(Level.FINE)) {   
                log.fine("Shutting down NATS publisher for channel: '" + channelName + "'.");
            }
            if (sc != null) {
               sc.close();
            }
        } catch (Throwable t) {
            String msg = "Caught exception shutting down NATS publisher for channel: '" + channelName + "'.";
            log.log(Level.SEVERE, msg, t);
            throw new IllegalStateException(msg, t);
        } finally {
            log.exiting(this.getClass().getName(), "shutdown");
        }
    }

    /**
     * Publishes an event to NATS.
     */
    @Override
    public void notify(FHIRNotificationEvent event) throws FHIRNotificationException {
        log.entering(this.getClass().getName(), "notify");
        String jsonString = null;
        try {
            jsonString = FHIRNotificationUtil.toJsonString(event, true);

            if (log.isLoggable(Level.FINE)) { 
                log.fine("Publishing NATS notification event to channel '" + channelName + "',\nmessage: " + jsonString);
            }
            
            sc.publish("FHIRNotificationEvent", jsonString.getBytes(), acb);
    
            if (log.isLoggable(Level.FINE)) {
                log.fine("Published NATS notification event to channel '" + channelName + "'");
            }
        } catch (Throwable e) {
            String msg = buildNotificationErrorMessage(channelName, (jsonString == null ? "<null>" : jsonString));
            log.log(Level.SEVERE, msg , e);
            throw new FHIRNotificationException(msg, e);
        } finally {
            log.exiting(this.getClass().getName(), "notify");
        }
    }
    
    /**
     * Builds a formatted error message to indicate a notification publication failure.
     */
    private String buildNotificationErrorMessage(String channelName, String notificationEvent) {
        return String.format("NATS publication failure; channel '%s'\nNotification event: %s\n.", channelName, notificationEvent);
    }
}

/* 
* Modified from original NATS documentation @ https://docs.nats.io/developing-with-nats/security/tls
*
* This example requires certificates to be in the java keystore format (.jks).
* To do so openssl is used to generate a pkcs12 file (.p12) from client-cert.pem and client-key.pem.
* The resulting file is then imported int a java keystore named keystore.jks using keytool which is part of java jdk.
* keytool is also used to import the CA certificate rootCA.pem into truststore.jks.  
*/
class SSLUtils {

    static KeyStore loadKeystore(String path, String password) throws Exception {
        KeyStore store = KeyStore.getInstance("PKCS12");
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(path));

        try {
            store.load(in, password.toCharArray());
        } finally {
            if (in != null) {
                in.close();
            }
        }

        return store;
    }

    static KeyManager[] createTestKeyManagers(Properties tlsProps) throws Exception {
        KeyStore store = loadKeystore(tlsProps.getProperty("keystore"), tlsProps.getProperty("keystore-pw"));
        KeyManagerFactory factory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        factory.init(store, tlsProps.getProperty("keystore-pw").toCharArray());
        return factory.getKeyManagers();
    }

    static TrustManager[] createTestTrustManagers(Properties tlsProps) throws Exception {
        KeyStore store = loadKeystore(tlsProps.getProperty("truststore"), tlsProps.getProperty("truststore-pw"));
        TrustManagerFactory factory = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        factory.init(store);
        return factory.getTrustManagers();
    }

    static SSLContext createSSLContext(Properties tlsProps) throws Exception {
        SSLContext ctx = SSLContext.getInstance("TLSv1.2");
        ctx.init(createTestKeyManagers(tlsProps), createTestTrustManagers(tlsProps), new SecureRandom());
        return ctx;
    }
}
