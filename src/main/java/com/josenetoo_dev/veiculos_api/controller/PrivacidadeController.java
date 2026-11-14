package com.josenetoo_dev.veiculos_api.controller;
import com.josenetoo_dev.veiculos_api.service.PrivacidadeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
@RestController @RequestMapping("/v1/privacidade/solicitacoes") @RequiredArgsConstructor
public class PrivacidadeController {
 private final PrivacidadeService service;
 @PostMapping public ResponseEntity<PrivacidadeService.Response> solicitar() {
  return ResponseEntity.accepted().body(service.solicitarExclusao());
 }
 @GetMapping public Page<PrivacidadeService.Response> minhas(@PageableDefault(size = 20) Pageable pageable) {
  return service.minhas(pageable);
 }
 @GetMapping("/admin") @PreAuthorize("hasRole('ADMIN')")
 public Page<PrivacidadeService.Response> todas(@PageableDefault(size = 50, sort = "criadoEm", direction = Sort.Direction.DESC) Pageable pageable) {
  return service.todas(pageable);
 }
}
