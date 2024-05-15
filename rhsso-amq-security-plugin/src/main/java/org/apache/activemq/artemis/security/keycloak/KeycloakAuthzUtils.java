package org.apache.activemq.artemis.security.keycloak;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.keycloak.authorization.client.AuthorizationDeniedException;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;
import org.keycloak.authorization.client.resource.AuthorizationResource;
import org.keycloak.authorization.client.resource.ProtectionResource;
import org.keycloak.representations.idm.authorization.AuthorizationRequest;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;

import java.io.IOException;
import javax.net.ssl.*;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;

public class KeycloakAuthzUtils {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(KeycloakAuthzUtils.class);

    private static Configuration configuration;

    private final AuthzClient authzClient;
    
    public KeycloakAuthzUtils() throws IOException {
        if(configuration == null){
            LOGGER.info("Configuration is null....init");
            configuration = KeycloakAuthzUtils.initConfiguration();
        }
        this.authzClient = AuthzClient.create(configuration);
        LOGGER.info("RHSSO AuthzClient correctly initialized");
    }

    private static Configuration initConfiguration() throws IOException {
        final Properties pluginProperties = new Properties();
        try (FileInputStream fis = new FileInputStream("/amq/extra/configmaps/amq-sso-plugin-config/amq-sso-plugin-config.properties")) {
            pluginProperties.load(fis);
            LOGGER.info("trust.self.signed.certificates: {}", pluginProperties.getProperty("trust.self.signed.certificates"));
        } catch (IOException ex) {
            LOGGER.info("No /amq/extra/configmaps/amq-sso-plugin-config/amq-sso-plugin-config.properties found");
        }
        LOGGER.info("Searching for Configuration for Red Hat SSO integration in folder {}", System.getProperty("artemis.instance.etc"));
        final ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return Files.list(Paths.get(System.getProperty("artemis.instance.etc"))) //
            .filter(name -> name.toString().endsWith(".json")) //
            .map(Path::toFile) //
            .map(file -> {
                LOGGER.info("Parsing file {}", file.getName());
                try {
                    return mapper.readValue(file, Configuration.class);
                } catch (JsonParseException e) {
                    e.printStackTrace();
                } catch (JsonMappingException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }).filter(keycloakConfiguration -> keycloakConfiguration != null) //
            .filter(keycloakConfiguration -> keycloakConfiguration.getCredentials().size() > 0) //
            .findFirst() //
            .map(configuration -> {
                LOGGER.info("Configuration authServerUrl: {}", configuration.getAuthServerUrl());
                LOGGER.info("Configuration realm: {}", configuration.getRealm());
                LOGGER.info("Configuration resource: {}", configuration.getResource());
                LOGGER.info("Configuration credentials: {}", configuration.getCredentials());
                if("true".equals(pluginProperties.getOrDefault("trust.self.signed.certificates", "false"))) {
                    return new Configuration(configuration.getAuthServerUrl(),
                            configuration.getRealm(),
                            configuration.getResource(),
                            configuration.getCredentials(),
                            getHttpClient(configuration.getAuthServerUrl()));
                }
                return configuration;
            }) //
            .get();
    }

    public static CloseableHttpClient getHttpClient(String url) {
        final TrustStrategy acceptingTrustStrategy = (cert, authType) -> true;
        SSLContext sslContext;
        try {
            sslContext = SSLContexts.custom()
                .loadTrustMaterial(null, acceptingTrustStrategy)
                .build();
                final SSLConnectionSocketFactory sslsf = 
                new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);
            final Registry<ConnectionSocketFactory> socketFactoryRegistry = 
                RegistryBuilder.<ConnectionSocketFactory> create()
                .register("https", sslsf)
                .register("http", new PlainConnectionSocketFactory())
                .build();
            final BasicHttpClientConnectionManager connectionManager =
                new BasicHttpClientConnectionManager(socketFactoryRegistry);
        
            return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .build();
        } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String[] getResources() throws IOException {
        final ProtectionResource protection = authzClient.protection();
        return protection.resource().findAll();
    }

    public ResourceRepresentation findResourceById(final String resourceId) {
        final ProtectionResource protection = authzClient.protection();
        return protection.resource().findById(resourceId);
    }

    public Boolean authorize(String authzToken, String resourceName, String scope) {
        LOGGER.info("Check permission for Resource: {} with scope: {}", resourceName, scope);
        try {
            final AuthorizationResource authResource = authzClient.authorization(authzToken);
            final AuthorizationRequest authRequest = new AuthorizationRequest();
            authRequest.addPermission(resourceName, scope);
            authResource.authorize(authRequest).getToken();
            return Boolean.TRUE;
        } catch(AuthorizationDeniedException e){
            LOGGER.error(e.getMessage(), e);
        }
        return Boolean.FALSE;
    }

}
