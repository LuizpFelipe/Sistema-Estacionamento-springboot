package br.com.estacionamento.dto;

import br.com.estacionamento.model.StatusTicket;
import br.com.estacionamento.model.Ticket;
import br.com.estacionamento.model.TipoVaga;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TicketResponse(
        Long id,
        String placa,
        Integer numeroVaga,
        TipoVaga tipoVaga,
        LocalDateTime entrada,
        LocalDateTime saida,
        BigDecimal valor,
        StatusTicket status
) {
    public static TicketResponse de(Ticket t) {
        return new TicketResponse(
                t.getId(),
                t.getPlaca(),
                t.getVaga().getNumero(),
                t.getVaga().getTipo(),
                t.getEntrada(),
                t.getSaida(),
                t.getValor(),
                t.getStatus()
        );
    }
}
