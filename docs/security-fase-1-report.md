# Auto Minas — entrega da Fase 1 de segurança

## Inspeção e escopo

Os três documentos fornecidos foram lidos antes das alterações: auditoria, schema SQL proposto e OpenAPI v1. A implementação preserva o modelo atual; não aplica o schema proposto completo nem as fases 2 a 7.

Base inspecionada: `main` em `9dec8090469700e609e217932890e52ea512e08b`, de 26/09/2026. Não havia commits na main posteriores a 30/09/2026. Foram analisados controllers, services, repositories, entidades, DTOs, segurança, configuração e build. Backend: Java 21, Spring Boot 4.1.0, JJWT 0.11.5, Maven wrapper 3.9.16; MySQL configurado, mas versão e schema efetivos do servidor implantado não puderam ser verificados. O projeto não tinha migrations Flyway; a cobertura existente não comprovava as vulnerabilidades.

Já existiam BCrypt, autenticação JWT e verificações de propriedade em alterações de usuários/anúncios e participantes de propostas/chat. Essas regras foram preservadas e exercitadas por testes. Restavam vazamento de contatos nas consultas de usuários, falta de roles/estados, segredo JWT padrão, upload escrevendo antes da autorização, validação insuficiente e ausência de revogação. A revisão também identificou corrida entre atualização de perfil e troca de senha/desativação.

## Alterações e vulnerabilidades corrigidas

- Consultas públicas usam DTO sem email/telefone/senha. Listagem e busca de usuários exigem ADMIN/REVIEWER; rota administrativa exige ADMIN. `/me` mantém contatos próprios.
- Cadastro fixa USER/PENDING_CONTACT_VERIFICATION, sem mass assignment de privilégio. JWT consulta papel/estado reais no banco; contas suspensas, bloqueadas e desativadas não autenticam.
- JWT exige chave externa de pelo menos 32 bytes fora de local e recusa o padrão antigo. Expiração de 30 minutos; versão de sessão invalida tokens após troca de senha/desativação. Tokens antigos exigem novo login.
- Atualização de conta usa lock e refresh para impedir que entidade antiga restaure senha, versão de sessão ou conta desativada. DELETE de conta preserva os relacionamentos e marca DELETED.
- Upload verifica propriedade antes de gravar, bloqueia anúncio para limitar concorrência e valida todo o lote. JPEG/PNG, extensão/MIME/assinatura/decoder consistentes, 5 MiB/foto, 10/lote, 20/anúncio, 6000 pixels/eixo e 20 milhões de pixels. Reencodificação remove metadados e conteúdo agregado; nomes gerados pelo servidor. Rollback remove arquivos novos.
- DTOs têm limites compatíveis com colunas legadas, valores positivos e senha limitada a 72 bytes UTF-8 do BCrypt. Paginação limitada a 50.
- Erros são ProblemDetail com mensagens controladas, sem valores rejeitados, stack traces ou detalhes SQL. Senhas não são serializadas nem incluídas em toString; logging SQL/debug de segurança desativado. A conta automática do Spring e o log de sua senha gerada foram eliminados.
- Flyway V1 para banco vazio e V2 aditiva; springdoc atualizado de 2.8.13 para 3.1.1, compatível com Spring Boot 4.1. Java/Spring/JJWT preservados.

## Validação real

Comando da CI: `./mvnw -B clean verify`, Java 21, H2 para regressões HTTP/serviços e MySQL 8.4 efêmero para migrations/validação Hibernate. Os testes usam JWT real, repositórios reais e diretório temporário; nenhum banco de produção foi usado.

