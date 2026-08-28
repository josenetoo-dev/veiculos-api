# Como rodar — Auto Minas (front) + veiculos-api (backend)

Fedora Linux. O front é um arquivo HTML estático; o backend é Java + Spring Boot + MySQL.

## 1. Backend

Instale as dependências:

    sudo dnf install -y java-21-openjdk-devel maven mysql-server

Suba o MySQL e crie o banco (nomes usados hoje em `src/main/resources/application.yaml`):

    sudo systemctl enable --now mysqld
    sudo mysql -e "CREATE DATABASE projeto_veiculos CHARACTER SET utf8mb4;"

O `application.yaml` conecta como `root` sem senha em `projeto_veiculos`. Se
seu MySQL local usa outro usuário/senha, ajuste `spring.datasource.*` no
`application.yaml` (não crie um `veiculos`/`veiculos` — não é o que o projeto
usa hoje).

Variáveis de ambiente opcionais (têm um valor padrão para rodar localmente
sem configurar nada):

    JWT_SECRET=<segredo do token>            # padrão já definido no yaml
    APP_PUBLIC_BASE_URL=http://localhost:8080 # usado para montar a URL pública das fotos

Rode:

    cd caminho/do/veiculos-api
    ./mvnw spring-boot:run     # ou: mvn spring-boot:run

Confira em `http://localhost:8080/swagger-ui.html`.

## 2. CORS

Já vem configurado — `SecurityConfig.corsConfigurationSource()` libera
`http://localhost:5500` e `http://127.0.0.1:5500` com credenciais. Não é
preciso criar nenhuma classe `CorsConfig` nem mexer em mais nada; se você
servir o front em outra porta/origem, adicione-a na lista de
`allowedOrigins` desse bean.

## 3. Front

Não abra por `file://` (o `fetch` falha). Sirva a pasta:

    cd caminho/desta/pasta
    python3 -m http.server 5500

Abra `http://localhost:5500/Auto%20Minas.dc.html`.

A URL do backend é editável no rodapé do site (fica salva no navegador). Padrão: `http://localhost:8080`.

## 4. Primeiro uso

1. Criar conta na tela de login → `POST /auth/register`.
2. Anunciar um carro → `POST /v1/anuncio` (o dono é sempre quem está logado).
3. Estoque lista `GET /v1/anuncio` paginado.
4. Uma segunda conta manda proposta nesse anúncio → aparece para o dono em
   Minha conta → Propostas (`GET /v1/proposta/minhas`).
5. Comprador e dono podem conversar na proposta (chat vinculado a ela).

## Limites conhecidos do backend hoje

- `GET /v1/anuncio/destaques` volta sempre vazio — nada no sistema marca um
  anúncio como destaque ainda (não existe papel de administrador).
- `PUT /v1/anuncio/{id}` sempre devolve o status para `ATIVO` — não dá para
  marcar um anúncio como `PAUSADO`/`VENDIDO` pela API ainda.
- Token de 24h, sem refresh; sem recuperação de senha; sem login social.
- Filtros existentes: categoria, status e código. Não há filtro por preço, km, marca ou modelo.
