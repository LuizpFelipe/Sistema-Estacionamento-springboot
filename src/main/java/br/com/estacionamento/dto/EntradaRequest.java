package br.com.estacionamento.dto;

import br.com.estacionamento.model.TipoVaga;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EntradaRequest(
        @NotBlank(message = "é obrigatória") String placa,
        @NotNull(message = "é obrigatório") TipoVaga tipoVaga
) {
}