| Execução | Resultado observado |
|---|---|
| Local `./mvnw -version` | Maven 3.9.16; ambiente local tinha Java 17 |
| Local `./mvnw -B test` | Não compilou: DNS de repo.maven.apache.org impediu baixar parent; não considerado sucesso |
| CI 37957256185 — reprodução inicial | 15 testes, 8 falhas; reproduziu vazamentos, upload gravado antes de 403 e validação insuficiente |
| CI 37957748782 — correções iniciais | 15 testes, zero falhas/erros; BUILD SUCCESS |
| CI 37958066200 — roles/estados/MySQL | 39 testes, zero falhas/erros/skips; BUILD SUCCESS |
| CI 37958356984 — corrida reproduzida | 48 testes, 1 falha e 1 erro; perfil concorrente podia restaurar senha/estado |
| CI 37958566624 — lock aplicado | 48 testes, 1 erro no harness transacional (UnexpectedRollbackException) |
| CI 37958805625 — harness e limites concorrentes | 50 testes, zero falhas/erros/skips; BUILD SUCCESS |
| CI 37958990514 — IDOR adicionais | 52 testes, 1 falha: requisição de edição retornou 400 antes da autorização |
| CI 37959250298 — teste de logs | 53 testes, 2 falhas: caso anterior e conta padrão automática ainda presente |
| CI 37959604095 — conta padrão desativada | 53 testes, 1 falha: fixture omitindo boolean obrigatório; teste de logs passou |

