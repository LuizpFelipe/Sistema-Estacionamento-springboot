package br.com.estacionamento.carga;

import br.com.estacionamento.model.TipoVaga;
import br.com.estacionamento.model.Vaga;
import br.com.estacionamento.repository.TicketRepository;
import br.com.estacionamento.repository.VagaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Testes de carga")
class CargaTest {

    private static final Pattern ID_JSON = Pattern.compile("\"id\"\\s*:\\s*(\\d+)");

    @LocalServerPort
    private int porta;
    @Autowired
    private VagaRepository vagaRepository;
    @Autowired
    private TicketRepository ticketRepository;

    private final HttpClient http = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Test
    @DisplayName("CT36 - 5.000 consultas de vagas livres com 50 clientes simultâneos")
    void cargaConsultaVagas() throws Exception {
        Metricas m = executarCarga("CT36 - Consulta de vagas livres", 5_000, 50, i ->
                http.send(get("/api/vagas/livres?tipo=CARRO"), HttpResponse.BodyHandlers.ofString()).statusCode());

        assertThat(m.erros5xx()).isZero();
        assertThat(m.status200()).isEqualTo(5_000);
        assertThat(m.p95Ms()).isLessThan(500); // limite folgado para não depender da máquina
    }

    @Test
    @DisplayName("CT37 - 1.000 ciclos completos de entrada e saída com 20 clientes simultâneos")
    void cargaEntradaESaida() throws Exception {
        ticketRepository.deleteAll();
        vagaRepository.deleteAll();
        for (int i = 1; i <= 100; i++) vagaRepository.save(new Vaga(i, TipoVaga.CARRO));

        AtomicInteger conflitos = new AtomicInteger();

        Metricas m = executarCarga("CT37 - Entrada + saída via API", 1_000, 20, i -> {
            String placa = String.format("CRG%04d", i);
            HttpResponse<String> entrada = http.send(
                    post("/api/tickets/entrada", "{\"placa\":\"" + placa + "\",\"tipoVaga\":\"CARRO\"}"),
                    HttpResponse.BodyHandlers.ofString());

            if (entrada.statusCode() != 201) {
                if (entrada.statusCode() == 409) conflitos.incrementAndGet();
                return entrada.statusCode();
            }

            Matcher id = ID_JSON.matcher(entrada.body());
            if (!id.find()) return 500;
            return http.send(post("/api/tickets/" + id.group(1) + "/saida", ""), HttpResponse.BodyHandlers.ofString())
                    .statusCode();
        });

        System.out.printf("  Ciclos completos: %d | Recusados com 409 (disputa de vaga): %d%n",
                m.status200(), conflitos.get());

        // 409 é esperado quando dois clientes disputam a mesma vaga; erro 5xx ou requisição perdida não
        assertThat(m.erros5xx()).isZero();
        assertThat(m.status200() + conflitos.get()).isEqualTo(1_000);
        assertThat(m.status200()).isPositive();
        assertThat(vagaRepository.countByTipoAndOcupadaFalse(TipoVaga.CARRO))
                .as("ao final da carga todas as vagas devem estar livres")
                .isEqualTo(100);
    }

    @FunctionalInterface
    private interface Requisicao {
        int executar(int indice) throws Exception;
    }

    private record Metricas(int total, int status200, int erros5xx, double mediaMs, long p95Ms, double reqPorSegundo) {
    }

    private Metricas executarCarga(String nome, int total, int clientes, Requisicao requisicao) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(clientes);
        List<Long> tempos = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger ok = new AtomicInteger();
        AtomicInteger erros5xx = new AtomicInteger();

        long inicio = System.nanoTime();
        for (int i = 0; i < total; i++) {
            int indice = i;
            pool.submit(() -> {
                long t0 = System.nanoTime();
                try {
                    int status = requisicao.executar(indice);
                    if (status >= 200 && status < 300) ok.incrementAndGet();
                    if (status >= 500) erros5xx.incrementAndGet();
                } catch (Exception e) {
                    erros5xx.incrementAndGet();
                } finally {
                    tempos.add((System.nanoTime() - t0) / 1_000_000);
                }
            });
        }
        pool.shutdown();
        assertThat(pool.awaitTermination(120, TimeUnit.SECONDS)).as("carga terminou a tempo").isTrue();
        double segundos = (System.nanoTime() - inicio) / 1_000_000_000.0;

        List<Long> ordenados = new ArrayList<>(tempos);
        Collections.sort(ordenados);
        long p95 = ordenados.get((int) Math.ceil(ordenados.size() * 0.95) - 1);
        double media = ordenados.stream().mapToLong(Long::longValue).average().orElse(0);

        Metricas m = new Metricas(total, ok.get(), erros5xx.get(), media, p95, total / segundos);

        System.out.println();
        System.out.println("===== " + nome + " =====");
        System.out.printf("  Requisições: %d | Clientes simultâneos: %d%n", total, clientes);
        System.out.printf("  Sucesso (2xx): %d | Erros 5xx/timeout: %d%n", m.status200(), m.erros5xx());
        System.out.printf("  Tempo total: %.2f s | Vazão: %.1f req/s%n", segundos, m.reqPorSegundo());
        System.out.printf("  Tempo médio: %.1f ms | p95: %d ms | máximo: %d ms%n",
                m.mediaMs(), m.p95Ms(), ordenados.get(ordenados.size() - 1));
        return m;
    }

    private HttpRequest get(String caminho) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + porta + caminho))
                .timeout(Duration.ofSeconds(10)).GET().build();
    }

    private HttpRequest post(String caminho, String json) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + porta + caminho))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
    }
}
