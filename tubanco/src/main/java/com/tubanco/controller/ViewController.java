package com.tubanco.controller;

import java.util.List; // ESTA ES LA QUE FALTA
import com.tubanco.model.Usuario;
import com.tubanco.model.Movimiento; // Importante
import com.tubanco.repository.UsuarioRepository;
import com.tubanco.repository.MovimientoRepository; // Importante
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate; // Importante
import java.time.LocalDateTime; // Importante
import java.util.Optional;
import java.io.IOException;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ViewController implements WebMvcConfigurer {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private MovimientoRepository movimientoRepository; // 1. Tienes que añadir esta línea

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
        // 1. Obtener el email del usuario logueado
        String email = authentication.getName();

        // 2. Buscar el usuario una sola vez
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // 3. Lógica para los gastos del mes
        LocalDateTime inicioMes = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        Double gastosEseMes = movimientoRepository.sumarGastosDelMes(usuario, inicioMes);

        // Si los gastos vienen negativos (ej: -50.0), los convertimos a positivo para
        // la barra
        double gastosPositivos = (gastosEseMes != null) ? Math.abs(gastosEseMes) : 0.0;

        // 4. Pasamos los datos al HTML
        model.addAttribute("usuario", usuario); // ESTA ES LA MÁS IMPORTANTE
        model.addAttribute("nombre", usuario.getNombre());
        model.addAttribute("saldo", usuario.getSaldo());
        model.addAttribute("cuenta", usuario.getNumeroCuenta());
        model.addAttribute("tarjeta", usuario.getNumeroTarjeta());
        model.addAttribute("caducidad", usuario.getFechaCaducidad());
        model.addAttribute("gastosMes", gastosPositivos);

        List<Movimiento> movimientos = movimientoRepository.findByUsuarioOrderByFechaDesc(usuario);
        model.addAttribute("movimientos", movimientos);

        return "tubanco";
    }

    @GetMapping("/movimientos")
    public String verMovimientosMes(Model model, Authentication authentication) {
        String email = authentication.getName();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Obtenemos todos los movimientos
        List<Movimiento> movimientos = movimientoRepository.findByUsuarioOrderByFechaDesc(usuario);

        // Calculamos el gasto del mes para mostrarlo también allí si quieres
        LocalDateTime inicioMes = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        Double gastosEseMes = movimientoRepository.sumarGastosDelMes(usuario, inicioMes);

        model.addAttribute("movimientos", movimientos);
        model.addAttribute("gastosMes", gastosEseMes != null ? Math.abs(gastosEseMes) : 0.0);

        return "movimientosmes"; // Nombre del archivo .html sin la extensión
    }

    // ... dentro de la clase ViewController ...

    @Transactional
    @PostMapping("/ingresar-dinero")
    public String ingresarDinero(@RequestParam Double cantidad, Authentication authentication) {
        String email = authentication.getName();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // 1. Actualizar el saldo del usuario automáticamente
        usuario.setSaldo(usuario.getSaldo() + cantidad);
        usuarioRepository.save(usuario);

        // 2. Crear y guardar el movimiento automáticamente
        Movimiento ingreso = new Movimiento();
        ingreso.setImporte(cantidad);
        ingreso.setEstablecimiento("Ingreso Automático");
        ingreso.setFecha(LocalDateTime.now());
        ingreso.setUsuario(usuario);

        movimientoRepository.save(ingreso);

        return "redirect:/tubanco"; // Recarga la página y verás los cambios
    }

    // 1. Mostrar la pantalla de transferencias
    @GetMapping("/transferencias")
    public String pantallaTransferencias(Model model, Authentication auth) {
        Usuario usuario = usuarioRepository.findByEmail(auth.getName()).get();
        model.addAttribute("saldo", usuario.getSaldo());
        return "transferencias";
    }

    // 2. Procesar la transferencia con lógica de "Gasto Mensual"
    @Transactional
    @PostMapping("/transferir")
    public String realizarTransferencia(@RequestParam String emailDestino,
            @RequestParam Double cantidad,
            Authentication auth) {

        Usuario emisor = usuarioRepository.findByEmail(auth.getName()).get();
        Usuario receptor = usuarioRepository.findByEmail(emailDestino)
                .orElseThrow(() -> new RuntimeException("Destinatario no encontrado"));

        if (emisor.getSaldo() < cantidad)
            return "redirect:/transferencias?error=Saldo insuficiente";

        // ACTUALIZACIÓN DE SALDOS
        emisor.setSaldo(emisor.getSaldo() - cantidad);
        receptor.setSaldo(receptor.getSaldo() + cantidad);

        usuarioRepository.save(emisor);
        usuarioRepository.save(receptor);

        // REGISTRO PARA EL EMISOR (Aparecerá en "Gastos del mes" porque es negativo)
        Movimiento movEmisor = new Movimiento();
        movEmisor.setImporte(-cantidad); // Negativo para que reste y sume en gastos
        movEmisor.setEstablecimiento("Transferencia enviada a " + receptor.getNombre());
        movEmisor.setFecha(LocalDateTime.now());
        movEmisor.setUsuario(emisor);
        movimientoRepository.save(movEmisor);

        // REGISTRO PARA EL RECEPTOR (Aparecerá como ingreso)
        Movimiento movReceptor = new Movimiento();
        movReceptor.setImporte(cantidad);
        movReceptor.setEstablecimiento("Transferencia recibida de " + emisor.getNombre());
        movReceptor.setFecha(LocalDateTime.now());
        movReceptor.setUsuario(receptor);
        movimientoRepository.save(movReceptor);

        return "redirect:/tubanco?exito=Transferencia realizada";
    }

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @GetMapping("/perfil")
    public String mostrarPerfil(Model model, Authentication auth) {
        Usuario usuario = usuarioRepository.findByEmail(auth.getName()).get();
        model.addAttribute("usuario", usuario);
        return "perfil";
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/imagenes/**")
                .addResourceLocations("file:uploads/");
    }

    @PostMapping("/perfil/actualizar-seguridad")
    public String actualizarSeguridad(
            @RequestParam String passwordActual,
            @RequestParam(required = false) String nuevoLimite,
            @RequestParam(required = false) String nuevaPassword,
            Authentication auth, RedirectAttributes ra) {

        Usuario usuario = usuarioRepository.findByEmail(auth.getName()).get();

        // VALIDACIÓN CRÍTICA: ¿Es la contraseña correcta?
        if (!passwordEncoder.matches(passwordActual, usuario.getPassword())) {
            ra.addFlashAttribute("error", "La contraseña actual no es correcta.");
            return "redirect:/perfil";
        }

        // Si la contraseña es correcta, procesamos los cambios
        if (nuevoLimite != null && !nuevoLimite.isEmpty()) {
            usuario.setLimiteMensual(Double.parseDouble(nuevoLimite));
        }

        if (nuevaPassword != null && !nuevaPassword.isEmpty()) {
            usuario.setPassword(passwordEncoder.encode(nuevaPassword));
        }

        usuarioRepository.save(usuario);
        ra.addFlashAttribute("exito", "Cambios realizados correctamente.");
        return "redirect:/perfil";
    }

    @PostMapping("/perfil/subir-foto")
    public String subirFoto(@RequestParam("archivo") MultipartFile archivo,
            Authentication auth, RedirectAttributes ra) {
        if (archivo.isEmpty())
            return "redirect:/perfil";

        try {
            String email = auth.getName();
            Usuario usuario = usuarioRepository.findByEmail(email).get();

            // ESTA LÍNEA NUEVA crea la carpeta si no existe
            Path directorioConsultas = Paths.get("uploads");
            if (!Files.exists(directorioConsultas)) {
                Files.createDirectories(directorioConsultas);
            }

            String nombreArchivo = usuario.getId() + "_" + archivo.getOriginalFilename();
            Path ruta = Paths.get("uploads").resolve(nombreArchivo);

            Files.copy(archivo.getInputStream(), ruta, StandardCopyOption.REPLACE_EXISTING);

            usuario.setFotoPerfil(nombreArchivo);
            usuarioRepository.save(usuario);

            ra.addFlashAttribute("exito", "Foto actualizada");
        } catch (IOException e) {
            e.printStackTrace(); // Esto hará que el error real salga en la consola
            ra.addFlashAttribute("error", "Error al subir la foto: " + e.getMessage());
        }

        return "redirect:/perfil";
    }
}