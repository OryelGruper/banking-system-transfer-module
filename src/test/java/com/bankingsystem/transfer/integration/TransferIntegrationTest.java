package com.bankingsystem.transfer.integration;

import tools.jackson.databind.json.JsonMapper;
import com.bankingsystem.transfer.dto.TransferRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end test exercising the full transfer flow -- controller, service,
 * database (real H2 + Liquibase schema/seed) -- via MockMvc, as required by
 * the spec.
 */
@SpringBootTest
@AutoConfigureMockMvc
// Without this, @SpringBootTest swaps in a fresh, randomly-named
// embedded H2 instance for the test -- separate from the one Liquibase
// just seeded at startup -- so every table comes back "not found".
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = "app.security.api-key=test-api-key")
class TransferIntegrationTest {

    private static final String API_KEY_HEADER = "X-FIB-AUTH";
    private static final String API_KEY = "test-api-key";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    void endToEndTransfer_updatesBalancesAndHonoursIdempotency() throws Exception {
        TransferRequest request = new TransferRequest("BG01FINV001", "BG01FINV003", new BigDecimal("100.00"));
        String idempotencyKey = UUID.randomUUID().toString();

        mockMvc.perform(post("/api/v1/transfers")
                        .header(API_KEY_HEADER, API_KEY)
                        .header("X-Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sourceIban").value("BG01FINV001"))
                .andExpect(jsonPath("$.destinationIban").value("BG01FINV003"))
                .andExpect(jsonPath("$.sourceAmount").value(100.00));

        mockMvc.perform(get("/api/v1/accounts/BG01FINV001").header(API_KEY_HEADER, API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(9900.00));

        mockMvc.perform(get("/api/v1/accounts/BG01FINV003").header(API_KEY_HEADER, API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(2600.00));

        // Replaying the same X-Idempotency-Key must not move money twice.
        mockMvc.perform(post("/api/v1/transfers")
                        .header(API_KEY_HEADER, API_KEY)
                        .header("X-Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sourceIban").value("BG01FINV001"));

        mockMvc.perform(get("/api/v1/accounts/BG01FINV001").header(API_KEY_HEADER, API_KEY))
                .andExpect(jsonPath("$.balance").value(9900.00));
    }

    @Test
    void missingApiKey_isRejected() throws Exception {
        mockMvc.perform(get("/api/v1/accounts"))
                .andExpect(status().isUnauthorized());
    }
}
