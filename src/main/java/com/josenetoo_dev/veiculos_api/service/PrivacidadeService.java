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
 public record Response(Long id, String protocolo, Long titularId, String tipo, String status, LocalDateTime criadoEm, String resposta, LocalDateTime respondidoEm) {
  public static Response from(SolicitacaoPrivacidade item) {
   return new Response(item.getId(), "AM-DADOS-" + item.getId(), item.getTitular().getId(), item.getTipo(), item.getStatus(), item.getCriadoEm(), item.getRespostaPublica(), item.getRespondidoEm());
  }
 }
 @Transactional
 public Response solicitarExclusao() {
  var user = usuarios.findByIdForUpdate(currentUser.current().getId()).orElseThrow();
  var item = solicitacoes.findFirstByTitularIdAndTipoAndStatusIn(user.getId(), "EXCLUSAO", java.util.List.of("SOLICITADA", "EM_ANALISE", "AGUARDANDO_TITULAR")).orElseGet(() -> {
   var request = new SolicitacaoPrivacidade();
   request.setTitular(user);
   return solicitacoes.saveAndFlush(request);
  });
  return Response.from(item);
 }
 public record Atendimento(
     @jakarta.validation.constraints.Pattern(regexp = "EM_ANALISE|AGUARDANDO_TITULAR|RESPONDIDA") String status,
     @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(min = 10, max = 2000) String resposta) {}

 @Transactional
 public Response atender(Long id, Atendimento pedido) {
  var admin = currentUser.admin();
  var item = solicitacoes.buscarParaAtendimento(id)
      .orElseThrow(() -> new com.josenetoo_dev.veiculos_api.exception.ex.UsuarioNaoEncontradoException("Solicitação não encontrada"));
  if (pedido.status() == null || !java.util.Set.of("EM_ANALISE", "AGUARDANDO_TITULAR", "RESPONDIDA").contains(pedido.status()))
      throw new IllegalArgumentException("Informe uma etapa de atendimento válida");
  String resposta = pedido.resposta().strip();
  if (resposta.equals(item.getRespostaPublica()) && pedido.status().equals(item.getStatus())) return Response.from(item);
  if ("RESPONDIDA".equals(item.getStatus()))
      throw new IllegalArgumentException("Este atendimento já foi respondido");
  var agora = LocalDateTime.now(java.time.ZoneOffset.UTC);
  String evento = agora + " | responsável " + admin.getId() + " | " + pedido.status() + " | " + resposta.replace("\n", " ") + "\n";
  item.setHistoricoAtendimento(java.util.Objects.requireNonNullElse(item.getHistoricoAtendimento(), "") + evento);
  item.setStatus(pedido.status());
  item.setRespostaPublica(resposta);
  item.setRespondidoEm(agora);
  item.setResponsavelId(admin.getId());
  return Response.from(item);
 }

 @Transactional(readOnly = true)
 public String historico(Long id) {
  currentUser.admin();
  return solicitacoes.findById(id).map(SolicitacaoPrivacidade::getHistoricoAtendimento).orElse("");
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
