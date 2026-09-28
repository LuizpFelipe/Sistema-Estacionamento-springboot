package br.com.estacionamento.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 7)
    private String placa;

    // igual à placa enquanto o ticket está aberto e null depois de fechado;
    // o UNIQUE impede dois tickets abertos para o mesmo veículo
    @Column(unique = true, length = 7)
    private String placaAtiva;

    @ManyToOne(optional = false)
    private Vaga vaga;

    @Column(nullable = false)
    private LocalDateTime entrada;

    private LocalDateTime saida;

    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusTicket status = StatusTicket.ABERTO;

    protected Ticket() {
    }

    public Ticket(String placa, Vaga vaga, LocalDateTime entrada) {
        this.placa = placa;
        this.placaAtiva = placa;
        this.vaga = vaga;
        this.entrada = entrada;
    }

    public void fechar(LocalDateTime saida, BigDecimal valor) {
        this.saida = saida;
        this.valor = valor;
        this.placaAtiva = null;
        this.status = StatusTicket.FECHADO;
    }

    public Long getId() {
        return id;
    }

    public String getPlaca() {
        return placa;
    }

    public Vaga getVaga() {
        return vaga;
    }

    public LocalDateTime getEntrada() {
        return entrada;
    }

    public LocalDateTime getSaida() {
        return saida;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public StatusTicket getStatus() {
        return status;
    }
}
