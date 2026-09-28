package br.com.estacionamento.concorrencia;

import br.com.estacionamento.model.StatusTicket;
import br.com.estacionamento.model.Ticket;
import br.com.estacionamento.model.TipoVaga;
import br.com.estacionamento.model.Vaga;
import br.com.estacionamento.repository.TicketRepository;
import br.com.estacionamento.repository.VagaRepository;
import br.com.estacionamento.service.EstacionamentoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Testes de concorrência")
class ConcorrenciaEntradaTest {

    @Autowired
    private EstacionamentoService service;
    @Autowired
    private VagaRepository vagaRepository;
    @Autowired
    private TicketRepository ticketRepository;

    @BeforeEach
    void limparBanco() {
        ticketRepository.deleteAll();
        vagaRepository.deleteAll();
    }

    @Test
    @DisplayName("CT28 - 100 carros disputando 1 vaga: apenas 1 ticket é emitido")
    void cemCarrosUmaVaga() throws Exception {
        criarVagas(1);

        Resultado r = executarAoMesmoTempo(100,
                i -> service.registrarEntrada(String.format("TST%04d", i), TipoVaga.CARRO));

        assertThat(r.sucessos()).isEqualTo(1);
        assertThat(r.falhas()).isEqualTo(99);
        assertThat(ticketRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("CT33 - 200 carros disputando 10 vagas: nunca dois tickets abertos na mesma vaga")
    void duzentosCarrosDezVagas() throws Exception {
        criarVagas(10);

        Resultado r = executarAoMesmoTempo(200,
                i -> service.registrarEntrada(String.format("TST%04d", i), TipoVaga.CARRO));

        List<Ticket> abertos = ticketRepository.findAll().stream()
                .filter(t -> t.getStatus() == StatusTicket.ABERTO).toList();
        Map<Long, Long> ticketsPorVaga = abertos.stream()
                .collect(Collectors.groupingBy(t -> t.getVaga().getId(), Collectors.counting()));

        assertThat(r.sucessos()).isBetween(1, 10);
        assertThat(abertos).hasSize(r.sucessos());
        assertThat(ticketsPorVaga.values()).allMatch(qtd -> qtd == 1);
        assertThat(service.vagasLivres(TipoVaga.CARRO)).isEqualTo(10 - r.sucessos());
    }

    @Test
    @DisplayName("CT34 - Mesmo veículo tentando entrar 50 vezes ao mesmo tempo: só 1 entrada")
    void mesmaPlacaSimultanea() throws Exception {
        criarVagas(10);

        Resultado r = executarAoMesmoTempo(50,
                i -> service.registrarEntrada("DUP1A23", TipoVaga.CARRO));

        assertThat(r.sucessos()).isEqualTo(1);
        assertThat(ticketRepository.existsByPlacaAndStatus("DUP1A23", StatusTicket.ABERTO)).isTrue();
        assertThat(ticketRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("CT35 - Saída do mesmo ticket 20 vezes ao mesmo tempo: só 1 cobrança")
    void saidaSimultaneaMesmoTicket() throws Exception {
        criarVagas(1);
        Long ticketId = service.registrarEntrada("SAI1A23", TipoVaga.CARRO).getId();

        Resultado r = executarAoMesmoTempo(20, i -> service.registrarSaida(ticketId));

        assertThat(r.sucessos()).isEqualTo(1);
        assertThat(service.buscarTicket(ticketId).getStatus()).isEqualTo(StatusTicket.FECHADO);
        assertThat(service.vagasLivres(TipoVaga.CARRO)).isEqualTo(1);
    }

    private void criarVagas(int quantidade) {
        for (int i = 1; i <= quantidade; i++) {
            vagaRepository.save(new Vaga(i, TipoVaga.CARRO));
        }
    }

    private record Resultado(int sucessos, int falhas) {
    }

    private Resultado executarAoMesmoTempo(int quantidade, IntConsumer tarefa) throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(Math.min(quantidade, 32));
        CountDownLatch largada = new CountDownLatch(1);
        AtomicInteger sucessos = new AtomicInteger();
        AtomicInteger falhas = new AtomicInteger();

        for (int i = 0; i < quantidade; i++) {
            int indice = i;
            pool.submit(() -> {
                try {
                    largada.await();
                    tarefa.accept(indice);
                    sucessos.incrementAndGet();
                } catch (Exception e) {
                    falhas.incrementAndGet();
                }
            });
        }

        largada.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(60, TimeUnit.SECONDS)).as("todas as threads terminaram").isTrue();
        return new Resultado(sucessos.get(), falhas.get());
    }
}
