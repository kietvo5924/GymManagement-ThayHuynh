package com.gym.service.gymmanagementservice.controllers;

import com.gym.service.gymmanagementservice.dtos.SaleRequestDTO;
import com.gym.service.gymmanagementservice.models.Sale;
import com.gym.service.gymmanagementservice.services.SaleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sales")
public class SaleController {

    private final SaleService saleService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<Sale> createSale(@Valid @RequestBody SaleRequestDTO request) {
        Sale newSale = saleService.createSale(request);
        return new ResponseEntity<>(newSale, HttpStatus.CREATED);
    }
}