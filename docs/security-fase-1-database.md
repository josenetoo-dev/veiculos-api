# Adoção de Flyway — Fase 1

## O que mudou

O banco atual usa `usuario`, `anuncio`, `anuncio_foto`, `proposta` e `mensagem`, com IDs bigint.
O schema proposto com `users`, `vehicles` e `listings` não foi aplicado.

- `V1__legacy_schema.sql`: estrutura correspondente às entidades existentes, para banco vazio.
- `V2__user_security.sql`: adiciona somente `usuario.role`, `usuario.status` e `usuario.token_version`.
  Contas existentes recebem USER/ACTIVE/0, mantendo IDs, senhas e dados relacionados.
- Novas contas criadas pela aplicação recebem USER/PENDING_CONTACT_VERIFICATION/0.
- `ddl-auto=validate` fora de local; Flyway desativado por padrão (`FLYWAY_ENABLED=false`).
- Baseline automático é proibido. Nenhuma migration foi executada em produção.

## Banco existente: pré-requisitos antes de qualquer deploy

1. Identificar versão real (`SELECT VERSION()`), fazer backup verificável e restaurá-lo em banco de ensaio.
2. Comparar `SHOW CREATE TABLE` das cinco tabelas com as entidades e V1. Conferir nomes,
   FKs, enum/VARCHAR, nulabilidade, precisão e escala. A versão do driver não prova a versão do servidor.
3. Verificar se já existe `flyway_schema_history`, ou colunas role/status/token_version introduzidas
   fora do controle de migrations. **Interromper a adoção se houver diferenças ou histórico conflitante**;
   não forçar baseline, repair, DROP ou conversões de tipos.
4. Em cópia compatível sem Flyway, executar **baseline explícito versão 1**, com descrição identificando
   a adoção do legado. Isso marca V1 como existente; não executa o CREATE TABLE de V1 nesse banco.
5. Executar migrate até versão 2 na cópia, validate e iniciar a aplicação com `ddl-auto=validate`.
   Comparar contagem de linhas, IDs, hashes, contatos, URLs e relações antes/depois.
6. Testar login, propriedade de anúncios, propostas/chat e upload contra essa cópia.
7. Planejar janela de adoção do banco e da aplicação juntos, sob autorização de operação separada.
   Este PR não executa nem configura a infraestrutura dessa janela.

Não habilitar Flyway indiscriminadamente em banco não vazio: a aplicação deve falhar pedindo baseline,
sem tentar adivinhar a estrutura. Não usar `baseline-on-migrate=true` nem `ddl-auto=update` em produção.

## Banco novo e configuração de execução

Banco vazio pode executar V1/V2 após habilitação explícita. Exige MySQL compatível; a suíte usa MySQL 8.4
isolado. Nenhum teste usa o banco implantado.

Fora do profile local, fornecer `JWT_SECRET` aleatório com no mínimo 32 bytes, além da configuração
real de DB_URL/DB_USERNAME/DB_PASSWORD conforme ambiente. O segredo padrão antigo é recusado.
O profile local sem segredo usa chave efêmera a cada início; seus tokens deixam de valer ao reiniciar.
Tokens legados sem claim `tv` são recusados: usuários precisarão autenticar novamente após adoção.
Access tokens expiram em 30 minutos; refresh/logout completos permanecem para evolução posterior.

Papéis administrativos não podem ser fornecidos no cadastro nem alterados pelo DTO de perfil.
Provisionar ADMIN/REVIEWER somente por operação autorizada no banco, após verificar a identidade da
conta; não existe conta/senha administrativa automática. Suspensão/bloqueio/desativação são consultados
em cada requisição. As alterações de estado administrativas completas não foram adicionadas ao produto.
Contas pendentes ainda usam os fluxos existentes; não há confirmação de contato ou selo simulado.

## Compatibilidade e limites

- `GET /v1/usuario/{id}` retorna apenas id/nome/criadoEm. Contatos próprios ficam em `/me`.
- `GET /v1/usuario` e `/buscar` exigem ADMIN ou REVIEWER; `/v1/admin/usuarios` exige ADMIN.
- IDs e rotas atuais foram preservados; UUIDs e renomeação global da OpenAPI proposta não foram aplicados.
- Os campos de descrição continuam VARCHAR(255), por isso o DTO limita 255. Ampliação exige migration
  de domínio posterior; valores monetários do DTO aceitam até 10 dígitos inteiros e 2 decimais,
  sem estreitar as colunas DECIMAL existentes.
- Upload: JPEG/PNG, 5 MiB/foto, 10 fotos/lote, 20/anúncio, 6000 pixels/eixo, 20 milhões de pixels.
  MIME, extensão, assinatura e decoder devem concordar. Conteúdo é reencodado sem metadados.
  WebP é rejeitado até haver codec confiável; nome e extensão no storage são gerados no servidor.
- Arquivos antigos não são varridos nem removidos. Conteúdo perigoso legado não é saneado automaticamente.
  Arquivos novos são limpos no rollback; falha de limpeza ou queda abrupta requer reconciliação de storage.
- DELETE da própria conta agora desativa e preserva relações. Isso não substitui política de retenção,
  anonimização ou exclusão definitiva de dados pessoais.

## Riscos restantes / Fase 2

Separar veículo de anúncio, migrar dados em cópia com plano de retorno, criar mídia com owner/visibility/storage key,
revisar respostas públicas e implementar listagem de recursos próprios. Workflow de publicação, identidade,
fontes oficiais e transações pertencem às fases seguintes. Anúncios ainda nascem ATIVO e a exclusão de anúncio
mantém o comportamento legado destrutivo; esses pontos não foram confundidos com autorização corrigida.
