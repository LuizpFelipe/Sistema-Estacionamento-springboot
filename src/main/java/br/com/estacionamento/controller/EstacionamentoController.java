package br.com.estacionamento.controller;

import br.com.estacionamento.dto.EntradaRequest;
import br.com.estacionamento.dto.TicketResponse;
import br.com.estacionamento.dto.VagasLivresResponse;
import br.com.estacionamento.model.TipoVaga;
import br.com.estacionamento.service.EstacionamentoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class EstacionamentoController {

    private final EstacionamentoService service;

    public EstacionamentoController(EstacionamentoService service) {
        this.service = service;
    }

    @PostMapping("/tickets/entrada")
    @ResponseStatus(HttpStatus.CREATED)
    public TicketResponse registrarEntrada(@RequestBody @Valid EntradaRequest request) {
        return TicketResponse.de(service.registrarEntrada(request.placa(), request.tipoVaga()));
    }

    @PostMapping("/tickets/{id}/saida")
    public TicketResponse registrarSaida(@PathVariable Long id) {
        return TicketResponse.de(service.registrarSaida(id));
    }

    @GetMapping("/tickets/{id}")
    public TicketResponse buscar(@PathVariable Long id) {
        return TicketResponse.de(service.buscarTicket(id));
    }

    @GetMapping("/vagas/livres")
    public VagasLivresResponse vagasLivres(@RequestParam TipoVaga tipo) {
        return new VagasLivresResponse(tipo, service.vagasLivres(tipo));
    }
}
