package com.slotcentral.jackpot.controller;

import com.slotcentral.jackpot.dto.*;
import com.slotcentral.jackpot.service.JackpotService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/jackpots")
public class JackpotController {

    private final JackpotService jackpotService;

    public JackpotController(JackpotService jackpotService) {
        this.jackpotService = jackpotService;
    }

    @PostMapping
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<JackpotResponse> create(@Valid @RequestBody CreateJackpotRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(jackpotService.createJackpot(req));
    }

    @GetMapping
    public ResponseEntity<Page<JackpotResponse>> list(
            @RequestParam(required = false) Boolean isActive,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(jackpotService.getAllJackpots(isActive, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JackpotResponse> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(jackpotService.getJackpotById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<JackpotResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateJackpotRequest req) {
        return ResponseEntity.ok(jackpotService.updateJackpot(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        jackpotService.deleteJackpot(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/contribute")
    public ResponseEntity<ContributeResponse> contribute(
            @PathVariable Long id,
            @Valid @RequestBody ContributeRequest req) {
        return ResponseEntity.ok(jackpotService.contribute(id, req));
    }

    @PostMapping("/{id}/win")
    public ResponseEntity<WinResponse> win(
            @PathVariable Long id,
            @Valid @RequestBody WinRequest req) {
        return ResponseEntity.ok(jackpotService.recordWin(id, req));
    }

    @GetMapping("/{id}/wins")
    public ResponseEntity<Page<WinHistoryResponse>> winHistory(
            @PathVariable Long id,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(jackpotService.getWinHistory(id, pageable));
    }

    @PostMapping("/{id}/reset")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<JackpotResponse> adminReset(@PathVariable Long id) {
        return ResponseEntity.ok(jackpotService.adminReset(id));
    }
}
