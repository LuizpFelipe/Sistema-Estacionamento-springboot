package br.com.estacionamento.model;

import jakarta.persistence.*;

@Entity
public class Vaga {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Integer numero;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoVaga tipo;

    @Column(nullable = false)
    private boolean ocupada = false;

    // impede que duas requisições simultâneas ocupem a mesma vaga
    @Version
    private Long versao;

    protected Vaga() {
    }

    public Vaga(Integer numero, TipoVaga tipo) {
        this.numero = numero;
        this.tipo = tipo;
    }

    public void ocupar() {
        if (ocupada) {
            throw new IllegalStateException("Vaga " + numero + " já está ocupada");
        }
        this.ocupada = true;
    }

    public void liberar() {
        this.ocupada = false;
    }

    public Long getId() {
        return id;
    }

    public Integer getNumero() {
        return numero;
    }

    public TipoVaga getTipo() {
        return tipo;
    }

    public boolean isOcupada() {
        return ocupada;
    }
}
