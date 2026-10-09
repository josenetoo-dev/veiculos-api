# Auto Minas — Fase 3: documentos privados e revisão humana

## Escopo implementado
- `Verificacao` (IDENTIDADE ou VEICULO), com estados RASCUNHO → EM_ANALISE → APROVADA/REJEITADA e possibilidade de corrigir solicitação rejeitada.
- Evidências permitidas nesta etapa: imagem JPEG ou PNG de frente e verso de documento de identidade, ou imagem do documento do veículo. Não há extração automática de CPF/placa e **não existe consulta oficial**.
- Upload só para titular do caso, em estado editável; download só para titular ou operador ADMIN/REVIEWER com conta ACTIVE.
- Revisores não podem aprovar o próprio caso. Apenas casos enviados e com as evidências obrigatórias admitem decisão.
- Toda revisão gera evento de auditoria com revisor, resultado, motivo e horário UTC.
- `ModeracaoService.aprovar` só publica um anúncio se houver duas verificações aprovadas: identidade do anunciante e seu veículo.
- Alterar dados descritivos do veículo invalida a verificação anterior e remove as evidências substituídas após commit.

## Segurança dos arquivos
1. Upload usa `ImageValidator`: JPEG/PNG, tamanho máximo 5 MiB, limites de dimensão e recodificação sem metadados.
2. Evidências são criptografadas com **AES-256-GCM** e IV aleatório, com AAD vinculado ao identificador aleatório do arquivo.
3. Bytes ficam em `VERIFICATION_STORAGE_DIR` (padrão `private-verification-documents`), **fora da árvore pública `upload.dir`**. Não há `ResourceHandler` para essa pasta.
4. Leitura exige autorização e responde com `Cache-Control: no-store, private`, `X-Content-Type-Options: nosniff` e disposição `attachment`.
5. `VERIFICATION_STORAGE_KEY` deve ser Base64 de **32 bytes aleatórios**, configurado exclusivamente no secret manager. Sem chave o armazenamento fica desativado (503) e não recebe documentos.
6. Rollback da transação limpa upload pendente; exclusão confirmada no banco agenda remoção física após commit.
7. O ID do arquivo e o material criptografado não aparecem em responses, logs ou URL pública.

## Endpoints
- `POST /v1/verificacoes/identidade`
- `POST /v1/verificacoes/veiculos/{veiculoId}`
- `GET /v1/verificacoes/minhas`
- `GET /v1/verificacoes/{id}`
- `POST /v1/verificacoes/{id}/evidencias?tipo=IDENTIDADE_FRENTE|IDENTIDADE_VERSO|DOCUMENTO_VEICULO` — multipart `arquivo`.
- `GET /v1/verificacoes/{id}/evidencias`
- `GET /v1/verificacoes/{id}/evidencias/{evidenciaId}/arquivo`
- `DELETE /v1/verificacoes/{id}/evidencias/{evidenciaId}`
- `POST /v1/verificacoes/{id}/enviar`
- Staff: `GET /v1/verificacoes/revisao/pendentes`, `POST /v1/verificacoes/revisao/{id}/aprovar`, `POST /v1/verificacoes/revisao/{id}/rejeitar` (body `{"motivo":"..."}`).

## Banco: V6
A migration `V6__manual_verification_evidence.sql` é aditiva e cria tabelas `verificacao`, `evidencia_verificacao` e `evento_verificacao`; não executa DDL em tabelas antigas nem modifica documentos de usuários já cadastrados. **Não executar sem ensaio com backup restaurado**. A aplicação usa `ddl-auto=validate` fora do perfil local.

## Segurança operacional — bloqueadores para produção
- **LGPD:** definir finalidade, base legal, aviso de privacidade, canal de direitos, acesso por função, retenção e descarte. Não guardar imagens além do necessário.
- **Armazenamento:** diretório persistente privado, permissões de filesystem, backup criptografado, acesso operacional restrito, restauração testada e estratégia de rotação de chave. Perder a chave significa perder acesso aos documentos.
- **Antivírus/antimalware:** apenas imagens são aceitas/recodificadas, mas uma avaliação adicional e testes de fuzzing devem preceder a abertura pública.
- **Revisão humana não certifica propriedade:** a aprovação deve ser exibida ao usuário como `documentação analisada manualmente`, jamais `validado pelo Governo` ou `fraude impossível`.
- **Fraude interna:** segregação de funções, logs de acesso a documentos, política de auditoria e duplo controle para decisões sensíveis são recomendados.
- **Envio de documentos:** habilitar somente quando houver procedimento de privacidade e operadores realmente responsáveis.
- **Proteção de imagens de anúncios:** a URL pública legada `/uploads/fotos/**` permanece separada e não deve armazenar documentos.

## Testes
- Testes de controller/segurança: escopo por dono, bloqueio de acesso externo, armazenamento criptografado, documentos obrigatórios, autoaprovação bloqueada, envio/aprovação, veículo e identidade, invalidação após edição e comportamento sem chave.
- Teste MySQL V6 de criação de tabelas, preservação de dados antigos e restrições.
- Registrar status real dos testes da CI antes de declarar a entrega aprovada.

## Não concluído nesta etapa
- Reconhecimento de documento, OCR confiável, biometria, certificação de placa/CRLV ou integrações oficiais.
- Verificação de contato por e-mail/telefone, onboarding de pessoas não técnicas, painel frontend de revisão e consentimento legal.
- Infra de storage em nuvem e rollout/produção.
