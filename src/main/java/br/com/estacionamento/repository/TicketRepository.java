package br.com.estacionamento.repository;

import br.com.estacionamento.model.StatusTicket;
import br.com.estacionamento.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    boolean existsByPlacaAndStatus(String placa, StatusTicket status);
}
