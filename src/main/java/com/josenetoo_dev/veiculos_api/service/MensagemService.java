package com.josenetoo_dev.veiculos_api.service;

import com.josenetoo_dev.veiculos_api.dto.mensagem_dto.MensagemRequest;
import com.josenetoo_dev.veiculos_api.dto.mensagem_dto.MensagemResponse;
import com.josenetoo_dev.veiculos_api.exception.ex.AcessoNegadoException;
import com.josenetoo_dev.veiculos_api.exception.ex.CredenciaisInvalidasException;
import com.josenetoo_dev.veiculos_api.exception.ex.PropostaNaoEncontradaException;
import com.josenetoo_dev.veiculos_api.model.Mensagem;
import com.josenetoo_dev.veiculos_api.model.Proposta;
import com.josenetoo_dev.veiculos_api.model.Usuario;
import com.josenetoo_dev.veiculos_api.repository.MensagemRepository;
import com.josenetoo_dev.veiculos_api.repository.PropostaRepository;
import com.josenetoo_dev.veiculos_api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MensagemService {
    private final MensagemRepository mensagemRepository;
    private final PropostaRepository propostaRepository;
    private final UsuarioRepository usuarioRepository;

    // Chat vinculado a uma proposta: só o comprador e o dono do anúncio daquela proposta podem participar

    @Transactional
    public MensagemResponse enviarMensagem(Long propostaId, MensagemRequest request) {
        Proposta proposta = buscarProposta(propostaId);
        Usuario usuario = obterUsuarioAutenticado();

        exigirParticipanteDaNegociacao(proposta, usuario);

        Mensagem mensagem = new Mensagem();
        mensagem.setConteudo(request.getConteudo());
        mensagem.setProposta(proposta);
        mensagem.setRemetente(usuario);

        return new MensagemResponse(mensagemRepository.save(mensagem));
    }

    @Transactional(readOnly = true)
    public Page<MensagemResponse> listarMensagens(Long propostaId, Pageable pageable) {
        Proposta proposta = buscarProposta(propostaId);
        Usuario usuario = obterUsuarioAutenticado();

        exigirParticipanteDaNegociacao(proposta, usuario);

        return mensagemRepository.findByPropostaIdOrderByCriadoEmAsc(propostaId, pageable)
                .map(MensagemResponse::new);
    }

    private Proposta buscarProposta(Long id) {
        return propostaRepository.findById(id)
                .orElseThrow(() -> new PropostaNaoEncontradaException("Proposta não encontrada"));
    }

    private Usuario obterUsuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new CredenciaisInvalidasException("Usuário não autenticado");
        }

        Long id;
        try {
            id = Long.valueOf(authentication.getName());
        } catch (NumberFormatException e) {
            throw new CredenciaisInvalidasException("Usuário autenticado não encontrado");
        }

        return usuarioRepository.findById(id)
                .orElseThrow(() -> new CredenciaisInvalidasException("Usuário autenticado não encontrado"));
    }

    private void exigirParticipanteDaNegociacao(Proposta proposta, Usuario usuario) {
        boolean ehComprador = proposta.getComprador().getId().equals(usuario.getId());
        boolean ehDonoDoAnuncio = proposta.getAnuncio().getUsuario().getId().equals(usuario.getId());

        if (!ehComprador && !ehDonoDoAnuncio) {
            throw new AcessoNegadoException("Você não faz parte desta negociação");
        }
    }
}
