package br.com.estacionamento.service;

import br.com.estacionamento.exception.EstacionamentoLotadoException;
import br.com.estacionamento.exception.RegraNegocioException;
import br.com.estacionamento.exception.TicketNaoEncontradoException;
import br.com.estacionamento.exception.VeiculoJaEstacionadoException;
import br.com.estacionamento.model.StatusTicket;
import br.com.estacionamento.model.Ticket;
import br.com.estacionamento.model.TipoVaga;
import br.com.estacionamento.model.Vaga;
import br.com.estacionamento.repository.TicketRepository;
import br.com.estacionamento.repository.VagaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.*;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Serviço de estacionamento")
class EstacionamentoServiceTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 9, 26, 10, 0);
    private final Clock clock = Clock.fixed(AGORA.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());

    @Mock
    private VagaRepository vagaRepository;
    @Mock
    private TicketRepository ticketRepository;

    private EstacionamentoService service;

    @BeforeEach
    void setUp() {
        service = new EstacionamentoService(vagaRepository, ticketRepository, new CalculadoraTarifa(), clock);
    }

    @Test
    @DisplayName("CT05 - Entrada válida gera ticket aberto e ocupa a vaga")
    void entradaValidaGeraTicket() {
        Vaga vaga = new Vaga(1, TipoVaga.CARRO);
        when(ticketRepository.existsByPlacaAndStatus("ABC1234", StatusTicket.ABERTO)).thenReturn(false);
        when(vagaRepository.findFirstByTipoAndOcupadaFalseOrderByNumeroAsc(TipoVaga.CARRO)).thenReturn(Optional.of(vaga));
        when(ticketRepository.saveAndFlush(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        Ticket ticket = service.registrarEntrada("ABC1234", TipoVaga.CARRO);

        assertThat(ticket.getStatus()).isEqualTo(StatusTicket.ABERTO);
        assertThat(ticket.getEntrada()).isEqualTo(AGORA);
        assertThat(ticket.getVaga().isOcupada()).isTrue();
        verify(vagaRepository).saveAndFlush(vaga);
    }

    @ParameterizedTest(name = "CT06 - placa \"{0}\" deve ser aceita")
    @ValueSource(strings = {"ABC1234", "ABC1D23", "abc-1234", " abc1d23 "})
    @DisplayName("CT06 - Placas nos formatos antigo e Mercosul são aceitas e normalizadas")
    void placasValidasSaoAceitas(String placa) {
        when(vagaRepository.findFirstByTipoAndOcupadaFalseOrderByNumeroAsc(TipoVaga.CARRO))
                .thenReturn(Optional.of(new Vaga(1, TipoVaga.CARRO)));
        when(ticketRepository.saveAndFlush(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        Ticket ticket = service.registrarEntrada(placa, TipoVaga.CARRO);

        assertThat(ticket.getPlaca()).matches("^[A-Z0-9]{7}$");
    }

    @ParameterizedTest(name = "CT07 - placa \"{0}\" deve ser rejeitada")
    @ValueSource(strings = {"AB1234", "ABCD123", "1234ABC", "ABC12345", "ABC-12"})
    @DisplayName("CT07 - Placas em formato inválido são rejeitadas")
    void placasInvalidasSaoRejeitadas(String placa) {
        assertThatThrownBy(() -> service.registrarEntrada(placa, TipoVaga.CARRO))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Placa inválida");
        verifyNoInteractions(vagaRepository);
    }

    @Test
    @DisplayName("CT08 - Placa vazia é rejeitada")
    void placaVaziaRejeitada() {
        assertThatThrownBy(() -> service.registrarEntrada("  ", TipoVaga.CARRO))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("obrigatória");
    }

    @Test
    @DisplayName("CT09 - Veículo que já está no estacionamento não pode entrar de novo")
    void veiculoJaEstacionadoNaoEntra() {
        when(ticketRepository.existsByPlacaAndStatus("ABC1234", StatusTicket.ABERTO)).thenReturn(true);

        assertThatThrownBy(() -> service.registrarEntrada("ABC1234", TipoVaga.CARRO))
                .isInstanceOf(VeiculoJaEstacionadoException.class);
        verify(ticketRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("CT10 - Estacionamento lotado para o tipo de vaga rejeita a entrada")
    void estacionamentoLotado() {
        when(vagaRepository.findFirstByTipoAndOcupadaFalseOrderByNumeroAsc(TipoVaga.MOTO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrarEntrada("ABC1234", TipoVaga.MOTO))
                .isInstanceOf(EstacionamentoLotadoException.class)
                .hasMessageContaining("MOTO");
    }

    @Test
    @DisplayName("CT11 - Tipo de vaga nulo é rejeitado")
    void tipoNuloRejeitado() {
        assertThatThrownBy(() -> service.registrarEntrada("ABC1234", null))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    @DisplayName("CT12 - Saída após 2h cobra R$ 15,00, fecha o ticket e libera a vaga")
    void saidaCalculaValorELiberaVaga() {
        Vaga vaga = new Vaga(1, TipoVaga.CARRO);
        vaga.ocupar();
        Ticket ticket = new Ticket("ABC1234", vaga, AGORA.minusHours(2));
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        Ticket fechado = service.registrarSaida(1L);

        assertThat(fechado.getStatus()).isEqualTo(StatusTicket.FECHADO);
        assertThat(fechado.getSaida()).isEqualTo(AGORA);
        assertThat(fechado.getValor()).isEqualByComparingTo(new BigDecimal("15.00"));
        assertThat(vaga.isOcupada()).isFalse();
    }

    @Test
    @DisplayName("CT13 - Ticket já encerrado não pode registrar saída de novo")
    void ticketJaFechado() {
        Ticket ticket = new Ticket("ABC1234", new Vaga(1, TipoVaga.CARRO), AGORA.minusHours(1));
        ticket.fechar(AGORA, new BigDecimal("10.00"));
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> service.registrarSaida(1L))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("já foi encerrado");
    }

    @Test
    @DisplayName("CT14 - Saída de ticket inexistente retorna erro")
    void ticketInexistente() {
        when(ticketRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrarSaida(99L))
                .isInstanceOf(TicketNaoEncontradoException.class);
    }
}
