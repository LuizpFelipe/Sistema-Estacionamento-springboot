package br.com.estacionamento.repetido;

import br.com.estacionamento.exception.RegraNegocioException;
import br.com.estacionamento.model.StatusTicket;
import br.com.estacionamento.model.Ticket;
import br.com.estacionamento.model.TipoVaga;
import br.com.estacionamento.service.EstacionamentoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Testes repetidos")
class RepetidoTest {

    private static final long VAGAS_CARRO = 10;
    private static final String LETRAS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String ALFANUMERICO = LETRAS + "0123456789";

    @Autowired
    private EstacionamentoService service;
    @Autowired
    private MockMvc mvc;

    @RepeatedTest(value = 1000, name = "CT29 - ciclo {currentRepetition} de {totalRepetitions}")
    @DisplayName("CT29 - Mesmo veículo entra e sai 1.000 vezes sem vazar vagas")
    void cicloEntradaSaidaNaoVazaVagas() {
        Ticket entrada = service.registrarEntrada("REP1A23", TipoVaga.CARRO);
        assertThat(service.vagasLivres(TipoVaga.CARRO)).isEqualTo(VAGAS_CARRO - 1);

        Ticket saida = service.registrarSaida(entrada.getId());

        assertThat(saida.getStatus()).isEqualTo(StatusTicket.FECHADO);
        assertThat(service.vagasLivres(TipoVaga.CARRO)).isEqualTo(VAGAS_CARRO);
    }

    @RepeatedTest(value = 1000, name = "CT30 - placa aleatória {currentRepetition} de {totalRepetitions}")
    @DisplayName("CT30 - Placas válidas geradas aleatoriamente são sempre aceitas")
    void placasValidasAleatoriasSaoAceitas() {
        String placa = gerarPlacaValida();

        Ticket ticket = service.registrarEntrada(placa, TipoVaga.MOTO);
        assertThat(ticket.getPlaca()).isEqualTo(placa);

        service.registrarSaida(ticket.getId());
    }

    @RepeatedTest(value = 1000, name = "CT31 - placa inválida {currentRepetition} de {totalRepetitions}")
    @DisplayName("CT31 - Placas inválidas geradas aleatoriamente são sempre recusadas")
    void placasInvalidasAleatoriasSaoRecusadas() {
        String placa = gerarPlacaInvalida();

        assertThatThrownBy(() -> service.registrarEntrada(placa, TipoVaga.CARRO))
                .as("placa gerada: %s", placa)
                .isInstanceOf(RegraNegocioException.class);
    }

    @RepeatedTest(value = 500, name = "CT32 - consulta {currentRepetition} de {totalRepetitions}")
    @DisplayName("CT32 - Consulta a tickets inexistentes sempre retorna 404")
    void ticketInexistenteSempreRetorna404() throws Exception {
        long idInexistente = ThreadLocalRandom.current().nextLong(1_000_000, Long.MAX_VALUE);

        mvc.perform(get("/api/tickets/" + idInexistente))
                .andExpect(status().isNotFound());
    }

    private static String gerarPlacaValida() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 3; i++) sb.append(LETRAS.charAt(r.nextInt(LETRAS.length())));
        sb.append(r.nextInt(10));
        sb.append(r.nextBoolean() ? String.valueOf(r.nextInt(10)) : String.valueOf(LETRAS.charAt(r.nextInt(LETRAS.length()))));
        sb.append(r.nextInt(10)).append(r.nextInt(10));
        return sb.toString();
    }

    private static String gerarPlacaInvalida() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        if (r.nextBoolean()) {
            int tamanho = r.nextBoolean() ? r.nextInt(1, 7) : r.nextInt(8, 12);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < tamanho; i++) sb.append(ALFANUMERICO.charAt(r.nextInt(ALFANUMERICO.length())));
            return sb.toString();
        }
        return r.nextInt(10) + gerarPlacaValida().substring(1);
    }
}
