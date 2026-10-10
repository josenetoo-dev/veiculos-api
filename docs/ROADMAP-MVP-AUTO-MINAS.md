# Auto Minas — Controle da entrega MVP (2026-10-09)

> **Fonte de verdade para continuidade do desenvolvimento.** Este documento descreve o código no GitHub, não um deploy funcional. Não confundir CI verde com sistema pronto para pessoas reais.

## Dependências de pull requests — ordem de integração

| Ordem | PR | Branch | Entrega |
|---|---|---|---|
| 1 | [#1](https://github.com/josenetoo-dev/veiculos-api/pull/1) | `security/fase-1` | JWT, roles, autorização, uploads, DTOs, Flyway V1–V3 e Fase 1.1 |
| 2 | [#2](https://github.com/josenetoo-dev/veiculos-api/pull/2) | `feature/fase-2-moderacao` | Aprovação/rejeição de anúncios e V4 |
| 3 | [#3](https://github.com/josenetoo-dev/veiculos-api/pull/3) | `feature/fase-2b-veiculos` | Separação inicial Veículo/Anúncio e V5 |
| 4 | [#4](https://github.com/josenetoo-dev/veiculos-api/pull/4) | `feature/fase-3-verificacoes` | Verificação humana e evidências privadas, V6 |
| 5 | [#5](https://github.com/josenetoo-dev/veiculos-api/pull/5) | `feature/fase-3b-verificacao-email` | Confirmação de contato por e-mail, V7 |
| 6 | [#6](https://github.com/josenetoo-dev/veiculos-api/pull/6) | `feature/fase-4-historico-arquivamento` | Arquivamento preservando negociação, V8 |
| 7 | [#7](https://github.com/josenetoo-dev/veiculos-api/pull/7) | `feature/fase-4b-denuncias` | Denúncias privadas, suspensão e revisão, V9 |
| 8 | [#8](https://github.com/josenetoo-dev/veiculos-api/pull/8) | `feature/fase-5-recuperacao-senha` | Recuperação segura de senha, SMTP opt-in, Flyway V10 |
| 9 | [#9](https://github.com/josenetoo-dev/veiculos-api/pull/9) | `security/fase-5b-midia-autorizada` | Fotos condicionadas à visibilidade do anúncio; remove handler estático |

Cada PR depende do anterior (o `base` de #7 é a branch de #6, etc.). O PR mais recente **não** está pronto para merge isolado na main.

**Procedimento após validação:** merge #1 na main; mudar base do #2 para main, revisar diff e CI; merge #2; mudar base do #3 para main; repetir até o #9. Respeitar aprovações, testes e migrações. Não há merge automático autorizado aqui.

## Implementado no backend (quando os PRs forem integrados)
- Login/cadastro JWT com verificação de status, revogação por versão de token, roles e controle de acesso.
- Contato e-mail com desafio de uso único (SMTP opt-in; contas novas só viram ACTIVE após confirmação).
- Dados privados de usuário fora de respostas públicas.
- Anúncios com fila de moderação; revisores não aprovam o próprio anúncio.
- Veículo separado como entidade física, ainda em **transição com colunas antigas espelhadas**.
- Identidade e veículo sujeitos a **aprovação documental humana**, usando evidências JPEG/PNG criptografadas AES-256-GCM em diretório privado, com respostas somente ao titular/revisor.
- Alteração de características do veículo invalida verificação; alteração de fotos reabre moderação.
- Publicação exige conta ACTIVE + identidade APROVADA + veículo APROVADA + aprovação de anúncio.
- Exclusão lógica de anúncios mantém propostas/mensagens; encerramento de conta arquiva seus anúncios.
- Denúncias de anúncios ATIVO; revisores podem suspender após comprovação, somente ADMIN pode reverter SUSPENSO para PENDENTE.
- Migrações Flyway V1–V10, testes Java/Spring e MySQL efêmero. **Validar CI do commit mais recente antes de marcar como concluído.**

## O que ainda impede o uso público

### P0 — Bloqueadores
1. **Migração real:** restaurar backup do MySQL original num ambiente separado, inspecionar schema legada, aplicar baseline V1 somente se compatível, ensaiar V2–V9 e iniciar com `ddl-auto=validate`. Não executar direto em produção.
2. **Armazenamento seguro:** configurar `VERIFICATION_STORAGE_KEY` (32 bytes aleatórios em Base64), `VERIFICATION_STORAGE_DIR` privado persistente, permissões, backups, restauração e rotação/recuperação de chave. Sem isso os endpoints de documento respondem 503.
3. **SMTP:** configurar `CONTACT_EMAIL_ENABLED=true`, `CONTACT_EMAIL_FROM`, `SMTP_*` e autenticação do domínio/remetente. Sem SMTP, novas contas não se ativam. `EMAIL_CHANGE_ENABLED` é opção separada.
4. **Operadores:** provisionar ADMIN inicial por procedimento restrito, verificar/revisar roles, definir fluxos de revisão, identidade legal e eventual apelação.
5. **Privacidade/LGPD:** política de coleta/retenção/eliminação de documentos, base legal, minimização, termos, responsabilidades de operadores, direitos de titulares e processo de incidentes.
6. **Frontend:** não foi identificado um repositório de frontend do Auto Minas dentre os repositórios conectados. É necessário integrar UI pública, cadastro, moderação, documento, email, propostas, denúncias e estados de erro; testar ponta a ponta.
7. **Validação de segurança:** verificação independente de IDOR/BOLA, proteção do storage, testes em restaurado, análise de segurança dos uploads, logs, backup e observabilidade.

### P1 — Antes de crescer
- Rate limit por IP/conta e anti-spam de cadastro, mensagens, relatórios e SMTP.
- Recuperação de senha foi implementada no PR #8, mas ainda depende de SMTP real, limites de gateway e revisão de segurança.
- Renovação/encerramento de sessão JWT com estratégia explícita.
- Regras e auditoria mais completas para contrapropostas, negociações simultâneas e eventual finalização de venda.
- O PR #9 protege o endpoint `/uploads/fotos/**` por status e autorização; caches/CDNs e fotos legadas requerem migração e homologação do frontend.
- Processo de contestação de decisões, SLA de resposta, políticas de revisores e remoção segura de documentos.
- Transição definitiva para campos de `Veiculo` como fonte única, removendo duplicação do modelo legado com testes de retrocompatibilidade.

## Não afirmar
- Que CPF/RENAVAM/CRLV foi oficialmente conferido via SERPRO ou SENATRAN.
- Que o Auto Minas garante ausência de fraude, propriedade legal, pagamento ou transferência.
- Que Flyway foi testado em dados reais ou executado em produção.
- Que o frontend público está integrado ou a implantação está online.
- Que CI verde substitui testes E2E, revisão jurídica e plano de resposta a incidentes.

## Critério de pronto para apresentação = pronto para uso público
Cadastro → confirmar e-mail → enviar identidade → revisão humana → cadastrar veículo/anúncio → enviar documento do veículo → revisão humana → anúncio aprovado → pesquisa pública → proposta/chat → denúncia/moderação → arquivamento e recuperação do histórico.

O fluxo deve funcionar do navegador/mobile ao banco, com serviços reais, operadores reais, segredos seguros e dados autorizados para o teste, sem simulações que aparentem certificação oficial.

## Próxima decisão operacional
Não fazer merge/deploy até avaliar os bloqueadores P0. O backend pode continuar evoluindo isoladamente. Falta fornecer o repositório ou os arquivos do frontend do Auto Minas para verificar e corrigir a experiência completa.
