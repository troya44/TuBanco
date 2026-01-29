package com.tubanco.controller;

import com.tubanco.model.Usuario;
import com.tubanco.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model; // ¡Importante para pasar datos al HTML!
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {

    @Autowired
    private UsuarioRepository usuarioRepository; // Necesitamos esto para buscar el saldo y cuenta

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @GetMapping("/tubanco")
    public String mostrarDashboard(Model model, Authentication authentication) {
        // 1. Obtenemos el email de quien acaba de entrar
        String email = authentication.getName();
        
        // 2. Buscamos al usuario en la base de datos
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        // 3. Enviamos los datos reales al HTML
        model.addAttribute("nombre", usuario.getNombre());
        model.addAttribute("saldo", usuario.getSaldo());
        model.addAttribute("cuenta", usuario.getNumeroCuenta());
        
        return "tubanco"; // Carga tubanco.html con los datos arriba inyectados
    }
}