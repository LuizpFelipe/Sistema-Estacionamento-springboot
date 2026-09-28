package br.com.estacionamento.controller;

import br.com.estacionamento.exception.EstacionamentoLotadoException;
import br.com.estacionamento.exception.TicketNaoEncontradoException;
import br.com.estacionamento.model.Ticket;
import br.com.estacionamento.model.TipoVaga;
import br.com.estacionamento.model.Vaga;
import br.com.estacionamento.service.EstacionamentoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EstacionamentoController.class)
@DisplayName("API REST (controller)")
class EstacionamentoControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private EstacionamentoService service;

    @Test
    @DisplayName("CT20 - POST /entrada válido retorna 201 com os dados do ticket")
    void entradaRetorna201() throws Exception {
        Ticket ticket = new Ticket("ABC1234", new Vaga(1, TipoVaga.CARRO), LocalDateTime.of(2026, 9, 26, 10, 0));
        when(service.registrarEntrada("ABC1234", TipoVaga.CARRO)).thenReturn(ticket);

        mvc.perform(post("/api/tickets/entrada")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"placa\":\"ABC1234\",\"tipoVaga\":\"CARRO\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.placa").value("ABC1234"))
                .andExpect(jsonPath("$.numeroVaga").value(1))
                .andExpect(jsonPath("$.status").value("ABERTO"));
    }

    @Test
    @DisplayName("CT21 - POST /entrada sem placa retorna 400 com mensagem de validação")
    void entradaSemPlacaRetorna400() throws Exception {
        mvc.perform(post("/api/tickets/entrada")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipoVaga\":\"CARRO\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("placa: é obrigatória"));
    }

    @Test
    @DisplayName("CT22 - POST /entrada com tipo de vaga inexistente retorna 400")
    void tipoVagaInvalidoRetorna400() throws Exception {
        mvc.perform(post("/api/tickets/entrada")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"placa\":\"ABC1234\",\"tipoVaga\":\"CAMINHAO\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CT23 - POST /entrada com estacionamento lotado retorna 409")
    void lotadoRetorna409() throws Exception {
        when(service.registrarEntrada(any(), any())).thenThrow(new EstacionamentoLotadoException(TipoVaga.CARRO));

        mvc.perform(post("/api/tickets/entrada")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"placa\":\"ABC1234\",\"tipoVaga\":\"CARRO\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Não há vagas livres do tipo CARRO"));
    }

    @Test
    @DisplayName("CT24 - POST /saida retorna 200 com o valor cobrado")
    void saidaRetornaValor() throws Exception {
        Ticket ticket = new Ticket("ABC1234", new Vaga(1, TipoVaga.CARRO), LocalDateTime.of(2026, 9, 26, 8, 0));
        ticket.fechar(LocalDateTime.of(2026, 9, 26, 10, 0), new BigDecimal("15.00"));
        when(service.registrarSaida(1L)).thenReturn(ticket);

        mvc.perform(post("/api/tickets/1/saida"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valor").value(15.00))
                .andExpect(jsonPath("$.status").value("FECHADO"));
    }

    @Test
    @DisplayName("CT25 - GET /tickets/{id} inexistente retorna 404")
    void ticketInexistenteRetorna404() throws Exception {
        when(service.buscarTicket(99L)).thenThrow(new TicketNaoEncontradoException(99L));

        mvc.perform(get("/api/tickets/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Ticket 99 não encontrado"));
    }

    @Test
    @DisplayName("CT26 - GET /vagas/livres retorna a quantidade por tipo")
    void vagasLivres() throws Exception {
        when(service.vagasLivres(TipoVaga.MOTO)).thenReturn(3L);

        mvc.perform(get("/api/vagas/livres").param("tipo", "MOTO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("MOTO"))
                .andExpect(jsonPath("$.livres").value(3));
    }
}
