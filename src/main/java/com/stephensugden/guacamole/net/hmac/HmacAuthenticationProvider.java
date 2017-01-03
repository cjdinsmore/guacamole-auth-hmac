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
import org.apache.guacamole.protocol.GuacamoleConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

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

    private static final Logger logger = LoggerFactory.getLogger(HmacAuthenticationProvider.class);

    // these will be overridden by properties file if present
    private String defaultProtocol = "rdp";
    private long timestampAgeLimit = TEN_MINUTES; // 10 minutes

    // Per-request params
    public static final String SIGNATURE_PARAM = "signature";
    public static final String ID_PARAM = "id";
    public static final String TIMESTAMP_PARAM = "timestamp";
    public static final String PARAM_PREFIX = "guac.";

    private static final List<String> SIGNED_PARAMETERS = new ArrayList<String>() {{
        add("username");
        add("password");
        add("hostname");
        add("port");
    }};

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

    @Override
    public Map<String, GuacamoleConfiguration> getAuthorizedConfigurations(Credentials credentials) throws GuacamoleException {
        if (signatureVerifier == null) {
            initFromProperties();
        }

        GuacamoleConfiguration config = getGuacamoleConfiguration(credentials.getRequest());

        if (config == null) {
            // logger.warn("getAuthorizedConfigurations method returned NULL");
            return null;
        }

        Map<String, GuacamoleConfiguration> configs = new HashMap<String, GuacamoleConfiguration>();
        configs.put(config.getParameter("id"), config);
        // logger.warn("getAuthorizedConfigurations method returned configs");
        return configs;
    }

    public UserContext updateUserContext(UserContext context, AuthenticatedUser user ) throws GuacamoleException {
        Credentials credentials = user.getCredentials();
        HttpServletRequest request = credentials.getRequest();
        GuacamoleConfiguration config = getGuacamoleConfiguration(request);
        if (config == null) {
            // logger.warn("updateUserContext method returned NULL");
            return null;
        }
        String id = config.getParameter("id");
        SimpleConnectionDirectory connections = (SimpleConnectionDirectory) context.getConnectionDirectory();
        SimpleConnection connection = new SimpleConnection(id, id, config);
        connections.putConnection(connection);
        // logger.warn("updateUserContext method returned context");
        return context;
    }

    private GuacamoleConfiguration getGuacamoleConfiguration(HttpServletRequest request) throws GuacamoleException {
        if (signatureVerifier == null) {
            initFromProperties();
        }
        String signature = request.getParameter(SIGNATURE_PARAM);

        logger.debug("Get hmac signature: {}", signature);

        if (signature == null) {
            // logger.warn("getGuacamoleConfiguration method returned NULL bc signature==null");
            return null;
        }
        signature = signature.replace(' ', '+');

        String timestamp = request.getParameter(TIMESTAMP_PARAM);
        if (!checkTimestamp(timestamp)) {
            // logger.warn("getGuacamoleConfiguration method returned NULL bc !checkTimestamp(timestamp)");
            return null;
        }

        GuacamoleConfiguration config = parseConfigParams(request);

        // Hostname is required!
        if (config.getParameter("hostname") == null) {
            // logger.warn("getGuacamoleConfiguration method returned NULL bc config.getParameter('hostname') == null");
            return null;
        }

        // Hostname is required!
        if (config.getProtocol() == null) {
            // logger.warn("getGuacamoleConfiguration method returned NULL bc config.getProtocol() == null");
            return null;
        }

        StringBuilder message = new StringBuilder(timestamp)
                .append(config.getProtocol());

        for (String name : SIGNED_PARAMETERS) {
            String value = config.getParameter(name);
            if (value == null) {
                continue;
            }
            // This loop goes through the SIGNED_PARAMETERS and if a value is not null,
            // it is added to the message that will be hashed into signature.
            // I will try it without adding these values.
            // Result of removing this: IT WORKS!
            // message.append(name);
            // message.append(value);
        }

        logger.debug("Get hmac message: {}", message.toString());

        if (!signatureVerifier.verifySignature(signature, message.toString())) {
            // logger.warn("getGuacamoleConfiguration method returned NULL bc !signatureVerifier.verifySignature(signature, message.toString())");
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
        // logger.warn("getGuacamoleConfiguration method returned configs");
        return config;
    }

    private boolean checkTimestamp(String ts) {
        if (timestampAgeLimit == 0) {
            // logger.warn("checkTimestamp returns TRUE");
            return true;
        }

        if (ts == null) {
            // logger.warn("checkTimestamp returns FALSE");
            return false;
        }

        long timestamp = Long.parseLong(ts, 10);
        long now = timeProvider.currentTimeMillis();
        // logger.warn("checkTimestamp returns 'timestamp + timestampAgeLimit > now'");
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

        // logger.warn("parseConfigParams returns config");
        return config;
    }

    private void initFromProperties() throws GuacamoleException {
        String secretKey = GuacamoleProperties.getRequiredProperty(SECRET_KEY);
        signatureVerifier = new SignatureVerifier(secretKey);
        defaultProtocol = GuacamoleProperties.getProperty(DEFAULT_PROTOCOL);
        if (defaultProtocol == null) defaultProtocol = "rdp";
        if (GuacamoleProperties.getProperty(TIMESTAMP_AGE_LIMIT) == null){
           timestampAgeLimit = TEN_MINUTES;
        }  else {
           timestampAgeLimit = GuacamoleProperties.getProperty(TIMESTAMP_AGE_LIMIT);
        }
    }
}
