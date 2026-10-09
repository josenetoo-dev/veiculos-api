# Auto Minas — plano da Fase 1

Base: 9dec809 (26/09/2026); nenhuma alteração posterior a 30/09 na main.
Referências: auditoria, schema MySQL 8 e OpenAPI propostos fornecidos pelo usuário.

Execução direta autorizada: security/fase-1; PR para main sem merge. Nenhuma conexão a produção.

1. Testes reais MockMvc + H2 + JWT para privacidade, IDOR, roles, uploads e validação.
2. DTO público sem contatos; DTO privado somente me/admin/reviewer. Guards no service e na rota.
3. USER/REVIEWER/ADMIN e estados PENDING_CONTACT_VERIFICATION/ACTIVE/SUSPENDED/BLOCKED/DELETED.
   Cadastro sempre USER pendente; contas existentes ACTIVE. Pendente pode usar fluxos existentes, sem selo.
4. JWT obrigatório fora de local, 32 bytes mínimos, 30 minutos e versão de sessão no banco.
5. Upload autoriza e bloqueia linha do anúncio antes de escrever; valida lote inteiro antes do storage.
   JPEG/PNG; WebP rejeitado até haver codec auditado. 5 MiB/foto, 10/lote, 20/anúncio,
   6000 pixels/eixo e 20 milhões de pixels. Reencodar sem metadados; limpar novos arquivos no rollback.
6. Erros ProblemDetail sem valores rejeitados/segredos; limites coerentes com banco legado.
7. Flyway opt-in, V1 estrutura legada para banco vazio, V2 aditiva role/status/token_version.
   Banco existente exige inspeção e baseline manual 1 em cópia; não executar em produção.
8. Compile/test/verify Java 21, relatório dos resultados reais e revisão de diff antes de PR.

Riscos: versão/schema efetivos de produção desconhecidos; não inferir pelo driver.
Preservar IDs bigint, nomes de tabelas/rotas e precisão/escala legadas. Não aplicar schema completo.
Fase 2: separar veículo/anúncio e migrar dados/mídia de forma ensaiada e reversível.
