package com.bankingsystem.transfer.web;

import com.bankingsystem.transfer.dto.TransferRequest;
import com.bankingsystem.transfer.dto.TransferResponse;
import com.bankingsystem.transfer.exception.InvalidTransferException;
import com.bankingsystem.transfer.service.TransferService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1/transfers")
public class TransferController {

    // Checked by hand, not bean validation, so a bad key always produces
    // this module's error JSON instead of Spring's default response.
    private static final Pattern UUID_PATTERN =
            Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping
    public ResponseEntity<TransferResponse> execute(
            @Valid @RequestBody TransferRequest request,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new InvalidTransferException("X-Idempotency-Key header is required");
        }
        if (!UUID_PATTERN.matcher(idempotencyKey).matches()) {
            throw new InvalidTransferException("X-Idempotency-Key must be a UUID");
        }

        TransferResponse response = transferService.execute(request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public TransferResponse getById(@PathVariable String id) {
        return transferService.getById(id);
    }
}
