package com.josenetoo_dev.veiculos_api.service;

import com.josenetoo_dev.veiculos_api.dto.comercial.DadosVendedorRequest;
import com.josenetoo_dev.veiculos_api.dto.comercial.DadosVendedorResponse;
import com.josenetoo_dev.veiculos_api.exception.ex.DadosPessoaisIndisponiveisException;
import com.josenetoo_dev.veiculos_api.exception.ex.CpfJaCadastradoException;
import com.josenetoo_dev.veiculos_api.model.Usuario;
import com.josenetoo_dev.veiculos_api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PerfilVendedorService {
    private final CurrentUserService currentUser;
    private final UsuarioRepository usuarios;
    private final CpfProtectionService cpfProtection;

    @Transactional(readOnly = true)
    public DadosVendedorResponse meusDados() {
        Usuario user = currentUser.current();
        String masked = user.getCpfFinalDigitos() == null ? null : "***.***.***-" + user.getCpfFinalDigitos();
        String fullName = user.getNomeCompletoVendedorCiphertext() == null ? null
                : cpfProtection.revealSellerName(user.getNomeCompletoVendedorCiphertext());
        return new DadosVendedorResponse(fullName, masked, user.getCpfCiphertext() != null);
    }

    @Transactional
    public DadosVendedorResponse atualizar(DadosVendedorRequest request) {
        Usuario user = currentUser.current();
        if (!cpfProtection.available()) throw new DadosPessoaisIndisponiveisException();
        var protectedCpf = cpfProtection.protect(request.cpf());
        if (usuarios.existsByCpfLookupHash(protectedCpf.lookupHash())
                && !protectedCpf.lookupHash().equals(user.getCpfLookupHash())) {
            throw new CpfJaCadastradoException();
        }
        String cleanedName = request.nomeCompleto().strip().replaceAll("\\s+", " ");
        user.setNomeCompletoVendedorCiphertext(cpfProtection.protectSellerName(cleanedName));
        user.setCpfCiphertext(protectedCpf.ciphertext());
        user.setCpfLookupHash(protectedCpf.lookupHash());
        user.setCpfFinalDigitos(protectedCpf.lastDigits());
        usuarios.saveAndFlush(user);
        return meusDados();
    }
}
