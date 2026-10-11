# Auto Minas — Backend Spring Boot

API REST do marketplace automotivo Auto Minas, construída com **Java 21, Spring Boot, Spring Security/JWT, JPA/Hibernate, MySQL 8.4 e Flyway**.

## Estado — 11/10/2026

- As fases consolidadas foram integradas na `main` pelo [PR #13](https://github.com/josenetoo-dev/veiculos-api/pull/13).
- O [frontend Next.js](https://github.com/josenetoo-dev/auto-minas-frontend) também foi integrado na `main`, pelo [PR #6](https://github.com/josenetoo-dev/auto-minas-frontend/pull/6).
- Testes Spring Security, autorização, migrações MySQL e regressões passaram na [CI do backend](https://github.com/josenetoo-dev/veiculos-api/actions/runs/38179845489); integração Docker/Chromium com dados fictícios aprovada na [CI do frontend](https://github.com/josenetoo-dev/auto-minas-frontend/actions/runs/38179856085).
- **Não existe implantação pública permanente nem validação de banco real**. A homologação usa banco fictício descartável.

## Funcionalidades implementadas

Autenticação JWT com revogação por versão, papéis `USER/REVIEWER/ADMIN`, cadastro/contato por e-mail, recuperação de senha opcional, anúncios e fotos, moderação humana, verificação documental privada cifrada (quando configurada), denúncias, arquivamento, propostas, contrapropostas, mensagens, localização cidade/UF, pausa/reativação/venda de anúncios.

Fotos arquivadas permanecem armazenadas para preservar histórico, mas sua leitura pública é bloqueada pelo status do anúncio. A política de retenção e exclusão definitiva exige definição antes de operar comercialmente.

A biometria AWS é opcional e permanece desligada na homologação. **Validação humana não é consulta oficial ao DENATRAN/SENATRAN/SERPRO/RENAVAM.**

## Testes

```bash
./mvnw -B clean verify
```

Os testes completos de migração usam um MySQL 8.4 isolado no GitHub Actions, com `MYSQL_TEST_URL`. `application.yaml` exige `JWT_SECRET` válido nos perfis não locais.

## Execução integrada

Para subir Java + Next.js + MySQL + Caddy de maneira coordenada, consulte o [guia de homologação do frontend](https://github.com/josenetoo-dev/auto-minas-frontend/blob/main/deploy/README.md) e use as duas branches `main` em diretórios irmãos.

O Compose cria banco **novo** e usa credenciais geradas localmente. Nunca o aponte para o banco original sem plano de migração.

## Pendências antes de usuários reais

1. Validar Flyway V1–V12 com cópia restaurada do banco legado; planejar backup, restauração e baseline manual se aplicável.
2. Definir LGPD, acesso, retenção/descarte de documentos, fotos, credenciais e trilhas de auditoria.
3. Configurar SMTP, políticas antiabuso, credenciais administrativas, monitoramento e testes de segurança adicionais.
4. Validar fluxos no site hospedado, com HTTPS e celulares reais; habilitar integrações externas somente mediante autorização.

Documentação das fases em [docs/](docs/), incluindo [roadmap de segurança](docs/ROADMAP-MVP-AUTO-MINAS.md). Nenhum recurso pago ou deploy é acionado pelo workflow de testes.
