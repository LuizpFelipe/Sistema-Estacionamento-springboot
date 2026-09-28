package br.com.estacionamento.config;

import br.com.estacionamento.model.Ticket;
import br.com.estacionamento.model.TipoVaga;
import br.com.estacionamento.model.Vaga;
import br.com.estacionamento.repository.TicketRepository;
import br.com.estacionamento.repository.VagaRepository;
import br.com.estacionamento.service.CalculadoraTarifa;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.LocalDateTime;

@Configuration
public class AppConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    public CommandLineRunner carregarDados(VagaRepository vagas,
                                           TicketRepository tickets,
                                           CalculadoraTarifa calculadora,
                                           Clock clock,
                                           @Value("${app.dados-exemplo:false}") boolean dadosExemplo) {
        return args -> {
            if (vagas.count() > 0) {
                return;
            }
            Vaga[] v = new Vaga[16];
            for (int i = 1; i <= 10; i++) v[i] = vagas.save(new Vaga(i, TipoVaga.CARRO));
            for (int i = 11; i <= 13; i++) v[i] = vagas.save(new Vaga(i, TipoVaga.MOTO));
            for (int i = 14; i <= 15; i++) v[i] = vagas.save(new Vaga(i, TipoVaga.PCD));

            if (!dadosExemplo) {
                return;
            }
            LocalDateTime agora = LocalDateTime.now(clock);

            estacionado(vagas, tickets, v[1], "ABC1D23", agora.minusMinutes(40));
            estacionado(vagas, tickets, v[2], "BRA2E19", agora.minusHours(3));
            estacionado(vagas, tickets, v[11], "MOT0A12", agora.minusMinutes(10));

            encerrado(tickets, calculadora, v[3], "QWE1234", agora.minusHours(5), agora.minusHours(4).minusMinutes(50));
            encerrado(tickets, calculadora, v[3], "RTY5F67", agora.minusHours(4), agora.minusHours(2));
            encerrado(tickets, calculadora, v[14], "PCD9G88", agora.minusHours(6), agora.minusHours(5).minusMinutes(20));
            encerrado(tickets, calculadora, v[4], "LNG4H56", agora.minusDays(1).minusHours(10), agora.minusHours(1));
        };
    }

    private void estacionado(VagaRepository vagas, TicketRepository tickets, Vaga vaga, String placa, LocalDateTime entrada) {
        vaga.ocupar();
        vagas.save(vaga);
        tickets.save(new Ticket(placa, vaga, entrada));
    }

    private void encerrado(TicketRepository tickets, CalculadoraTarifa calculadora, Vaga vaga,
                           String placa, LocalDateTime entrada, LocalDateTime saida) {
        Ticket t = new Ticket(placa, vaga, entrada);
        t.fechar(saida, calculadora.calcular(entrada, saida));
        tickets.save(t);
    }
}
