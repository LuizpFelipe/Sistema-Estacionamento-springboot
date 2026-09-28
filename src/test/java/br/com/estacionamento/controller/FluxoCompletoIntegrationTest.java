package br.com.estacionamento.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Fluxo completo (funcional)")
class FluxoCompletoIntegrationTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper mapper;

    @Test
    @DisplayName("CT27 - Entrada, consulta e saída de um veículo atualizam as vagas corretamente")
    void fluxoEntradaESaida() throws Exception {
        mvc.perform(get("/api/vagas/livres").param("tipo", "PCD"))
                .andExpect(jsonPath("$.livres").value(2));

        String resposta = mvc.perform(post("/api/tickets/entrada")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"placa\":\"QWE1R23\",\"tipoVaga\":\"PCD\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode ticket = mapper.readTree(resposta);
        long id = ticket.get("id").asLong();

        mvc.perform(get("/api/vagas/livres").param("tipo", "PCD"))
                .andExpect(jsonPath("$.livres").value(1));

        mvc.perform(post("/api/tickets/entrada")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"placa\":\"QWE1R23\",\"tipoVaga\":\"PCD\"}"))
                .andExpect(status().isConflict());

        mvc.perform(post("/api/tickets/" + id + "/saida"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FECHADO"))
                .andExpect(jsonPath("$.valor").value(0.0));

        mvc.perform(get("/api/vagas/livres").param("tipo", "PCD"))
                .andExpect(jsonPath("$.livres").value(2));
    }
}
