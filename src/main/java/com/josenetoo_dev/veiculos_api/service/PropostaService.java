package com.josenetoo_dev.veiculos_api.service;

import com.josenetoo_dev.veiculos_api.dto.proposta_dto.ContrapropostaRequest;
import com.josenetoo_dev.veiculos_api.dto.proposta_dto.PropostaRequest;
import com.josenetoo_dev.veiculos_api.dto.proposta_dto.PropostaResponse;
import com.josenetoo_dev.veiculos_api.enums.StatusAnuncio;
import com.josenetoo_dev.veiculos_api.enums.StatusProposta;
import com.josenetoo_dev.veiculos_api.exception.ex.*;
import com.josenetoo_dev.veiculos_api.exception.ex.ContrapropostaJaRealizadaException;
import com.josenetoo_dev.veiculos_api.model.Anuncio;
import com.josenetoo_dev.veiculos_api.model.Proposta;
import com.josenetoo_dev.veiculos_api.model.Usuario;
import com.josenetoo_dev.veiculos_api.repository.AnuncioRepository;
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
public class PropostaService {
    private final PropostaRepository propostaRepository;
    private final AnuncioRepository anuncioRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional
    public PropostaResponse mandarProposta(PropostaRequest request) {
        Proposta proposta = new Proposta();

        Anuncio anuncio = anuncioRepository.findById(request.getAnuncioId())
                .orElseThrow(() -> new AnuncioNaoEncontradoException("Anuncio não encontrado"));

        Usuario usuario = obterUsuarioAutenticado();

        if (anuncio.getUsuario().getId().equals(usuario.getId())) {
            throw new AcessoNegadoException("Você não pode fazer uma proposta no seu próprio anúncio");
        }

        if (anuncio.getStatus() != StatusAnuncio.ATIVO) {
            throw new AnuncioIndisponivelException("Este anúncio não está disponível para receber propostas");
        }

        proposta.setStatus(StatusProposta.PENDENTE);
        proposta.setValor(request.getValor());
        proposta.setDescricao(request.getDescricao());
        proposta.setAnuncio(anuncio);
        proposta.setComprador(usuario);

        Proposta propostaSalva = propostaRepository.save(proposta);

        return new PropostaResponse(propostaSalva);
    }

    @Transactional(readOnly = true)
    public Page<PropostaResponse> minhasPropostas(Pageable pageable) {
        Usuario usuario = obterUsuarioAutenticado();

        return propostaRepository.findByCompradorIdOrAnuncioUsuarioId(usuario.getId(), usuario.getId(), pageable)
                .map(PropostaResponse::new);
    }

    @Transactional(readOnly = true)
    public Proposta buscarPorId(Long id) {
        Proposta proposta = buscarProposta(id);
        Usuario usuario = obterUsuarioAutenticado();

        exigirParticipanteDaNegociacao(proposta, usuario);
        return proposta;
    }

    @Transactional
    public PropostaResponse cancelarProposta(Long id) {
        Proposta proposta = buscarProposta(id);
        Usuario usuario = obterUsuarioAutenticado();

        exigirComprador(proposta, usuario);

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

    // Quem decide (aceitar/negar) depende do estado: proposta original -> dono do anúncio decide;
    // depois que o dono contrapropõe, quem decide passa a ser o comprador.
    @Transactional
    public PropostaResponse aceitarProposta(Long id) {
        Proposta proposta = buscarProposta(id);
        Usuario usuario = obterUsuarioAutenticado();

        if (proposta.getStatus() == StatusProposta.ACEITA) {
            throw new PropostaJaAceitaException("Proposta já está aceita");
        }

        if (proposta.getStatus() == StatusProposta.CANCELADA) {
            throw new PropostaJaCanceladaException("Proposta já está cancelada");
        }

        if (proposta.getStatus() == StatusProposta.NEGADA) {
            throw new PropostaJaNegadaException("Proposta já está negada");
        }

        if (proposta.getStatus() == StatusProposta.CONTRAPROPOSTA) {
            exigirComprador(proposta, usuario);
            // Contraproposta aceita vira o valor/descrição definitivos da proposta
            proposta.setValor(proposta.getContrapropostaValor());
            proposta.setDescricao(proposta.getContrapropostaDescricao());
        } else {
            exigirProprietarioDoAnuncio(proposta, usuario);
        }

        proposta.setStatus(StatusProposta.ACEITA);

        Proposta propostaSalva = propostaRepository.save(proposta);

        return new PropostaResponse(propostaSalva);
    }

    @Transactional
    public PropostaResponse negarProposta(Long id) {
        Proposta proposta = buscarProposta(id);
        Usuario usuario = obterUsuarioAutenticado();

        if (proposta.getStatus() == StatusProposta.NEGADA) {
            throw new PropostaJaNegadaException("Proposta já está negada");
        }

        if (proposta.getStatus() == StatusProposta.CANCELADA || proposta.getStatus() == StatusProposta.ACEITA) {
            throw new NaoPodeCancelarAndNegarPropostaException("Não é possível negar uma proposta já finalizada");
        }

        if (proposta.getStatus() == StatusProposta.CONTRAPROPOSTA) {
            exigirComprador(proposta, usuario);
        } else {
            exigirProprietarioDoAnuncio(proposta, usuario);
        }

        proposta.setStatus(StatusProposta.NEGADA);

        Proposta propostaSalva = propostaRepository.save(proposta);

        return new PropostaResponse(propostaSalva);
    }

    @Transactional
    public PropostaResponse fazerContraproposta(Long id, ContrapropostaRequest request) {
        Proposta proposta = buscarProposta(id);
        Usuario usuario = obterUsuarioAutenticado();

        // Só o dono do anúncio pode fazer contraproposta — depois é o comprador quem aceita/nega
        exigirProprietarioDoAnuncio(proposta, usuario);

        // Só é permitida uma contraproposta por proposta
        if (proposta.getContrapropostaFeita()) {
            throw new ContrapropostaJaRealizadaException("Esta proposta já possui uma contraproposta");
        }

        // Só pode fazer contraproposta em proposta PENDENTE
        if (proposta.getStatus() != StatusProposta.PENDENTE) {
            throw new NaoPodeCancelarAndNegarPropostaException("Só é possível fazer contraproposta em propostas pendentes");
        }

        proposta.setContrapropostaValor(request.getValor());
        proposta.setContrapropostaDescricao(request.getDescricao());
        proposta.setContrapropostaFeita(true);
        proposta.setStatus(StatusProposta.CONTRAPROPOSTA);

        return new PropostaResponse(propostaRepository.save(proposta));
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
        if (!ehComprador(proposta, usuario) && !ehProprietarioDoAnuncio(proposta, usuario)) {
            throw new AcessoNegadoException("Você não tem permissão para visualizar esta proposta");
        }
    }

    private void exigirComprador(Proposta proposta, Usuario usuario) {
        if (!ehComprador(proposta, usuario)) {
            throw new AcessoNegadoException("Somente o comprador pode realizar esta ação nesta proposta");
        }
    }

    private void exigirProprietarioDoAnuncio(Proposta proposta, Usuario usuario) {
        if (!ehProprietarioDoAnuncio(proposta, usuario)) {
            throw new AcessoNegadoException("Somente o proprietário do anúncio pode alterar esta proposta");
        }
    }

    private boolean ehComprador(Proposta proposta, Usuario usuario) {
        return proposta.getComprador().getId().equals(usuario.getId());
    }

    private boolean ehProprietarioDoAnuncio(Proposta proposta, Usuario usuario) {
        return proposta.getAnuncio().getUsuario().getId().equals(usuario.getId());
    }
}