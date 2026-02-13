package com.syntaxnow.identity.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "keycloak")
public class KeycloakProperties {

    private String url;
    private String realm;
    private String clientId;
    private String clientSecret;
    private String adminUser;
    private String adminPassword;

    private Endpoints endpoints;

    @Getter
    @Setter
    public static class Endpoints {

        private String adminToken;
        private String realmToken;
        private String logout;
        private String userinfo;

        private String adminUsers;
        private String searchUsers;
        private String resetPassword;
        private String impersonate;
    }
}
