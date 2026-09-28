package br.com.estacionamento.service;

import br.com.estacionamento.exception.*;
import br.com.estacionamento.model.StatusTicket;
import br.com.estacionamento.model.Ticket;
import br.com.estacionamento.model.TipoVaga;
import br.com.estacionamento.model.Vaga;
import br.com.estacionamento.repository.TicketRepository;
import br.com.estacionamento.repository.VagaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.regex.Pattern;

@Service
public class EstacionamentoService {

    // formato antigo (ABC1234) ou Mercosul (ABC1D23)
    private static final Pattern PLACA = Pattern.compile("^[A-Z]{3}[0-9][A-Z0-9][0-9]{2}$");

    private final VagaRepository vagaRepository;
    private final TicketRepository ticketRepository;
    private final CalculadoraTarifa calculadora;
    private final Clock clock;

    public EstacionamentoService(VagaRepository vagaRepository,
                                 TicketRepository ticketRepository,
                                 CalculadoraTarifa calculadora,
                                 Clock clock) {
        this.vagaRepository = vagaRepository;
        this.ticketRepository = ticketRepository;
        this.calculadora = calculadora;
        this.clock = clock;
    }

    @Transactional
    public Ticket registrarEntrada(String placa, TipoVaga tipo) {
        String placaNormalizada = normalizarPlaca(placa);
        if (tipo == null) {
            throw new RegraNegocioException("Tipo de vaga é obrigatório");
        }

        if (ticketRepository.existsByPlacaAndStatus(placaNormalizada, StatusTicket.ABERTO)) {
            throw new VeiculoJaEstacionadoException(placaNormalizada);
        }

        Vaga vaga = vagaRepository.findFirstByTipoAndOcupadaFalseOrderByNumeroAsc(tipo)
                .orElseThrow(() -> new EstacionamentoLotadoException(tipo));

        vaga.ocupar();
        vagaRepository.saveAndFlush(vaga);

        Ticket ticket = new Ticket(placaNormalizada, vaga, agora());
        return ticketRepository.saveAndFlush(ticket);
    }

    @Transactional
    public Ticket registrarSaida(Long ticketId) {
        Ticket ticket = buscarTicket(ticketId);

        if (ticket.getStatus() == StatusTicket.FECHADO) {
            throw new RegraNegocioException("Ticket " + ticketId + " já foi encerrado");
        }

        LocalDateTime saida = agora();
        BigDecimal valor = calculadora.calcular(ticket.getEntrada(), saida);

        ticket.fechar(saida, valor);
        ticket.getVaga().liberar();
        return ticketRepository.save(ticket);
    }

    @Transactional(readOnly = true)
    public Ticket buscarTicket(Long ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNaoEncontradoException(ticketId));
    }

    @Transactional(readOnly = true)
    public long vagasLivres(TipoVaga tipo) {
        return vagaRepository.countByTipoAndOcupadaFalse(tipo);
    }

    // o banco guarda até microssegundos; truncar evita que o horário gravado fique maior que o real
    private LocalDateTime agora() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.MICROS);
    }

    private String normalizarPlaca(String placa) {
        if (placa == null || placa.isBlank()) {
            throw new RegraNegocioException("Placa é obrigatória");
        }
        String normalizada = placa.replace("-", "").trim().toUpperCase();
        if (!PLACA.matcher(normalizada).matches()) {
            throw new RegraNegocioException("Placa inválida: " + placa);
        }
        return normalizada;
    }
}
