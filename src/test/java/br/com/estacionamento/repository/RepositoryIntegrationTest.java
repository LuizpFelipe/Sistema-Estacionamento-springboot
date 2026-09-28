package br.com.estacionamento.repository;

import br.com.estacionamento.model.StatusTicket;
import br.com.estacionamento.model.Ticket;
import br.com.estacionamento.model.TipoVaga;
import br.com.estacionamento.model.Vaga;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@DisplayName("Repositórios (integração com banco)")
class RepositoryIntegrationTest {

    @Autowired
    private VagaRepository vagaRepository;
    @Autowired
    private TicketRepository ticketRepository;

    @Test
    @DisplayName("CT15 - Busca a vaga livre de menor número do tipo pedido")
    void buscaPrimeiraVagaLivre() {
        vagaRepository.save(new Vaga(3, TipoVaga.CARRO));
        vagaRepository.save(new Vaga(1, TipoVaga.CARRO));
        vagaRepository.save(new Vaga(2, TipoVaga.MOTO));

        Optional<Vaga> vaga = vagaRepository.findFirstByTipoAndOcupadaFalseOrderByNumeroAsc(TipoVaga.CARRO);

        assertThat(vaga).isPresent();
        assertThat(vaga.get().getNumero()).isEqualTo(1);
    }

    @Test
    @DisplayName("CT16 - Vagas ocupadas não são retornadas como livres")
    void ignoraVagasOcupadas() {
        Vaga ocupada = new Vaga(1, TipoVaga.PCD);
        ocupada.ocupar();
        vagaRepository.save(ocupada);

        assertThat(vagaRepository.findFirstByTipoAndOcupadaFalseOrderByNumeroAsc(TipoVaga.PCD)).isEmpty();
    }

    @Test
    @DisplayName("CT17 - Conta corretamente as vagas livres por tipo")
    void contaVagasLivres() {
        Vaga ocupada = new Vaga(1, TipoVaga.CARRO);
        ocupada.ocupar();
        vagaRepository.save(ocupada);
        vagaRepository.save(new Vaga(2, TipoVaga.CARRO));
        vagaRepository.save(new Vaga(3, TipoVaga.CARRO));
        vagaRepository.save(new Vaga(4, TipoVaga.MOTO));

        assertThat(vagaRepository.countByTipoAndOcupadaFalse(TipoVaga.CARRO)).isEqualTo(2);
        assertThat(vagaRepository.countByTipoAndOcupadaFalse(TipoVaga.MOTO)).isEqualTo(1);
    }

    @Test
    @DisplayName("CT18 - Banco não permite duas vagas com o mesmo número")
    void numeroDeVagaUnico() {
        vagaRepository.saveAndFlush(new Vaga(1, TipoVaga.CARRO));

        assertThatThrownBy(() -> vagaRepository.saveAndFlush(new Vaga(1, TipoVaga.MOTO)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CT19 - Identifica se a placa tem ticket aberto (e ignora tickets fechados)")
    void verificaTicketAbertoPorPlaca() {
        Vaga vaga = vagaRepository.save(new Vaga(1, TipoVaga.CARRO));
        ticketRepository.save(new Ticket("ABC1234", vaga, LocalDateTime.now()));

        Ticket fechado = new Ticket("XYZ9876", vaga, LocalDateTime.now().minusHours(1));
        fechado.fechar(LocalDateTime.now(), new BigDecimal("10.00"));
        ticketRepository.save(fechado);

        assertThat(ticketRepository.existsByPlacaAndStatus("ABC1234", StatusTicket.ABERTO)).isTrue();
        assertThat(ticketRepository.existsByPlacaAndStatus("XYZ9876", StatusTicket.ABERTO)).isFalse();
    }
}
