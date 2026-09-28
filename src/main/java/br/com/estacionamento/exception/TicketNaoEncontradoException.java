package br.com.estacionamento.exception;

public class TicketNaoEncontradoException extends RuntimeException {
    public TicketNaoEncontradoException(Long id) {
        super("Ticket " + id + " não encontrado");
    }
}
