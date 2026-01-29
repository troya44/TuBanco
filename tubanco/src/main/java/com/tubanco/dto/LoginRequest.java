package com.tubanco.dto;

import jakarta.validation.constraints.NotBlank;

public class LoginRequest {
    
    @NotBlank(message = "El DNI es obligatorio")
    private String dni;
    
    @NotBlank(message = "La contraseña es obligatoria")
    private String password;
    
    public LoginRequest() {}
    
    public String getDni() {
        return dni;
    }
    
    public void setDni(String dni) {
        this.dni = dni;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
}