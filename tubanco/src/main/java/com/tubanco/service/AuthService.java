package com.tubanco.service;

import com.tubanco.dto.AuthResponse;
import com.tubanco.dto.LoginRequest;
import com.tubanco.dto.RegisterRequest;
import com.tubanco.model.Usuario;
import com.tubanco.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class AuthService {
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private JwtService jwtService;
    
    public AuthResponse register(RegisterRequest request) {
        
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("El email ya está registrado");
        }
        
        if (usuarioRepository.existsByDni(request.getDni())) {
            throw new RuntimeException("El DNI ya está registrado");
        }
        
        Usuario usuario = new Usuario();
        usuario.setNombre(request.getNombre());
        usuario.setApellidos(request.getApellidos());
        usuario.setDni(request.getDni());
        usuario.setEmail(request.getEmail());
        usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        usuario.setTelefono(request.getTelefono());
        usuario.setFechaRegistro(LocalDateTime.now());
        usuario.setActivo(true);
        
        Usuario usuarioGuardado = usuarioRepository.save(usuario);
        
        String token = jwtService.generateToken(usuarioGuardado.getDni());
        
        return new AuthResponse(
            token,
            usuarioGuardado.getId(),
            usuarioGuardado.getEmail(),
            usuarioGuardado.getNombre(),
            "Usuario registrado exitosamente"
        );
    }
    
    public AuthResponse login(LoginRequest request) {
        
        Usuario usuario = usuarioRepository.findByDni(request.getDni())
            .orElseThrow(() -> new RuntimeException("DNI o contraseña incorrectos"));
        
        if (!usuario.getActivo()) {
            throw new RuntimeException("Usuario inactivo");
        }
        
        if (!passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
            throw new RuntimeException("DNI o contraseña incorrectos");
        }
        
        String token = jwtService.generateToken(usuario.getDni());
        
        return new AuthResponse(
            token,
            usuario.getId(),
            usuario.getEmail(),
            usuario.getNombre(),
            "Bienvenido/a " + usuario.getNombre()
        );
    }
}