package com.tubanco.service;

import com.tubanco.model.Usuario;
import com.tubanco.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service // Esta anotación es vital para que Spring lo encuentre
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // 1. Buscamos el usuario en la base de datos por email
        Usuario usuario = usuarioRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("No se encontró el usuario con email: " + email));

        // 2. Retornamos un objeto 'User' que Spring Security sabe procesar
        return User.withUsername(usuario.getEmail())
                   .password(usuario.getPassword()) // La contraseña debe estar encriptada en la BD
                   .roles("USER") // Le asignamos un rol por defecto
                    .build();
    }
}