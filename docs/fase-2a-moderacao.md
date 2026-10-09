# Auto Minas — Fase 2A: moderação obrigatória de anúncios

> Implementação incremental sobre a branch `security/fase-1`. Não inclui separação de `Vehicle`/`Listing`, cadastro documental, verificações oficiais ou deploy.

## Regra de publicação
1. Vendedor autenticado cadastra anúncio com `POST /v1/anuncio`. O status inicial passa a ser **PENDENTE**, nunca ATIVO.
2. Vendedor pode editar enquanto o anúncio está disponível para edição. **Qualquer edição** altera o status para PENDENTE e limpa a aprovação anterior.
3. `ADMIN` ou `REVIEWER` consulta a fila e decide; um revisor não pode revisar seu próprio anúncio.
4. Aprovação exige **conta ACTIVE do vendedor**, mas isso não representa identidade ou propriedade documental verificada.
5. Somente anúncios **ATIVO** aparecem no feed, destaques e buscas públicas. Rejeitados e pendentes são visíveis somente ao titular e à equipe autorizada.
6. Rejeições requerem motivo (até 500 caracteres), visível ao vendedor. Após editar, o anúncio volta à fila.

## Endpoints
- `GET /v1/moderacao/anuncios` — página de anúncios PENDENTE; ADMIN/REVIEWER.
- `GET /v1/moderacao/anuncios/{id}` — análise de anúncio; ADMIN/REVIEWER.
- `POST /v1/moderacao/anuncios/{id}/aprovar` — pendente → ativo; ADMIN/REVIEWER.
- `POST /v1/moderacao/anuncios/{id}/rejeitar` — body `{"motivo":"texto"}`; pendente → rejeitado; ADMIN/REVIEWER.
- `GET /v1/anuncio/meus` — vendedor acompanha seus anúncios e motivo de rejeição.

As ações administrativas retornam 401 sem login, 403 sem privilégios ou para revisão do próprio anúncio e 409 para transição fora do estado PENDENTE.

## Auditoria
- `anuncio.revisado_em`, `anuncio.revisado_por_id`, `anuncio.motivo_rejeicao`: última decisão.
- `moderacao_evento`: histórico de decisões com ID do anúncio e do revisor, transição, motivo e horário UTC; sem chaves estrangeiras para preservar histórico mesmo após exclusão legada.
- Auditoria não equivale a certificação oficial de CPF, veículo ou propriedade.

## Banco: V4
`V4__listing_moderation.sql` adiciona campos e histórico. **Também converte anúncios legados ATIVO em PENDENTE** — isso oculta os anúncios antigos até revisão humana. Dados, relacionamentos e fotos são preservados, mas a visibilidade muda.

Antes de produção:
1. Restaurar backup de MySQL em ambiente de ensaio.
2. Auditar schema real antes de baseline Flyway V1.
3. Ensaiar V2, V3 e V4; validar `ddl-auto=validate`.
4. Auditar migração de status e capacidade da equipe de revisar o inventário antigo.
5. Verificar reversibilidade operacional e comportamento do cliente frontend.
6. Solicitar autorização específica para migração/deploy, jamais executar automaticamente por conta deste PR.

## Testes e limites
- Testes de regressão adicionados para criação não pública, aprovação por perfil, bloqueio de revisão própria, rejeição/motivo, edição e nova revisão, proibição de bypass e auditoria.
- MySQL dedicado ensaia V4 em dados legados simulados e comprova retenção dos registros.
- A execução final é verificada na CI da branch. Não interpretar CI verde como teste no banco verdadeiro de produção.

### Fora de escopo
- Verificações de pessoa e veículo, integração oficial, moderação de documentos, denúncia, antiabuso geral e storage de mídia privado.
- A URL direta de imagem já conhecida continua acessível no modelo de storage existente.
- A migração definitiva que separará entidade `Veiculo` de `Anuncio` será tratada na próxima entrega incremental, preservando dados e contratos.
