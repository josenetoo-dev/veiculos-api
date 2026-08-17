package com.josenetoo_dev.veiculos_api.service;

import com.josenetoo_dev.veiculos_api.dto.proposta_dto.PropostaRequest;
import com.josenetoo_dev.veiculos_api.dto.proposta_dto.PropostaResponse;
import com.josenetoo_dev.veiculos_api.enums.StatusProposta;
import com.josenetoo_dev.veiculos_api.exception.ex.*;
import com.josenetoo_dev.veiculos_api.model.Anuncio;
import com.josenetoo_dev.veiculos_api.model.Proposta;
import com.josenetoo_dev.veiculos_api.model.Usuario;
import com.josenetoo_dev.veiculos_api.repository.AnuncioRepository;
import com.josenetoo_dev.veiculos_api.repository.PropostaRepository;
import com.josenetoo_dev.veiculos_api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PropostaService {
    private final PropostaRepository propostaRepository;
    private final AnuncioRepository anuncioRepository;
    private final UsuarioRepository usuarioRepository;

    // CRUD + read diversos | Sem o update e delete em propostas

    @Transactional
    public PropostaResponse mandarProposta(PropostaRequest request) {
        Proposta proposta = new Proposta();

        Anuncio anuncio = anuncioRepository.findById(request.getAnuncioId())
                .orElseThrow(() -> new AnuncioNaoEncontradoException("Anuncio não encontrado"));

        Usuario usuario = usuarioRepository.findById(request.getCompradorId())
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuario não encontrado"));

        proposta.setStatus(StatusProposta.PENDENTE);
        proposta.setValor(request.getValor());
        proposta.setDescricao(request.getDescricao());
        proposta.setAnuncio(anuncio);
        proposta.setComprador(usuario);

        Proposta propostaSalva = propostaRepository.save(proposta);

        return new PropostaResponse(propostaSalva);
    }

    @Transactional(readOnly = true)
    public Page<PropostaResponse> listarPropostas(Pageable pageable) {
        return propostaRepository.findAll(pageable)
                .map(PropostaResponse::new);
    }

    @Transactional(readOnly = true)
    public Proposta buscarPorId(Long id) {
        return propostaRepository.findById(id)
                .orElseThrow(() -> new PropostaNaoEncontradaException("Proposta não encontrada"));
    }

    @Transactional
    public PropostaResponse cancelarProposta(Long id) {
        Proposta proposta = propostaRepository.findById(id)
                .orElseThrow(() -> new PropostaNaoEncontradaException("Proposta não encontrada exception"));

        if (proposta.getStatus() == StatusProposta.CANCELADA) {
            throw new PropostaJaCanceladaException("Proposta já está cancelada");
        }

        if (proposta.getStatus() == StatusProposta.ACEITA || proposta.getStatus() == StatusProposta.NEGADA) {
            throw new NaoPodeCancelarAndNegarPropostaException("Não é possível cancelar uma proposta já finalizada");
        }

        proposta.setStatus(StatusProposta.CANCELADA);

        Proposta propostaSalva = propostaRepository.save(proposta);

        return new PropostaResponse(propostaSalva);
    }

    @Transactional
    public PropostaResponse aceitarProposta(Long id) {
        Proposta proposta = propostaRepository.findById(id)
                .orElseThrow(() -> new PropostaNaoEncontradaException("Proposta não encontrada exception"));

        if (proposta.getStatus() == StatusProposta.ACEITA) {
            throw new PropostaJaAceitaException("Proposta já está aceita");
        }

        if (proposta.getStatus() == StatusProposta.CANCELADA) {
            throw new PropostaJaCanceladaException("Proposta já está cancelada");
        }

        if (proposta.getStatus() == StatusProposta.NEGADA) {
            throw new PropostaJaNegadaException("Proposta já está negada");
        }

        proposta.setStatus(StatusProposta.ACEITA);

        Proposta propostaSalva = propostaRepository.save(proposta);

        return new PropostaResponse(propostaSalva);
    }

    @Transactional
    public PropostaResponse negarProposta(Long id) {
        Proposta proposta = propostaRepository.findById(id)
                .orElseThrow(() -> new PropostaNaoEncontradaException("Proposta não encontrada"));

        if (proposta.getStatus() == StatusProposta.NEGADA) {
            throw new PropostaJaNegadaException("Proposta já está negada");
        }

        if (proposta.getStatus() == StatusProposta.CANCELADA || proposta.getStatus() == StatusProposta.ACEITA) {
            throw new NaoPodeCancelarAndNegarPropostaException("Não é possível negar uma proposta já finalizada");
        }

        proposta.setStatus(StatusProposta.NEGADA);

        Proposta propostaSalva = propostaRepository.save(proposta);

        return new PropostaResponse(propostaSalva);
    }
}