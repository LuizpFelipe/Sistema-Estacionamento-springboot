package br.com.estacionamento.exception;

import br.com.estacionamento.model.TipoVaga;

public class EstacionamentoLotadoException extends RuntimeException {
    public EstacionamentoLotadoException(TipoVaga tipo) {
        super("Não há vagas livres do tipo " + tipo);
    }
}
