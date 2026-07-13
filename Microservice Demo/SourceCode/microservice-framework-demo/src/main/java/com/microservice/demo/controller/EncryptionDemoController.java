package com.microservice.demo.controller;

import com.microservice.framework.fieldencryption.api.FieldEncryptor;
import com.microservice.framework.web.api.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * field-encryption-starter demo endpoints.
 *
 * @author Andy Yang
 */
@RestController
@RequestMapping("/demo/encryption")
public class EncryptionDemoController {

    private final FieldEncryptor fieldEncryptor;

    public EncryptionDemoController(FieldEncryptor fieldEncryptor) {
        this.fieldEncryptor = fieldEncryptor;
    }

    @PostMapping("/encrypt")
    public ApiResponse<Map<String, Object>> encrypt(@RequestBody Map<String, Object> body) {
        String plaintext = String.valueOf(body.getOrDefault("plaintext", ""));
        String ciphertext = fieldEncryptor.encrypt(plaintext);
        return ApiResponse.success(Map.of("ciphertext", ciphertext,
                "isEncryptedAfterEncrypt", fieldEncryptor.isEncrypted(ciphertext)));
    }

    @PostMapping("/decrypt")
    public ApiResponse<Map<String, Object>> decrypt(@RequestBody Map<String, Object> body) {
        String ciphertext = String.valueOf(body.getOrDefault("ciphertext", ""));
        String plaintext = fieldEncryptor.decrypt(ciphertext);
        return ApiResponse.success(Map.of("decryptedText", plaintext, "decrypted", true));
    }

    @PostMapping("/roundtrip")
    public ApiResponse<Map<String, Object>> roundtrip(@RequestBody Map<String, Object> body) {
        String plaintext = String.valueOf(body.getOrDefault("plaintext", ""));
        String ciphertext = fieldEncryptor.encrypt(plaintext);
        String decrypted = fieldEncryptor.decrypt(ciphertext);
        return ApiResponse.success(Map.of("ciphertext", ciphertext,
                "roundtripMatch", plaintext.equals(decrypted),
                "isEncryptedAfterEncrypt", fieldEncryptor.isEncrypted(ciphertext),
                "verificationPassed", plaintext.equals(decrypted) && fieldEncryptor.isEncrypted(ciphertext)));
    }
}
