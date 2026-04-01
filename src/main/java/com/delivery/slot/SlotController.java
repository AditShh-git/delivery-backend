package com.delivery.slot;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/slots")
@RequiredArgsConstructor
@Tag(name = "Delivery Slots", description = "Endpoints for delivery slot capacity management")
public class SlotController {

        private final SlotService slotService;

        @PreAuthorize("hasAnyRole('ADMIN','COMPANY')")
        @PostMapping
        @Operation(summary = "Create Delivery Slot", description = "Admin or Company creates a new delivery capacity slot")
        public ResponseEntity<SlotCapacity> createSlot(
                        @Valid @RequestBody CreateSlotRequest request) {

                log.info("POST /api/slots — companyId={}, zone={}, date={}, label={}",
                                request.companyId(), request.zone(), request.slotDate(), request.slotLabel());

                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(slotService.createSlot(request));
        }

        @PreAuthorize("hasAnyRole('ADMIN','COMPANY')")
        @GetMapping
        @Operation(summary = "Get Delivery Slots", description = "Retrieve list of slots for a company and date")
        public ResponseEntity<List<SlotCapacity>> getSlots(
                        @RequestParam Long companyId,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

                log.info("GET /api/slots — companyId={}, date={}", companyId, date);
                return ResponseEntity.ok(slotService.getSlots(companyId, date));
        }
}
