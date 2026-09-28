package br.com.estacionamento.repository;

import br.com.estacionamento.model.TipoVaga;
import br.com.estacionamento.model.Vaga;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VagaRepository extends JpaRepository<Vaga, Long> {

    Optional<Vaga> findFirstByTipoAndOcupadaFalseOrderByNumeroAsc(TipoVaga tipo);

    long countByTipoAndOcupadaFalse(TipoVaga tipo);
}
