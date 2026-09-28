package br.com.estacionamento.service;

import br.com.estacionamento.exception.RegraNegocioException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

@Component
public class CalculadoraTarifa {

    public static final long TOLERANCIA_MINUTOS = 15;
    public static final BigDecimal PRIMEIRA_HORA = new BigDecimal("10.00");
    public static final BigDecimal HORA_ADICIONAL = new BigDecimal("5.00");
    public static final BigDecimal TETO_DIARIO = new BigDecimal("40.00");

    private static final long MINUTOS_POR_DIA = 24 * 60;

    public BigDecimal calcular(LocalDateTime entrada, LocalDateTime saida) {
        if (entrada == null || saida == null) {
            throw new RegraNegocioException("Entrada e saída são obrigatórias");
        }
        if (saida.isBefore(entrada)) {
            throw new RegraNegocioException("Saída não pode ser anterior à entrada");
        }

        long minutos = Duration.between(entrada, saida).toMinutes();
        if (minutos <= TOLERANCIA_MINUTOS) {
            return BigDecimal.ZERO.setScale(2);
        }

        long diasCompletos = minutos / MINUTOS_POR_DIA;
        long minutosRestantes = minutos % MINUTOS_POR_DIA;

        BigDecimal valorDias = TETO_DIARIO.multiply(BigDecimal.valueOf(diasCompletos));
        BigDecimal valorRestante = valorParcial(minutosRestantes).min(TETO_DIARIO);

        return valorDias.add(valorRestante).setScale(2);
    }

    private BigDecimal valorParcial(long minutos) {
        if (minutos == 0) {
            return BigDecimal.ZERO;
        }
        if (minutos <= 60) {
            return PRIMEIRA_HORA;
        }
        long horasAdicionais = (minutos - 60 + 59) / 60; // fração de hora conta como hora cheia
        return PRIMEIRA_HORA.add(HORA_ADICIONAL.multiply(BigDecimal.valueOf(horasAdicionais)));
    }
}
