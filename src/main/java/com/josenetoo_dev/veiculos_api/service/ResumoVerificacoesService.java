package com.josenetoo_dev.veiculos_api.service;

import com.josenetoo_dev.veiculos_api.dto.verificacao.ResumoVerificacoesResponse;
import com.josenetoo_dev.veiculos_api.dto.verificacao.ResumoVerificacoesResponse.AnaliseDocumental;
import com.josenetoo_dev.veiculos_api.enums.StatusVerificacao;
import com.josenetoo_dev.veiculos_api.model.Verificacao;
import com.josenetoo_dev.veiculos_api.repository.*;
import com.josenetoo_dev.veiculos_api.exception.ex.AnuncioNaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ResumoVerificacoesService {
    private final AnuncioService anuncioService;
    private final AnuncioRepository anuncios;
    private final VerificacaoRepository verificacoes;

    @Transactional(readOnly = true)
    public ResumoVerificacoesResponse consultar(Long anuncioId) {
        anuncioService.exigirVisibilidadePorId(anuncioId);
        var anuncio = anuncios.findById(anuncioId).orElseThrow(() -> new AnuncioNaoEncontradoException("Anúncio não encontrado"));
        var identidade = verificacoes.findByUsuarioIdentidadeId(anuncio.getUsuario().getId());
        var veiculo = anuncio.getVeiculo() == null ? Optional.<Verificacao>empty()
                : verificacoes.findByVeiculoId(anuncio.getVeiculo().getId());
        return new ResumoVerificacoesResponse(resumo(identidade), resumo(veiculo),
                "Análise documental manual não comprova consultas oficiais, inspeção, posse atual ou ausência de fraude. Não equivale aos quatro níveis completos de confiança.");
    }

    private AnaliseDocumental resumo(Optional<Verificacao> verificacao) {
        return verificacao.filter(v -> v.getStatus() == StatusVerificacao.APROVADA && v.getRevisadoEm() != null)
                .map(v -> new AnaliseDocumental(true, v.getRevisadoEm()))
                .orElseGet(() -> new AnaliseDocumental(false, null));
    }
}
