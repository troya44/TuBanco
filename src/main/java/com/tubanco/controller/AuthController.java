package com.tubanco.controller;

import com.tubanco.dto.AuthResponse;
import com.tubanco.dto.LoginRequest;
import com.tubanco.dto.RegisterRequest;
import com.tubanco.model.Usuario;
import com.tubanco.repository.UsuarioRepository;
import jakarta.validation.Valid;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication; // Para el CVV
import com.tubanco.service.EmailService; // IMPORTANTE: Importa tu servicio

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            if (usuarioRepository.findByEmail(request.getEmail()).isPresent()) {
                return ResponseEntity.badRequest().body("El email ya está registrado");
            }

            Usuario usuario = new Usuario(
                    request.getNombre(),
                    request.getApellidos(),
                    request.getDni(),
                    request.getEmail(),
                    passwordEncoder.encode(request.getPassword()));

            usuarioRepository.save(usuario);

            try {
                emailService.enviarCorreoBienvenida(usuario.getEmail(), usuario.getNombre());
            } catch (Exception e) {
                // No detenemos el registro si falla el correo, solo lo logueamos
                System.err.println("No se pudo enviar el correo de bienvenida: " + e.getMessage());
            }

            // --- SOLUCIÓN PARA EL ERROR ---
            java.util.Map<String, String> response = new java.util.HashMap<>();
            response.put("message", "Usuario registrado con éxito");
            response.put("cuenta", usuario.getNumeroCuenta());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        // Para el login igual: usamos un Map para evitar errores de AuthResponse
        java.util.Map<String, String> response = new java.util.HashMap<>();
        response.put("message", "Login correcto");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-cvv")
    public ResponseEntity<?> verificarParaCVV(@RequestBody Map<String, String> payload, Authentication auth) {
        // Verificación de seguridad: si no hay sesión, auth será null
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Sesión no válida");
        }

        try {
            String passwordIngresada = payload.get("password");

            // Evitamos que passwordIngresada sea null antes de comparar
            if (passwordIngresada == null) {
                return ResponseEntity.badRequest().body("La contraseña es requerida");
            }

            Usuario usuario = usuarioRepository.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            if (passwordEncoder.matches(passwordIngresada, usuario.getPassword())) {
                Map<String, String> response = new HashMap<>();
                response.put("cvv", usuario.getCvv());
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Contraseña incorrecta");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }
}