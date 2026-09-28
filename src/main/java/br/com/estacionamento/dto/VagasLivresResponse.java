package br.com.estacionamento.dto;

import br.com.estacionamento.model.TipoVaga;

public record VagasLivresResponse(TipoVaga tipo, long livres) {
}
