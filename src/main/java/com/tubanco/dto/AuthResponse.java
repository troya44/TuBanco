package com.tubanco.dto;

public class AuthResponse {
    
    private String token;
    private String tipo = "Bearer";
    private String mensaje;
    private Long userId;
    private String email;
    private String nombre;
    
    public AuthResponse(String token, Long userId, String email, String nombre, String mensaje) {
        this.token = token;
        this.userId = userId;
        this.email = email;
        this.nombre = nombre;
        this.mensaje = mensaje;
    }
    
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public String getTipo() {
        return tipo;
    }
    
    public void setTipo(String tipo) {
        this.tipo = tipo;
    }
    
    public String getMensaje() {
        return mensaje;
    }
    
    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getNombre() {
        return nombre;
    }
    
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
}