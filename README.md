# Auto Minas — Backend

O **Auto Minas** é um marketplace de veículos: quem vende cria o anúncio com fotos, quem compra busca por cidade, manda proposta e conversa com o vendedor.

Este repositório é o **backend** (a API). O frontend está em [auto-minas-frontend](https://github.com/josenetoo-dev/auto-minas-frontend).

O projeto nasceu para a feira de tecnologia da minha escola e funciona em modo de homologação, com dados fictícios. Ainda não tem deploy público.

## Como as peças se encaixam

```
Next.js (frontend)  →  API Spring Boot (este repo)  →  MySQL
```

## Tecnologias

Java 21 · Spring Boot · Spring Security + JWT · JPA/Hibernate · MySQL 8.4 · Flyway

## O que dá pra fazer

- **Criar conta e entrar** com JWT. O token pode ser revogado, então o logout é de verdade.
- **Três tipos de usuário:** comum, revisor e administrador, cada um com suas permissões.
- **Anunciar veículos** com fotos, e depois pausar, reativar, marcar como vendido ou arquivar.
- **Buscar por cidade e UF.**
- **Negociar:** propostas, contrapropostas e mensagens entre comprador e vendedor.
- **Moderação humana:** um revisor analisa anúncios e denúncias.
- **Verificação de documentos:** os arquivos ficam privados e cifrados (quando configurado).
- **Recuperação de senha por e-mail** (opcional).

## Decisões que tomei

- Fotos de anúncios arquivados continuam salvas para manter o histórico, mas o público não consegue vê-las.
- A verificação de documento é feita por uma pessoa. Não há consulta automática ao DENATRAN, SENATRAN, SERPRO ou RENAVAM.
- A biometria com AWS existe, mas fica desligada na homologação.

## Como rodar

Para subir tudo junto (Java, Next.js, MySQL e Caddy), siga o [guia de homologação do frontend](https://github.com/josenetoo-dev/auto-minas-frontend/blob/main/deploy/README.md). Clone as branches `main` dos dois repositórios em pastas vizinhas.

O Docker Compose cria um banco novo com credenciais geradas na sua máquina. **Nunca aponte para um banco real.**

## Testes

```bash
./mvnw -B clean verify
```

Os testes de migração usam um MySQL 8.4 isolado na CI do GitHub Actions (variável `MYSQL_TEST_URL`). Fora do perfil local, a aplicação exige um `JWT_SECRET` válido.

## O que falta antes de ter usuários reais

1. Testar as migrações do Flyway (V1 a V12) numa cópia do banco antigo, com plano de backup.
2. Definir regras de LGPD: retenção e descarte de documentos, fotos e logs.
3. Configurar e-mail (SMTP), proteção contra abuso, monitoramento e mais testes de segurança.
4. Validar tudo hospedado, com HTTPS e em celulares reais.

## Documentação

As fases do projeto estão em [docs/](https://github.com/josenetoo-dev/veiculos-api/blob/main/docs), incluindo o [roadmap de segurança](https://github.com/josenetoo-dev/veiculos-api/blob/main/docs/ROADMAP-MVP-AUTO-MINAS.md).

## O que aprendi construindo

Com o Auto Minas, aprendi bastante sobre o desenvolvimento de uma API mais completa. Consegui entender melhor como organizar o projeto, trabalhar com relacionamentos entre entidades usando JPA e Hibernate, implementar autenticação com JWT e configurar a segurança com Spring Security.

Também aprendi a lidar melhor com validações, tratamento de erros e testes das rotas. Uma das minhas maiores dificuldades foi entender o Spring Security, principalmente como funciona o processo de autenticação e autorização.
