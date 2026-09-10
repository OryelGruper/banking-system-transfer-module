package com.bankingsystem.transfer.web;

import com.bankingsystem.transfer.dto.StandingOrderRequest;
import com.bankingsystem.transfer.dto.StandingOrderResponse;
import com.bankingsystem.transfer.service.StandingOrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/standing-orders")
public class StandingOrderController {

    private final StandingOrderService standingOrderService;

    public StandingOrderController(StandingOrderService standingOrderService) {
        this.standingOrderService = standingOrderService;
    }

    @PostMapping
    public ResponseEntity<StandingOrderResponse> create(@Valid @RequestBody StandingOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(standingOrderService.create(request));
    }

    @GetMapping
    public List<StandingOrderResponse> listActive() {
        return standingOrderService.listActive();
    }

    @GetMapping("/{id}")
    public StandingOrderResponse getById(@PathVariable String id) {
        return standingOrderService.getById(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(@PathVariable String id) {
        standingOrderService.cancel(id);
        return ResponseEntity.noContent().build();
    }
}
