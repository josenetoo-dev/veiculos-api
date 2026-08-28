# VZP Car — contrato de dados

O que o front-end consome hoje. Se o banco devolver estes campos com estes nomes, o site funciona sem mudança de layout.

---

## 1. `veiculos` — o que a tela realmente usa

| Campo | Tipo | Obrigatório | Observação |
|---|---|---|---|
| `id` | int / uuid | sim | chave |
| `codigo` | text | sim | código de vitrine, ex. `VZ-1041` — é o que o vendedor busca |
| `nome` | text | sim | marca + modelo, ex. `Toyota Corolla XEi` |
| `versao` | text | sim | linha de apoio, ex. `2.0 Flex CVT · 1 dono` |
| `ano_modelo` | int | sim | a tela mostra `ano-1/ano` |
| `km` | int | sim | número puro; a formatação é do front |
| `preco` | decimal(12,2) | sim | número puro, sem `R$` |
| `categoria` | enum | sim | `Seminovos` · `Premium` · `Blindados` · `Motos` · `Consignação` |
| `cambio` | enum | sim | `Automático` · `Manual` · `Automatizado` |
| `combustivel` | enum | sim | `Flex` · `Gasolina` · `Diesel` · `Elétrico` · `Híbrido` |
| `cor` | text | não | |
| `final_placa` | char(1) | não | |
| `laudo_cautelar` | text/enum | não | ex. `Aprovado — sem sinistro` |
| `documentacao` | text | não | ex. `IPVA 2026 pago, sem multas` |
| `garantia` | text | não | ex. `3 meses motor e câmbio` |
| `unidade_id` | fk | sim | → `unidades` |
| `status` | enum | sim | `disponivel` · `reservado` · `vendido` — o site só lista `disponivel` |
| `destaque` | bool | sim | alimenta "Selecionados da semana" na home |
| `created_at` | timestamp | sim | ordenação por "mais recentes" |

**Importante:** valores monetários e km vêm como número, nunca como string formatada. Enums em texto exatamente como acima — a tela compara pelo valor para filtrar.

### `veiculo_fotos`
| Campo | Tipo | Observação |
|---|---|---|
| `id` | int | |
| `veiculo_id` | fk | |
| `url` | text | |
| `ordem` | int | `0` = foto de capa (é a que aparece no card) |
| `tipo` | enum | `frente` · `traseira` · `interior` · `painel` · `outro` |

### `veiculo_opcionais`
Tabela de junção simples: `veiculo_id` + `opcional_id`, com `opcionais(id, nome)`. A ficha lista só os nomes.

---

## 2. `unidades`

`id`, `nome`, `endereco`, `cidade`, `uf`, `horario` (texto livre, ex. `Seg a sex 9h–19h · sáb 9h–15h`), `telefone_whatsapp`, `lat`, `lng` (para o mapa), `ativa` (bool).

---

## 3. `usuarios` (área do cliente)

| Campo | Tipo | Observação |
|---|---|---|
| `id` | uuid | |
| `nome` | text | |
| `email` | text unique | |
| `telefone` | text | |
| `senha_hash` | text | **nullable** — quem entra por Google não tem senha |
| `email_verificado_em` | timestamp | |
| `created_at` | timestamp | |

### `usuario_identidades` (é isso que faz o login Google funcionar)
`id`, `usuario_id` (fk), `provedor` (`google`), `provedor_id` (o `sub` que o Google devolve — **unique junto com `provedor`**), `email_provedor`, `created_at`.

Duas regras que evitam 90% dos bugs de login social:
1. Casar conta pelo `provedor_id`, **não** pelo e-mail (e-mail muda, `sub` não).
2. Se o e-mail do Google já existir em `usuarios`, vincular a identidade ao usuário existente em vez de criar um duplicado.

### `favoritos`
`usuario_id` + `veiculo_id` + `created_at`, com unique no par.

---

## 4. Leads — três origens, mesma ideia

Pode ser uma tabela só com `tipo`, ou três. Sugiro uma só: `leads` com `tipo` em `simulacao` · `avaliacao` · `contato`.

Campos comuns: `id`, `tipo`, `nome`, `telefone`, `email`, `mensagem`, `veiculo_id` (nullable), `unidade_id` (nullable), `usuario_id` (nullable — lead pode ser anônimo), `origem` (`site`), `status` (`novo` · `em_atendimento` · `ganho` · `perdido`), `created_at`.

Específicos de `simulacao`: `valor_veiculo`, `valor_entrada`, `prazo_meses`, `parcela_estimada`, `taxa_aplicada`.

Específicos de `avaliacao`: `marca`, `modelo`, `ano`, `km`, `intencao` (`venda_direta` · `consignacao` · `troca`), `observacoes`.

---

## 5. `config_financiamento`

A tela calcula a parcela no navegador com a fórmula de prestação fixa (Tabela Price):

```
parcela = (P * i) / (1 - (1 + i)^-n)
```

onde `P` = preço − entrada, `i` = taxa mensal em decimal, `n` = prazo em meses.

Guarde `taxa_mensal` (hoje `1.79`) em uma tabela de configuração, não fixa no código — assim a loja muda a taxa sem publicar o site. Campos: `taxa_mensal`, `entrada_minima_pct`, `prazos_disponiveis` (ex. `[24,36,48,60]`), `vigente_desde`.

---

## 6. Endpoints que o front espera

| Tela | Chamada |
|---|---|
| Home | `GET /veiculos?destaque=true&limit=3` |
| Estoque | `GET /veiculos?q=&categoria=&preco_max=&cambio=&ordenar=&page=` |
| Ficha | `GET /veiculos/:codigo` (com fotos e opcionais embutidos) |
| Simulador | `GET /config-financiamento` · `POST /leads` |
| Avaliação | `POST /leads` |
| Contato | `GET /unidades` · `POST /leads` |
| Login | `POST /auth/login` · `POST /auth/registro` · `GET /auth/google` → callback |
| Favoritos | `GET/POST/DELETE /favoritos` (autenticado) |

Filtros: paginação no servidor a partir de ~60 carros. `ordenar` aceita `relevancia` · `preco_asc` · `preco_desc` · `km_asc`.

---

## 7. Cuidados

- **Índices**: `veiculos(status, categoria, preco)`, `veiculos(codigo)` unique, `usuario_identidades(provedor, provedor_id)` unique.
- **Preço**: `decimal`, nunca `float` — float erra centavo.
- **Nunca** deletar veículo vendido: mude `status` para `vendido`. O histórico é o que sustenta relatório de vendas.
- **OAuth Google**: o `client_secret` fica só no servidor. O navegador nunca vê.
- **Senha**: `bcrypt` ou `argon2`. Nada de MD5/SHA1.
