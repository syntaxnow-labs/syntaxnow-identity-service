package com.syntaxnow.identity.service;

import com.syntaxnow.identity.config.KeycloakProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakAdminService {

    private final KeycloakProperties keycloakProperties;
    private final RestTemplate restTemplate;

    public String getAdminToken() {

        String url =
                keycloakProperties.getUrl() + keycloakProperties.getEndpoints().getAdminToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", "admin-cli");
        form.add("username", keycloakProperties.getAdminUser());
        form.add("password", keycloakProperties.getAdminPassword());


        HttpEntity<MultiValueMap<String, String>> request =
                new HttpEntity<>(form, headers);

        ResponseEntity<Map> response =
                restTemplate.postForEntity(url, request, Map.class);

        return Objects.requireNonNull(response.getBody())
                .get("access_token")
                .toString();
    }

    public String findUserByPhone(String phone, String adminToken) {

        String username = normalizeUsername(phone);

        String url = keycloakProperties.getUrl() +
                keycloakProperties.getEndpoints().getSearchUsers();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);

        ResponseEntity<List> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        List.class,
                        Map.of(
                                "realm", keycloakProperties.getRealm(),
                                "username", username
                        )
                );

        List<Map<String, Object>> users = Objects.requireNonNull(response.getBody());

        if (users.isEmpty()) {
            return null;
        }

        for (Map<String, Object> user : users) {
            if (user.get("username").toString().equals(username)) {
                return user.get("id").toString();
            }
        }

        return null;
    }

    public String createUser(String phone, String adminToken) {

        String username = normalizeUsername(phone);

        String url = keycloakProperties.getUrl() +
                keycloakProperties.getEndpoints().getAdminUsers();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> payload = new HashMap<>();
        payload.put("username", username);
        payload.put("enabled", true);

        payload.put("attributes", Map.of(
                "phone_number", List.of(phone)
        ));

        ResponseEntity<Void> response =
                restTemplate.postForEntity(
                        url,
                        new HttpEntity<>(payload, headers),
                        Void.class,
                        Map.of(
                                "realm", keycloakProperties.getRealm()
                        )
                );

        String location = response.getHeaders().getFirst("Location");

        if (location == null) {
            throw new IllegalStateException("Keycloak did not return Location header");
        }

        return location.substring(location.lastIndexOf("/") + 1);
    }

    public void setUserPassword(String userId, String password, String adminToken) {

        String url = keycloakProperties.getUrl() +
                keycloakProperties.getEndpoints().getResetPassword();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "type", "password",
                "value", password,
                "temporary", false
        );

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(body, headers);

        restTemplate.put(
                url,
                request,
                Map.of(
                        "realm", keycloakProperties.getRealm(),
                        "userId", userId
                ));
    }

    private String normalizeUsername(String phone) {
        return phone.replace("+", "");
    }
}
