package com.stephensugden.guacamole.net.hmac;

import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.net.auth.AuthenticatedUser;
import org.apache.guacamole.net.auth.Credentials;
import org.apache.guacamole.net.auth.UserContext;
import org.apache.guacamole.net.auth.simple.SimpleAuthenticationProvider;
import org.apache.guacamole.net.auth.simple.SimpleConnection;
import org.apache.guacamole.net.auth.simple.SimpleConnectionDirectory;
import org.apache.guacamole.properties.GuacamoleProperties;
import org.apache.guacamole.properties.IntegerGuacamoleProperty;
import org.apache.guacamole.properties.StringGuacamoleProperty;
import org.apache.guacamole.properties.BooleanGuacamoleProperty;
import org.apache.guacamole.protocol.GuacamoleConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

import java.io.File;
import java.io.FileInputStream;

public class HmacAuthenticationProvider extends SimpleAuthenticationProvider {

    public static final long TEN_MINUTES = 10 * 60 * 1000;

    // Properties file params
    private static final StringGuacamoleProperty SECRET_KEY = new StringGuacamoleProperty() {
        @Override
        public String getName() { return "secret-key"; }
    };

    private static final StringGuacamoleProperty DEFAULT_PROTOCOL = new StringGuacamoleProperty() {
        @Override
        public String getName() { return "default-protocol"; }
    };

    private static final IntegerGuacamoleProperty TIMESTAMP_AGE_LIMIT = new IntegerGuacamoleProperty() {
        @Override
        public String getName() { return "timestamp-age-limit"; }
    };

    private static final BooleanGuacamoleProperty USE_LOCAL_PRIVKEY = new BooleanGuacamoleProperty() {
        @Override
        public String getName() { return "use-local-privkey"; }
    };

    private static final Logger logger = LoggerFactory.getLogger(HmacAuthenticationProvider.class);

    // these will be overridden by properties file if present
    private String defaultProtocol = "rdp";
    private long timestampAgeLimit = TEN_MINUTES; // 10 minutes
    private boolean useLocalPrivKey = false;

    // Per-request params
    public static final String SIGNATURE_PARAM = "signature";
    public static final String ID_PARAM = "id";
    public static final String TIMESTAMP_PARAM = "timestamp";
    public static final String PARAM_PREFIX = "guac.";

    private SignatureVerifier signatureVerifier;

    private final TimeProviderInterface timeProvider;

    public HmacAuthenticationProvider(TimeProviderInterface timeProvider) {
        this.timeProvider = timeProvider;
    }

    public HmacAuthenticationProvider() {
        timeProvider = new DefaultTimeProvider();
    }

    public String getIdentifier() {
        return "hmac";
    }

    // @Override
    public Map<String, GuacamoleConfiguration> getAuthorizedConfigurations(Credentials credentials) throws GuacamoleException {
        if (signatureVerifier == null) {
            initFromProperties();
        }

        GuacamoleConfiguration config = getGuacamoleConfiguration(credentials.getRequest());

        if (config == null) {
            return null;
        }

        Map<String, GuacamoleConfiguration> configs = new HashMap<String, GuacamoleConfiguration>();
        configs.put(config.getParameter("id"), config);
        return configs;
    }

    public UserContext updateUserContext(UserContext context, AuthenticatedUser user ) throws GuacamoleException {
        Credentials credentials = user.getCredentials();
        HttpServletRequest request = credentials.getRequest();
        GuacamoleConfiguration config = getGuacamoleConfiguration(request);
        if (config == null) {
            return null;
        }
        String id = config.getParameter("id");
        SimpleConnectionDirectory connections = (SimpleConnectionDirectory) context.getConnectionDirectory();
        SimpleConnection connection = new SimpleConnection(id, id, config);
        connections.putConnection(connection);

        return context;
    }

