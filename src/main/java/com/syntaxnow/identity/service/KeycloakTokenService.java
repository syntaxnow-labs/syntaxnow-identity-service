package com.syntaxnow.identity.service;

import com.syntaxnow.identity.config.KeycloakProperties;
import com.syntaxnow.identity.dto.TokenExchangeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class KeycloakTokenService {

    private final KeycloakAdminService keycloakAdmin;
    private final KeycloakProperties keycloakProperties;
    private final RestTemplate restTemplate;

    public Map<String, Object> loginUser(String phone, String password) {
        String url = keycloakProperties.getUrl() +
                keycloakProperties.getEndpoints().getRealmToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", keycloakProperties.getClientId()); // Public client
        form.add("client_secret", keycloakProperties.getClientSecret()); // Add client secret for authentication
        form.add("grant_type", "password");
        form.add("username", phone.replace("+", ""));
        form.add("password", password);
        form.add("scope", "openid profile email phone");

        HttpEntity<MultiValueMap<String, String>> request =
                new HttpEntity<>(form, headers);
        ResponseEntity<Map> response =
                restTemplate.postForEntity(
                        url,
                        request,
                        Map.class,
                        Map.of("realm", keycloakProperties.getRealm())
                );

        return response.getBody();
    }

    public Map<String, Object> issueForUser(String userId) {

        // Step 1: Get admin token
        String adminToken = keycloakAdmin.getAdminToken();

        // Step 2: Impersonate user (creates user session)
        String impersonateUrl = keycloakProperties.getUrl() +
                keycloakProperties.getEndpoints().getImpersonate();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);

        restTemplate.postForEntity(
                impersonateUrl,
                new HttpEntity<>(headers),
                Void.class,
                Map.of(
                        "realm", keycloakProperties.getRealm(),
                        "userId", userId
                )
        );

        // Step 3: Exchange session → real tokens
        String tokenUrl = keycloakProperties.getUrl() +
                keycloakProperties.getEndpoints().getRealmToken();

        HttpHeaders tokenHeaders = new HttpHeaders();
        tokenHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", keycloakProperties.getClientId());
        form.add("client_secret", keycloakProperties.getClientSecret());
        form.add("scope", "openid profile email phone");

        ResponseEntity<Map> tokenResponse =
                restTemplate.postForEntity(
                        tokenUrl,
                        new HttpEntity<>(form, tokenHeaders),
                        Map.class,
                        Map.of("realm", keycloakProperties.getRealm())
                );

        return tokenResponse.getBody();
    }

    public TokenExchangeResponse exchangeAuthorizationCode(String code) {

        String url = keycloakProperties.getUrl() +
                keycloakProperties.getEndpoints().getRealmToken();

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", "syntaxnow-frontend");
        form.add("code", code);
        form.add("redirect_uri", "http://localhost:5173/google/callback");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        ResponseEntity<Map> response =
                restTemplate.postForEntity(
                        url,
                        new HttpEntity<>(form, headers),
                        Map.class,
                        Map.of("realm", keycloakProperties.getRealm())
                );

        Map<String, Object> token = response.getBody();

        return new TokenExchangeResponse(
                null,
                null,
                token.get("access_token").toString(),
                token.get("refresh_token").toString(),
                (int) token.get("expires_in")
        );
    }

    public TokenExchangeResponse refresh(String refreshToken) {
        String url = keycloakProperties.getUrl() +
                keycloakProperties.getEndpoints().getRealmToken();


        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("client_id", keycloakProperties.getClientId());
        form.add("client_secret", keycloakProperties.getClientSecret());
        form.add("refresh_token", refreshToken);

        ResponseEntity<Map> response =
                restTemplate.postForEntity(
                        url,
                        new HttpEntity<>(form, headers),
                        Map.class,
                        Map.of("realm", keycloakProperties.getRealm())
                );

        Map<String, Object> token = response.getBody();

        return new TokenExchangeResponse(
                null,
                null,
                token.get("access_token").toString(),
                token.get("refresh_token").toString(),
                (int) token.get("expires_in")
        );
    }

    public void logout(String refreshToken) {
        String url = keycloakProperties.getUrl() +
                keycloakProperties.getEndpoints().getLogout();

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", keycloakProperties.getClientId());
        form.add("client_secret", keycloakProperties.getClientSecret());
        form.add("refresh_token", refreshToken);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        restTemplate.postForEntity(
                url,
                new HttpEntity<>(form, headers),
                Void.class,
                Map.of("realm", keycloakProperties.getRealm())
        );
    }
}
