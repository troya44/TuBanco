package com.tubanco.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "tarjetas")
public class Tarjeta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String numero;
    private String cvv;
    private String fechaCaducidad;
    private String tipo; // PREPAGO, DEBITO, PREMIUM, VIRTUAL
    private String alias;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario; // La relación: muchas tarjetas pertenecen a un usuario
}