Resultado final: [CI 37959817109](https://github.com/josenetoo-dev/veiculos-api/actions/runs/37959817109), commit `070fe81e55a27cc50de0025409184ea5b7448150`: **53 testes, zero falhas, zero erros, zero ignorados; BUILD SUCCESS**. Distribuição: 44 regressões de segurança, 6 configurações JWT, 2 migrations MySQL e 1 validação Hibernate/MySQL. O log desta execução não contém a senha automática do Spring. O caso de edição IDOR passou após fornecer `segundaMao=false` na fixture; a API não foi enfraquecida para aceitar boolean nulo. `git diff --check` também concluiu com código 0.

Cobertura: anônimo, usuário comum, ADMIN/REVIEWER, contatos públicos/privados, IDOR/BOLA em contas/anúncios/fotos/propostas/chat, mass assignment de role, JWT inválido/expirado/versão/estado/role forjado, troca de senha, desativação, corridas de conta, upload falso/corrompido/extensão/MIME/tamanho/lote/pixels/concorrência, ausência de arquivos após rejeição e rollback, validação sem eco de senha e logs sem credenciais/tokens nos fluxos testados.

Os testes MySQL ensaiam banco vazio, recusa de baseline automático, baseline manual de legado e preservação de IDs/hashes/contatos/URLs; Hibernate valida a estrutura migrada. Isso não comprova que o banco implantado tem schema idêntico.

## Banco, adoção e riscos restantes

Consulte `security-fase-1-database.md` para procedimento obrigatório: backup restaurado em cópia, inspeção de versão/SHOW CREATE TABLE, baseline explícito versão 1 somente se compatível, V2 e validação na cópia. V2 adiciona role/status/token_version, com USER/ACTIVE/0 para contas existentes; não apaga dados nem estreita tipos. Flyway fica desativado por padrão. Nenhuma migration foi executada em produção.

O deploy exige schema compatível já migrado e JWT_SECRET externo; iniciar contra o banco antigo sem V2 falhará em validate. Não houve alteração da infraestrutura de produção. WebP permanece rejeitado; arquivos antigos não são saneados; queda abrupta/falha de cleanup exige reconciliação de storage. Rate limiting, refresh/logout completos, confirmação real de contato e políticas de retenção ficam pendentes. O teste de logs verifica os fluxos exercitados, não constitui auditoria de todos os sistemas externos de logging.

Anúncios continuam nascendo ATIVO; exclusão de anúncio mantém comportamento legado destrutivo. Nenhuma exclusão de dados existentes foi executada nesta implementação. Papéis administrativos exigem provisionamento operacional autorizado; não há senha/admin padrão.

## Fase 2

Separar veículo e anúncio, ensaiar migração e plano de retorno preservando IDs/relações, estruturar mídia com proprietário/visibilidade/storage key, revisar respostas públicas e disponibilizar listagens dos recursos próprios. Não antecipar workflows de publicação, identidade, fontes oficiais e transações das fases seguintes neste PR.

## Arquivos alterados

- `.github/workflows/security-tests.yml`
- `docs/security-fase-1-database.md`
- `docs/security-fase-1-plan.md`
- `pom.xml`
- `src/main/java/com/josenetoo_dev/veiculos_api/PaginationConfig.java`
- `src/main/java/com/josenetoo_dev/veiculos_api/VeiculosApiApplication.java`
- `src/main/java/com/josenetoo_dev/veiculos_api/controller/AdminUsuarioController.java`
- `src/main/java/com/josenetoo_dev/veiculos_api/controller/AnuncioFotoController.java`
- `src/main/java/com/josenetoo_dev/veiculos_api/controller/UsuarioController.java`
- `src/main/java/com/josenetoo_dev/veiculos_api/dto/anuncio_dto/AnuncioRequest.java`
- `src/main/java/com/josenetoo_dev/veiculos_api/dto/anuncio_foto_dto/AnuncioFotoRequest.java`
- `src/main/java/com/josenetoo_dev/veiculos_api/dto/auth/LoginRequest.java`
- `src/main/java/com/josenetoo_dev/veiculos_api/dto/auth/RegisterRequest.java`
- `src/main/java/com/josenetoo_dev/veiculos_api/dto/mensagem_dto/MensagemRequest.java`
- `src/main/java/com/josenetoo_dev/veiculos_api/dto/proposta_dto/ContrapropostaRequest.java`
- `src/main/java/com/josenetoo_dev/veiculos_api/dto/proposta_dto/PropostaRequest.java`
- `src/main/java/com/josenetoo_dev/veiculos_api/dto/usuario_dto/TrocarSenhaRequest.java`
- `src/main/java/com/josenetoo_dev/veiculos_api/dto/usuario_dto/UsuarioPublicResponse.java`
- `src/main/java/com/josenetoo_dev/veiculos_api/dto/usuario_dto/UsuarioRequest.java`
- `src/main/java/com/josenetoo_dev/veiculos_api/dto/usuario_dto/UsuarioResponse.java`
- `src/main/java/com/josenetoo_dev/veiculos_api/enums/Role.java`
- `src/main/java/com/josenetoo_dev/veiculos_api/enums/StatusUsuario.java`
- `src/main/java/com/josenetoo_dev/veiculos_api/exception/GlobalExceptionHandler.java`
- `src/main/java/com/josenetoo_dev/veiculos_api/model/Anuncio.java`
- `src/main/java/com/josenetoo_dev/veiculos_api/model/AnuncioFoto.java`
- `src/main/java/com/josenetoo_dev/veiculos_api/model/Mensagem.java`
- `src/main/java/com/josenetoo_dev/veiculos_api/model/Proposta.java`
- `src/main/java/com/josenetoo_dev/veiculos_api/model/Usuario.java`
- `src/main/java/com/josenetoo_dev/veiculos_api/repository/AnuncioFotoRepository.java`
- `src/main/java/com/josenetoo_dev/veiculos_api/repository/AnuncioRepository.java`
- `src/main/java/com/josenetoo_dev/veiculos_api/repository/UsuarioRepository.java`
- `src/main/java/com/josenetoo_dev/veiculos_api/security/JwtFilter.java`
- `src/main/java/com/josenetoo_dev/veiculos_api/security/JwtUtil.java`
- `src/main/java/com/josenetoo_dev/veiculos_api/security/SecurityConfig.java`
- `src/main/java/com/josenetoo_dev/veiculos_api/security/SecurityErrors.java`
- `src/main/java/com/josenetoo_dev/veiculos_api/service/AnuncioFotoService.java`
- `src/main/java/com/josenetoo_dev/veiculos_api/service/AuthService.java`
- `src/main/java/com/josenetoo_dev/veiculos_api/service/ImageStorage.java`
- `src/main/java/com/josenetoo_dev/veiculos_api/service/ImageValidator.java`
- `src/main/java/com/josenetoo_dev/veiculos_api/service/UsuarioService.java`
- `src/main/java/com/josenetoo_dev/veiculos_api/validation/SafePassword.java`
- `src/main/java/com/josenetoo_dev/veiculos_api/validation/SafePasswordValidator.java`
- `src/main/resources/application.yaml`
- `src/main/resources/db/migration/V1__legacy_schema.sql`
- `src/main/resources/db/migration/V2__user_security.sql`
- `src/test/java/com/josenetoo_dev/veiculos_api/JwtConfigurationTest.java`
- `src/test/java/com/josenetoo_dev/veiculos_api/MySqlMigrationTest.java`
- `src/test/java/com/josenetoo_dev/veiculos_api/MySqlSchemaValidationTest.java`
- `src/test/java/com/josenetoo_dev/veiculos_api/SecurityRegressionTest.java`
- `docs/security-fase-1-report.md`
