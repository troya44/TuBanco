package com.tubanco.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Random;

import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

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

    // --- NUEVOS CAMPOS DE TARJETA ---
    @Column(unique = true, length = 16)
    private String numeroTarjeta;

    @Column(length = 5)
    private String fechaCaducidad; // Formato MM/YY

    @Column(length = 3)
    private String cvv;

    private Double saldo;

    private Boolean activo;

    private String fotoPerfil = "default.png"; // Para futuras mejoras

    @Column(name = "limite_mensual")
    private Double limiteMensual = 1000.00; // Valor por defecto

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
        this.saldo = 0.0;
        this.limiteMensual = 1000.00; // Puedes ajustar este valor según tus necesidades
        this.fotoPerfil = "default.png"; // Foto de perfil por defecto
    }

    /**
     * Lógica automática antes de guardar en la base de datos (INSERT).
     * Genera IBAN, Tarjeta de Crédito, CVV y Caducidad.
     */
    @PrePersist
    public void generarDatosBancarios() {
        Random random = new Random();

        // 1. Generar IBAN (ES + 14 números)
        if (this.numeroCuenta == null) {
            long numeroAleatorio = (long) (Math.random() * 100000000000000L);
            this.numeroCuenta = "ES" + String.format("%014d", numeroAleatorio);
        }

        // 2. Generar Tarjeta de Crédito (16 números aleatorios)
        if (this.numeroTarjeta == null) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 16; i++) {
                sb.append(random.nextInt(10));
            }
            this.numeroTarjeta = sb.toString();

            // 3. Generar CVV (3 números)
            this.cvv = String.format("%03d", random.nextInt(1000));

            // 4. Generar Fecha Caducidad (Mes actual / Año actual + 5)
            LocalDateTime hoy = LocalDateTime.now();
            this.fechaCaducidad = String.format("%02d/%d",
                    hoy.getMonthValue(),
                    (hoy.getYear() + 5) % 100);
        }

        if (this.saldo == null) {
            this.saldo = 0.0;
        }
    }

    

    // --- GETTERS Y SETTERS ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public String getNumeroCuenta() {
        return numeroCuenta;
    }

    public void setNumeroCuenta(String numeroCuenta) {
        this.numeroCuenta = numeroCuenta;
    }

    public String getNumeroTarjeta() {
        return numeroTarjeta;
    }

    public void setNumeroTarjeta(String numeroTarjeta) {
        this.numeroTarjeta = numeroTarjeta;
    }

    public String getFechaCaducidad() {
        return fechaCaducidad;
    }

    public void setFechaCaducidad(String fechaCaducidad) {
        this.fechaCaducidad = fechaCaducidad;
    }

    public String getCvv() {
        return cvv;
    }

    public void setCvv(String cvv) {
        this.cvv = cvv;
    }

    public Double getSaldo() {
        return saldo;
    }

    public void setSaldo(Double saldo) {
        this.saldo = saldo;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public Double getLimiteMensual() {
        return limiteMensual != null ? limiteMensual : 1000.0;
    }

    public void setLimiteMensual(Double limiteMensual) {
        this.limiteMensual = limiteMensual;
    }

    public String getFotoPerfil() {
        return fotoPerfil;
    }

    public void setFotoPerfil(String fotoPerfil) {
        this.fotoPerfil = fotoPerfil;
    }
}