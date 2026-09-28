# API de Estacionamento

Trabalho da disciplina de Engenharia de Software - Desenvolvimento e Documentação de Testes.

## Integrante

|Nome|RA|
|-|-|
|Luiz Felipe Pintor|347395-2025|

## Descrição do projeto

É uma API em Spring Boot para controlar a entrada e a saída de veículos em um estacionamento.
Quando um carro entra, o sistema procura uma vaga livre do tipo escolhido (carro, moto ou PcD) e gera um ticket.
Na saída, calcula quanto o cliente tem que pagar e libera a vaga.

Funcionalidades:

* registrar entrada de veículo
* registrar saída e calcular o valor
* consultar um ticket
* consultar quantas vagas livres tem de cada tipo

Regras que o sistema segue:

* a placa pode estar no formato antigo (ABC1234) ou Mercosul (ABC1D23)
* o mesmo carro não pode entrar de novo se ainda estiver lá dentro
* se não tiver vaga do tipo pedido, a entrada é recusada
* até 15 minutos não paga nada
* a primeira hora custa R$ 10,00 e cada hora a mais R$ 5,00 (fração conta como hora cheia)
* o máximo cobrado por dia é R$ 40,00
* duas requisições ao mesmo tempo não podem pegar a mesma vaga

Na primeira vez que a aplicação sobe, ela cria 15 vagas (1 a 10 carro, 11 a 13 moto, 14 e 15 PcD)
e alguns tickets de exemplo, para já ter dados para testar no Swagger.

O modelo do banco tem duas tabelas, VAGA e TICKET (uma vaga tem vários tickets ao longo do tempo).
O MER completo está em [docs/mer.md](docs/mer.md).

## Tecnologias

* Java 21
* Spring Boot 3.5 (Web, Data JPA, Validation)
* PostgreSQL (banco da aplicação)
* H2 em memória (banco usado só nos testes)
* JUnit 5, Mockito, AssertJ e MockMvc (testes)
* JaCoCo (cobertura de código)
* Swagger (documentação da API)
* Maven
* Git / GitHub

## Como executar o projeto

Precisa ter o JDK 21, o Maven e o PostgreSQL instalados.

1. Clonar o repositório

```bash
git clone git clone https://github.com/LuizpFelipe/Sistema-Estacionamento-springboot.git
```

2. Criar o banco no PostgreSQL

```sql
CREATE DATABASE estacionamento;
```

Se preferir usar Docker, o arquivo `docker-compose.yml` já cria o banco:

```bash
docker compose up -d
```

3. Conferir usuário e senha do banco em `src/main/resources/application.properties`.
O padrão é usuário `postgres` e senha `postgres`. Se for diferente, dá para mudar no arquivo
ou passar pelas variáveis de ambiente `DB\_USER` e `DB\_PASSWORD`.
4. Rodar a aplicação

```bash
mvn spring-boot:run
```

Ou pelo Eclipse: botão direito em `EstacionamentoApplication.java` > Run As > Java Application.

5. Abrir o Swagger no navegador: http://localhost:8080/swagger-ui.html

Endpoints:

|Método|Rota|O que faz|
|-|-|-|
|POST|/api/tickets/entrada|registra a entrada. Ex: `{"placa": "ABC1D23", "tipoVaga": "CARRO"}`|
|POST|/api/tickets/{id}/saida|registra a saída e calcula o valor|
|GET|/api/tickets/{id}|consulta um ticket|
|GET|/api/vagas/livres?tipo=CARRO|mostra quantas vagas livres tem do tipo|

## Tipos de testes

Os testes ficam em `src/test/java`, separados por pasta conforme o tipo.

O foco do trabalho são três tipos:

* **Repetidos** (`repetido/RepetidoTest`, e o CT04 em `CalculadoraTarifaTest`): usam o `@RepeatedTest` do JUnit
para rodar o mesmo teste centenas ou milhares de vezes, com placas e tempos sorteados. Servem para achar erros
que só acontecem de vez em quando.
* **Concorrência** (`concorrencia/ConcorrenciaEntradaTest`): várias threads chamam o sistema no mesmo instante,
como se vários carros chegassem juntos. Conferem se dois carros nunca ficam na mesma vaga e se o mesmo carro
não entra duas vezes.
* **Carga** (`carga/CargaTest`): a aplicação sobe numa porta de verdade e recebe milhares de requisições HTTP.
Mede o tempo de resposta e confere se não dá erro. As métricas aparecem no console.

E alguns testes de base, para garantir que cada regra funciona sozinha:

* **Unitários** (`service/`): testam o cálculo da tarifa e as regras do service, usando Mockito no lugar do banco.
* **Integração com banco** (`repository/`): testam as consultas no banco H2.
* **API** (`controller/`): testam os endpoints com MockMvc, conferindo o código HTTP e o JSON de resposta,
e um fluxo completo de entrada e saída.

Ferramentas usadas nos testes:

* **JUnit 5**: framework que roda os testes (`@Test`, `@RepeatedTest`, `@ParameterizedTest`)
* **AssertJ**: para escrever as verificações, por exemplo `assertThat(valor).isEqualByComparingTo("15.00")`
* **Mockito**: cria objetos falsos, para testar o service sem depender do banco
* **MockMvc**: faz requisições HTTP nos controllers sem precisar subir o servidor
* **JaCoCo**: mede quanto do código foi executado pelos testes (cobertura)

## Como executar os testes

```bash
mvn test
```

Os testes não precisam do PostgreSQL, eles usam o H2 em memória. Então dá para rodar logo depois de clonar.

Também dá para rodar pelo Eclipse: botão direito em `src/test/java` > Run As > JUnit Test.

Para rodar só um tipo de teste:

```bash
mvn test -Dtest=RepetidoTest
mvn test -Dtest=ConcorrenciaEntradaTest
mvn test -Dtest=CargaTest
```

Depois do `mvn test`, o relatório de cobertura fica em `target/site/jacoco/index.html`.

Os testes também rodam sozinhos no GitHub Actions a cada push.

## Resultado dos testes

Fiz 37 casos de teste (CT01 a CT37). Como alguns testes repetem várias vezes, no total são 13.551 execuções.

* Execuções: 13.551
* Aprovadas: 13.551
* Reprovadas: 0
* Taxa de aprovação: 100%
* Cobertura de linhas: 86% (no pacote `service`, onde ficam as regras, deu 100%)

Nos testes de carga, a consulta de vagas aguentou 5.000 requisições com 50 clientes ao mesmo tempo sem nenhum erro,
com tempo médio de 7 ms.

Na primeira vez que rodei todos os testes, os testes repetidos deram 1.701 erros. O problema era o arredondamento
do horário quando o banco salvava a entrada. Corrigi e depois disso todos passaram. Está explicado na documentação.

A documentação completa dos testes está em [docs/documentacao-testes.md](docs/documentacao-testes.md).

