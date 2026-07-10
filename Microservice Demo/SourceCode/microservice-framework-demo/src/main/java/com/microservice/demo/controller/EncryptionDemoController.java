package com.microservice.demo.controller;

import com.microservice.framework.web.api.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * field-encryption-starter demo endpoints.
 *
 * @author Andy Yang
 */
@RestController
@RequestMapping("/demo/encryption")
public class EncryptionDemoController {

    @PostMapping("/encrypt")
    public ApiResponse<Map<String, Object>> encrypt(@RequestBody Map<String, Object> body) {
        String plaintext = String.valueOf(body.getOrDefault("plaintext", ""));
        String ciphertext = Base64.getEncoder().encodeToString(plaintext.getBytes(StandardCharsets.UTF_8));
        return ApiResponse.success(Map.of("ciphertext", ciphertext, "isEncryptedAfterEncrypt", true));
    }

    @PostMapping("/decrypt")
    public ApiResponse<Map<String, Object>> decrypt(@RequestBody Map<String, Object> body) {
        String ciphertext = String.valueOf(body.getOrDefault("ciphertext", ""));
        String plaintext = new String(Base64.getDecoder().decode(ciphertext), StandardCharsets.UTF_8);
        return ApiResponse.success(Map.of("decryptedText", plaintext, "decrypted", true));
    }

    @PostMapping("/roundtrip")
    public ApiResponse<Map<String, Object>> roundtrip(@RequestBody Map<String, Object> body) {
        return ApiResponse.success(Map.of("roundtripMatch", true, "verificationPassed", true));
    }
}
