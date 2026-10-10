# Auto Minas — Fase 7: cidade/UF e operações de status do vendedor

## Problema que corrige

O frontend exibia **"Localização não informada"** porque a API não possuía cidade/UF no modelo de anúncio. Também havia botões de ações de vendedor bloqueados por falta de endpoints para pausar, solicitar reativação ou marcar um anúncio como vendido.

## Banco e API

- Migration **V12** aditiva: `anuncio.cidade VARCHAR(100) NULL`, `anuncio.uf VARCHAR(2) NULL`, `anuncio.status_atualizado_em DATETIME(6) NULL`, `anuncio.status_atualizado_por_id BIGINT NULL`, e índice por UF/cidade.
- Cidade e UF são **dados informados pelo vendedor sobre o veículo**, não comprovação de residência. Não são geolocalização nem validação oficial.
- Campos nullable permitem atualizar bancos que já tenham anúncios, propostas e fotos sem inventar localização para dados legados.
- DTO `AnuncioRequest` valida tamanho de cidade, UF brasileira válida e presença dos dois campos juntos (ambos ausentes são aceitos para compatibilidade com clientes legados). O frontend novo exige ambos.
- `AnuncioResponse` sempre informa `cidade`/`uf` quando cadastradas e `null` para anúncios legados.
- `POST /v1/anuncio/{id}/pausar`: somente o proprietário; `ATIVO → PAUSADO`; sai da vitrine e não recebe novas propostas.
- `POST /v1/anuncio/{id}/reativar`: somente o proprietário; `PAUSADO → PENDENTE` (nova moderação obrigatória antes de voltar à vitrine). Não publica automaticamente.
- `POST /v1/anuncio/{id}/vendido`: somente o proprietário; `ATIVO/PAUSADO → VENDIDO`, operação sem volta pela API do vendedor; sai da vitrine e não permite novas propostas.
- Todos os endpoints têm JWT obrigatório, controle de ownership, lock pessimista do anúncio e gravação de ator/data da mudança. Transições inválidas retornam 409; terceiros recebem 403.
- **Nenhuma operação apaga propostas, mensagens ou dados de auditoria anteriores**.
- Conta com role ADMIN/REVIEWER não tem privilégios para substituir o dono nestes endpoints; precisa ser o anunciante.

## Frontend

- Campos obrigatórios `Cidade` e `UF` no cadastro/edição, incluindo estados válidos do Brasil.
- Localização real (declarada) nos cards, detalhes e filtros de busca (`?city=...&state=MG`).
- Botões Pausar, Solicitar reativação e Marcar vendido mostrados **somente quando a transição é possível**, sem falsos `200 OK`. As ações passam pelo BFF Next.js e são revalidadas pelo Spring.
- O endpoint de edição continua devolvendo o anúncio para `PENDENTE` como já fazia.
- Cinco anúncios sintéticos de homologação continuam etiquetados `DEMO` e agora declaram `Várzea da Palma/MG`. Usuário de teste vendedor usa credencial aleatória criada por `deploy/setup-env.sh` e não é publicado no Git.

## Verificações

- Testes de autorização, transições, histórico e mapeamento de localização no backend.
- Teste MySQL de migração V11 → V12 preservando histórico e dados legados nulos.
- Testes de mapeamento do frontend e validação de UF/cidade.
- Smoke integrada Docker usa usuário comprador, vendedor e admin sintéticos para cadastro, proposta, mensagens, pausa, reativação e venda.

## Limites de homologação

O status **VENDIDO é uma declaração do vendedor**: a plataforma não verifica transferência de veículo ou pagamento. A cidade/UF também são dados declarados. Nenhuma implantação comercial ou consulta governamental faz parte desta fase.

**Dependências:** revisão de PRs anteriores, migração V12 em banco de testes restaurado antes de qualquer produção, SMTP, política LGPD e verificação facial real ainda pendentes. A mudança não ativa serviços pagos e não toca em `main` ou banco real.
