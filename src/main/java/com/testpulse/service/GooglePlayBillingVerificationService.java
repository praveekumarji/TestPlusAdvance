package com.testpulse.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.testpulse.dto.PaymentRecordRequest;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GooglePlayBillingVerificationService {

    private static final String TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";
    private static final String PUBLISHER_SCOPE = "https://www.googleapis.com/auth/androidpublisher";
    private static final String PUBLISHER_BASE = "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String packageName;
    private final String serviceAccountEmail;
    private final String privateKeyPem;

    public GooglePlayBillingVerificationService(
            ObjectMapper objectMapper,
            @Value("${google.play.package-name:}") String packageName,
            @Value("${google.play.service-account-email:}") String serviceAccountEmail,
            @Value("${google.play.service-account-private-key:}") String privateKeyPem) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
        this.packageName = packageName;
        this.serviceAccountEmail = serviceAccountEmail;
        this.privateKeyPem = privateKeyPem;
    }

    public VerificationResult verifyAndAcknowledge(PaymentRecordRequest request) {
        if (packageName.isBlank() || serviceAccountEmail.isBlank() || privateKeyPem.isBlank()) {
            throw new IllegalStateException("Google Play verification is not configured.");
        }

        String purchaseToken = required(request.getGooglePurchaseToken(), "Google Play purchase token");
        String productId = request.getGoogleProductId();
        if (productId == null || productId.isBlank()) {
            productId = request.getPlanCode();
        }
        if (productId == null || productId.isBlank()) {
            productId = request.getPlanId();
        }
        productId = required(productId, "Google Play product ID");

        try {
            String accessToken = createAccessToken();
            String productPath = PUBLISHER_BASE + encode(packageName) + "/purchases/products/"
                    + encode(productId) + "/tokens/" + encode(purchaseToken);
            HttpResponse<String> purchaseResponse = send("GET", productPath, accessToken, null);
            if (purchaseResponse.statusCode() != 200) {
                throw new IllegalArgumentException("Google Play could not verify this purchase.");
            }

            JsonNode purchase = objectMapper.readTree(purchaseResponse.body());
            if (purchase.path("purchaseState").asInt(-1) != 0) {
                throw new IllegalArgumentException("Google Play purchase is not completed.");
            }
            if (purchase.hasNonNull("productId") && !productId.equals(purchase.get("productId").asText())) {
                throw new IllegalArgumentException("Google Play product does not match the selected plan.");
            }

            String orderId = purchase.path("orderId").asText(null);
            acknowledge(productId, purchaseToken, accessToken);
            return new VerificationResult(productId, orderId);
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Google Play verification failed.", ex);
        }
    }

    private void acknowledge(String productId, String purchaseToken, String accessToken) throws Exception {
        String acknowledgePath = PUBLISHER_BASE + encode(packageName) + "/purchases/products/"
                + encode(productId) + "/tokens/" + encode(purchaseToken) + ":acknowledge";
        HttpResponse<String> response = send("POST", acknowledgePath, accessToken, "{}");
        if (response.statusCode() != 200 && response.statusCode() != 204) {
            throw new IllegalStateException("Google Play purchase acknowledgement failed.");
        }
    }

    private String createAccessToken() throws Exception {
        long issuedAt = Instant.now().getEpochSecond();
        String header = base64Url("{\"alg\":\"RS256\",\"typ\":\"JWT\"}");
        String claims = base64Url(objectMapper.writeValueAsString(java.util.Map.of(
                "iss", serviceAccountEmail,
                "scope", PUBLISHER_SCOPE,
                "aud", TOKEN_ENDPOINT,
                "iat", issuedAt,
                "exp", issuedAt + 3600
        )));
        String unsignedToken = header + "." + claims;

        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(readPrivateKey());
        signature.update(unsignedToken.getBytes(StandardCharsets.UTF_8));
        String assertion = unsignedToken + "." + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(signature.sign());

        String body = "grant_type=" + encode("urn:ietf:params:oauth:grant-type:jwt-bearer")
                + "&assertion=" + encode(assertion);
        HttpResponse<String> response = sendForm(TOKEN_ENDPOINT, body);
        if (response.statusCode() != 200) {
            throw new IllegalStateException("Could not obtain Google Play access token.");
        }
        return objectMapper.readTree(response.body()).path("access_token").asText();
    }

    private PrivateKey readPrivateKey() throws Exception {
        String normalized = privateKeyPem.replace("\\n", "\n")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        return KeyFactory.getInstance("RSA").generatePrivate(
                new PKCS8EncodedKeySpec(Base64.getDecoder().decode(normalized)));
    }

    private HttpResponse<String> send(String method, String url, String accessToken, String body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/json");
        if ("POST".equals(method)) {
            builder.header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body == null ? "" : body));
        } else {
            builder.GET();
        }
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> sendForm(String url, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required.");
        }
        return value.trim();
    }

    private String base64Url(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public record VerificationResult(String productId, String orderId) {
    }
}