    private GuacamoleConfiguration getGuacamoleConfiguration(HttpServletRequest request) throws GuacamoleException {
        if (signatureVerifier == null) {
            initFromProperties();
        }
        String signature = request.getParameter(SIGNATURE_PARAM);

        logger.info("Get hmac signature: {}", signature);

        if (signature == null) {
            return null;
        }
        signature = signature.replace(' ', '+');

        String timestamp = request.getParameter(TIMESTAMP_PARAM);
        if (!checkTimestamp(timestamp)) {
            return null;
        }

        GuacamoleConfiguration config = parseConfigParams(request);

        // Hostname is required!
        if (config.getParameter("hostname") == null) {
            logger.warn("hostname parameter is missing");
            return null;
        }

        // Protocol is required!
        if (config.getProtocol() == null) {
            logger.warn("protocol parameter is missing");
            return null;
        }

        // Port is required!
        if (config.getParameter("port") == null) {
            logger.warn("port parameter is missing");
            return null;
        }

        StringBuilder message = new StringBuilder(timestamp).append(config.getProtocol());

        // Add the hostname to the message so that it cannot be changed
        message.append(config.getParameter("hostname"));

        // Do the same for the port
        message.append(config.getParameter("port"));

        // Username is not required! Append if it does exist
        if (config.getParameter("username") == null) {
            logger.warn("username is null");
        } else { message.append(config.getParameter("username")); }

        // Password is not required! Append if it does exist
        if (config.getParameter("password") == null) {
            logger.warn("password parameter is missing");
        } else { message.append(config.getParameter("password")); }

        logger.info("Recieved message: {}\nRecieved signature: {}", message.toString(), signature);

        if (!signatureVerifier.verifySignature(signature, message.toString())) {
            logger.severe("Signatures do not match.");
            return null;
        }
        String id = request.getParameter(ID_PARAM);
        if (id == null) {
            id = "DEFAULT";
        } else {
        	// This should really use BasicGuacamoleTunnelServlet's IdentfierType, but it is private!
        	// Currently, the only prefixes are both 2 characters in length, but this could become invalid at some point.
        	// see: guacamole-client@a0f5ccb:guacamole/src/main/java/org/glyptodon/guacamole/net/basic/BasicGuacamoleTunnelServlet.java:244-252
        	id = id.substring(2);
        }
        // This isn't normally part of the config, but it makes it much easier to return a single object
        config.setParameter("id", id);

        if ( useLocalPrivKey ) {
            // Look for a private key locally
            File key_file = null;
            String username = null;
            FileInputStream fis = null;
            byte[] data = null;
            String key = null;

            // Open the key_file
            try {
              username = config.getParameter("username");
              key_file = new File("/etc/guacamole/keys/" + username + "/id_rsa_guac");
            } catch (Exception ex) {
              logger.info("Exception in opening key_file:\n{}", ex.getMessage());
            }

            // Create array of same length as key
            data = new byte[(int) key_file.length()];

            // Open FileInputStream to read from and write to byte array
            try {
              fis = new FileInputStream(key_file);
            } catch (Exception ex) {
              logger.info("Exception in creating FileInputStream key_file:\n{}", ex.getMessage());
            }

            // Input data from key_file to data (byte array) using FileInputStream
            try {
              fis.read(data);
              fis.close();
            } catch (Exception ex) {
              logger.info("Exception in reading or closing data:\n{}", ex.getMessage());
            }

            // Convert data array to UTF-8 String
            try {
              key = new String(data, "UTF-8");
            } catch (Exception ex) {
              logger.info("Exception in creating key string:\n{}", ex.getMessage());
            }

            // Remove trailing newline
            key = key.substring(0, key.length() - 1);

            logger.info("Successfully gathered local private key.");

            config.setParameter("private-key", key);
            config.setParameter("sftp-private-key", key);
        }

        return config;
    }

    private boolean checkTimestamp(String ts) {
        if (timestampAgeLimit == 0) {
            return true;
        }

        if (ts == null) {
            return false;
        }

        long timestamp = Long.parseLong(ts, 10);
        long now = timeProvider.currentTimeMillis();
        return timestamp + timestampAgeLimit > now;
    }

    private GuacamoleConfiguration parseConfigParams(HttpServletRequest request) {
        GuacamoleConfiguration config = new GuacamoleConfiguration();

        Map<String, String[]> params = request.getParameterMap();

        for (String name : params.keySet()) {
            String value = request.getParameter(name);
            if (!name.startsWith(PARAM_PREFIX) || value == null || value.length() == 0) {
                continue;
            }
            else if (name.equals(PARAM_PREFIX + "protocol")) {
                config.setProtocol(request.getParameter(name));
            }
            else {
                config.setParameter(name.substring(PARAM_PREFIX.length()), request.getParameter(name));
            }
        }

        if (config.getProtocol() == null) config.setProtocol(defaultProtocol);

        return config;
    }

    private void initFromProperties() throws GuacamoleException {
        String secretKey = GuacamoleProperties.getRequiredProperty(SECRET_KEY);
        signatureVerifier = new SignatureVerifier(secretKey);
        defaultProtocol = GuacamoleProperties.getProperty(DEFAULT_PROTOCOL);
        useLocalPrivKey = GuacamoleProperties.getProperty(USE_LOCAL_PRIVKEY);
        if (defaultProtocol == null) defaultProtocol = "rdp";
        if (GuacamoleProperties.getProperty(TIMESTAMP_AGE_LIMIT) == null){
           timestampAgeLimit = TEN_MINUTES;
        }  else {
           timestampAgeLimit = GuacamoleProperties.getProperty(TIMESTAMP_AGE_LIMIT);
        }
    }
}
