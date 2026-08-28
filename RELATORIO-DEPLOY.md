# Relatório de Preparação para Deploy — Auto Minas API

## O que foi feito

- **`src/main/resources/application-prod.yaml`** (criado): perfil de produção, ativado via `SPRING_PROFILES_ACTIVE=prod`. Usa variáveis de ambiente para datasource, JWT secret, diretório de upload e URL pública da API, sem afetar o `application.yaml` de desenvolvimento.
- **`src/main/java/.../security/SecurityConfig.java`** (modificado): o método `corsConfigurationSource()` agora lê a variável de ambiente `FRONTEND_URL` e a adiciona à lista de origens permitidas, mantendo `http://localhost:5500` e `http://127.0.0.1:5500` para desenvolvimento local. Import `java.util.ArrayList` adicionado.
- **`Procfile`** (criado): instrui o Railway a subir a aplicação com `java -Dspring.profiles.active=prod -jar target/*.jar`, ativando o perfil `prod`.
- **`pom.xml`**: verificado — `spring-boot-maven-plugin` já está presente no `<build>` e `java.version` já está definido como `21`. Nenhuma alteração foi necessária.

## Variáveis de ambiente necessárias no Railway

| Variável                    | Descrição                                              | Exemplo                          |
|-----------------------------|--------------------------------------------------------|-----------------------------------|
| SPRING_DATASOURCE_URL       | URL do banco MySQL gerado pelo Railway                 | jdbc:mysql://...                 |
| SPRING_DATASOURCE_USERNAME  | Usuário do banco gerado pelo Railway                   | root                              |
| SPRING_DATASOURCE_PASSWORD  | Senha do banco gerado pelo Railway                     | (gerado automaticamente)         |
| JWT_SECRET                  | Chave secreta para assinar os tokens JWT               | MinhaChaveSuperSecreta2026!      |
| APP_PUBLIC_BASE_URL         | URL pública da API após o deploy                       | https://veiculos-api.railway.app |
| FRONTEND_URL                | URL do frontend em produção (para liberar o CORS)      | https://meusite.com              |
| SPRING_PROFILES_ACTIVE      | Perfil Spring ativo — deve ser sempre "prod"           | prod                              |

## Limitações conhecidas (importante ler)

### Upload de fotos
As fotos enviadas são salvas em disco local no caminho `/tmp/uploads/fotos`.
No Railway, esse diretório **não é persistente**: toda vez que o serviço reiniciar ou for atualizado,
as fotos salvas serão perdidas.

Para a apresentação escolar isso não é um problema — os dados podem ser recadastrados.
Para produção real no futuro, a solução correta é migrar o upload para um serviço de armazenamento
em nuvem como Cloudinary (gratuito) ou AWS S3.

### Banco de dados
O Railway oferece um banco MySQL gratuito com limite de uso. Para a apresentação é mais que suficiente.

## Status geral
O projeto está pronto para deploy no Railway após as alterações acima.
