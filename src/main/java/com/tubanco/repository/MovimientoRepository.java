package com.tubanco.repository;

import com.tubanco.model.Movimiento;
import com.tubanco.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface MovimientoRepository extends JpaRepository<Movimiento, Long> {

    // Fíjate bien en el nombre, debe ser idéntico al que usas en el Controller
    List<Movimiento> findByUsuarioOrderByFechaDesc(Usuario usuario);

    @Query("SELECT SUM(m.importe) FROM Movimiento m WHERE m.usuario = :usuario AND m.importe < 0 AND m.fecha >= :inicioMes")
    Double sumarGastosDelMes(@Param("usuario") Usuario usuario, @Param("inicioMes") LocalDateTime inicioMes);

}