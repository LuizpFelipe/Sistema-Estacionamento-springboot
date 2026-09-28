package br.com.estacionamento.exception;

public class VeiculoJaEstacionadoException extends RuntimeException {
    public VeiculoJaEstacionadoException(String placa) {
        super("Veículo " + placa + " já está no estacionamento");
    }
}
