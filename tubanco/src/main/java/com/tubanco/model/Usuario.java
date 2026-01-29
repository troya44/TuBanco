package com.tubanco.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String nombre;
    
    @Column(nullable = false, length = 100)
    private String apellidos;
    
    @Column(unique = true, nullable = false, length = 20)
    private String dni;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String password;
    
    @Column(length = 15)
    private String telefono;
    
    @Column(name = "fecha_registro")
    private LocalDateTime fechaRegistro;

    @Column(unique = true)
    private String numeroCuenta;

    private Double saldo;

    private Boolean activo;

    // Constructor vacío requerido por JPA
    public Usuario() {
    }
    
    // Constructor para registro
    public Usuario(String nombre, String apellidos, String dni, String email, String password) {
        this.nombre = nombre;
        this.apellidos = apellidos;
        this.dni = dni;
        this.email = email;
        this.password = password;
        this.fechaRegistro = LocalDateTime.now();
        this.activo = true;
        this.saldo = 0.0; // Inicializamos a cero para evitar NullPointerException
    }

    // Lógica automática para generar el IBAN antes de guardar en la base de datos
    @PrePersist
    public void generarNumeroCuenta() {
        if (this.numeroCuenta == null) {
            // Genera: ES + 14 dígitos aleatorios
            long numeroAleatorio = (long) (Math.random() * 100000000000000L);
            this.numeroCuenta = "ES" + String.format("%014d", numeroAleatorio);
        }
        if (this.saldo == null) {
            this.saldo = 0.0;
        }
    }

    // --- GETTERS Y SETTERS ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }

    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    public String getNumeroCuenta() { return numeroCuenta; }
    public void setNumeroCuenta(String numeroCuenta) { this.numeroCuenta = numeroCuenta; }

    public Double getSaldo() { return saldo; }
    public void setSaldo(Double saldo) { this.saldo = saldo; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}