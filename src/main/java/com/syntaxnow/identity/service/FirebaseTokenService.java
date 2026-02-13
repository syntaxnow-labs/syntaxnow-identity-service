package com.syntaxnow.identity.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class FirebaseTokenService {

  private static final String EXPECTED_ISSUER =
      "https://securetoken.google.com/sn-otp-auth";

  private static final String EXPECTED_AUDIENCE =
      "sn-otp-auth";

  public FirebaseToken verifyOtpToken(String token) throws Exception {

    // Step 1: Verify signature + expiry
    FirebaseToken decoded =
        FirebaseAuth.getInstance().verifyIdToken(token);

    // Step 2: Issuer validation
    if (!EXPECTED_ISSUER.equals(decoded.getIssuer())) {
      throw new RuntimeException("Invalid issuer");
    }

    // Step 3: Audience validation
    Object aud = decoded.getClaims().get("aud");

    if (aud == null || !EXPECTED_AUDIENCE.equals(aud.toString())) {
      throw new RuntimeException("Invalid audience");
    }

    // Step 4: Provider validation (OTP only)
    Map<String, Object> firebase =
        (Map<String, Object>) decoded.getClaims().get("firebase");

    if (firebase == null) {
      throw new RuntimeException("Missing firebase claim");
    }

    String provider = (String) firebase.get("sign_in_provider");

    if (!"phone".equals(provider)) {
      throw new RuntimeException("Not an OTP login token");
    }

    return decoded;
  }

  public String extractPhone(FirebaseToken decoded) {

    String phone = (String) decoded.getClaims().get("phone_number");

    if (phone == null) {
      throw new RuntimeException("Phone number missing in token");
    }

    return phone;
  }
}
