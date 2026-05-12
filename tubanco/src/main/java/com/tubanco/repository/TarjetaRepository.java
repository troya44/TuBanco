package com.tubanco.repository;

import com.tubanco.model.Tarjeta;
import com.tubanco.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TarjetaRepository extends JpaRepository<Tarjeta, Long> {
    // Este método es el que usaremos en el Dashboard para listar sus tarjetas
    List<Tarjeta> findByUsuario(Usuario usuario);
}