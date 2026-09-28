package br.com.estacionamento.service;

import br.com.estacionamento.exception.RegraNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Calculadora de tarifa")
class CalculadoraTarifaTest {

    private final CalculadoraTarifa calculadora = new CalculadoraTarifa();
    private final LocalDateTime entrada = LocalDateTime.of(2026, 9, 26, 8, 0);

    @ParameterizedTest(name = "CT01 - {0} min de permanência deve custar R$ {1}")
    @CsvSource({
            "0,    0.00",
            "15,   0.00",
            "16,   10.00",
            "60,   10.00",
            "61,   15.00",
            "120,  15.00",
            "121,  20.00",
            "420,  40.00",
            "421,  40.00",
            "1439, 40.00",
            "1440, 40.00",
            "1455, 50.00",
            "2880, 80.00"
    })
    @DisplayName("CT01 - Valores de cobrança nos limites das faixas")
    void deveCalcularValorNosLimitesDasFaixas(long minutos, String esperado) {
        BigDecimal valor = calculadora.calcular(entrada, entrada.plusMinutes(minutos));
        assertThat(valor).isEqualByComparingTo(esperado);
    }

    @Test
    @DisplayName("CT02 - Saída anterior à entrada deve ser rejeitada")
    void deveRejeitarSaidaAntesDaEntrada() {
        assertThatThrownBy(() -> calculadora.calcular(entrada, entrada.minusMinutes(1)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("anterior");
    }

    @Test
    @DisplayName("CT03 - Entrada ou saída nulas devem ser rejeitadas")
    void deveRejeitarDatasNulas() {
        assertThatThrownBy(() -> calculadora.calcular(null, entrada))
                .isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> calculadora.calcular(entrada, null))
                .isInstanceOf(RegraNegocioException.class);
    }

    @RepeatedTest(value = 10_000, name = "CT04 - execução {currentRepetition} de {totalRepetitions}")
    @DisplayName("CT04 - Propriedades da cobrança com tempos aleatórios")
    void propriedadesDaCobrancaDevemSempreValer() {
        long minutos = ThreadLocalRandom.current().nextLong(0, 3 * 24 * 60);

        BigDecimal valor = calculadora.calcular(entrada, entrada.plusMinutes(minutos));
        BigDecimal valorUmMinutoDepois = calculadora.calcular(entrada, entrada.plusMinutes(minutos + 1));
        long diasIniciados = minutos / (24 * 60) + 1;

        assertThat(valor).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        assertThat(valor).isLessThanOrEqualTo(
                CalculadoraTarifa.TETO_DIARIO.multiply(BigDecimal.valueOf(diasIniciados)));
        assertThat(valorUmMinutoDepois).isGreaterThanOrEqualTo(valor);
    }
}
