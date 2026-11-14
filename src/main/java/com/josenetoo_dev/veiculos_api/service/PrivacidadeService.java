package com.josenetoo_dev.veiculos_api.service;
import com.josenetoo_dev.veiculos_api.model.SolicitacaoPrivacidade;
import com.josenetoo_dev.veiculos_api.repository.SolicitacaoPrivacidadeRepository;
import com.josenetoo_dev.veiculos_api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
@Service @RequiredArgsConstructor
public class PrivacidadeService {
 private final CurrentUserService currentUser;
 private final UsuarioRepository usuarios;
 private final SolicitacaoPrivacidadeRepository solicitacoes;
 public record Response(Long id, String protocolo, Long titularId, String tipo, String status, LocalDateTime criadoEm) {
  public static Response from(SolicitacaoPrivacidade item) {
   return new Response(item.getId(), "AM-DADOS-" + item.getId(), item.getTitular().getId(), item.getTipo(), item.getStatus(), item.getCriadoEm());
  }
 }
 @Transactional
 public Response solicitarExclusao() {
  var user = usuarios.findByIdForUpdate(currentUser.current().getId()).orElseThrow();
  var item = solicitacoes.findFirstByTitularIdAndTipoAndStatus(user.getId(), "EXCLUSAO", "SOLICITADA").orElseGet(() -> {
   var request = new SolicitacaoPrivacidade();
   request.setTitular(user);
   return solicitacoes.saveAndFlush(request);
  });
  return Response.from(item);
 }
 @Transactional(readOnly = true)
 public Page<Response> minhas(Pageable pageable) {
  return solicitacoes.findByTitularIdOrderByCriadoEmDesc(currentUser.current().getId(), pageable).map(Response::from);
 }
 @Transactional(readOnly = true)
 public Page<Response> todas(Pageable pageable) {
  currentUser.admin();
  return solicitacoes.findAll(pageable).map(Response::from);
 }
}
