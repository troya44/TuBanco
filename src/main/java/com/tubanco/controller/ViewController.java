package com.tubanco.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Comparator;
import java.util.Map;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import com.tubanco.model.Usuario;
import com.tubanco.model.Tarjeta;
import com.tubanco.model.Movimiento;
import com.tubanco.repository.UsuarioRepository;
import com.tubanco.repository.TarjetaRepository;
import com.tubanco.repository.MovimientoRepository;
import com.tubanco.service.EmailService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.transaction.annotation.Transactional;

@Controller
public class ViewController implements WebMvcConfigurer {

    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private MovimientoRepository movimientoRepository;
    @Autowired
    private TarjetaRepository tarjetaRepository;
    @Autowired
    private EmailService emailService; // Servicio de correo añadido
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @GetMapping("/")
    public String index() {
        return "login";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @GetMapping("/crearTarjeta")
    public String nuevaTarjetaPage() {
        return "crearTarjeta";
    }

    @GetMapping("/tubanco")
    public String mostrarDashboard(Model model, Authentication authentication) {
        String email = authentication.getName();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        List<Tarjeta> todasLasTarjetas = tarjetaRepository.findByUsuario(usuario);
        Tarjeta principal = null;
        List<Tarjeta> secundarias = new ArrayList<>();

        if (!todasLasTarjetas.isEmpty()) {
            todasLasTarjetas.sort(Comparator.comparing(Tarjeta::getId));
            principal = todasLasTarjetas.get(0);
            if (todasLasTarjetas.size() > 1) {
                secundarias = todasLasTarjetas.subList(1, todasLasTarjetas.size());
            }
        }

        model.addAttribute("usuario", usuario);
        model.addAttribute("nombre", usuario.getNombre());
        model.addAttribute("saldo", usuario.getSaldo());
        model.addAttribute("cuenta", usuario.getNumeroCuenta());
        model.addAttribute("tarjetaPrincipal", principal);
        model.addAttribute("misTarjetas", secundarias);
        model.addAttribute("movimientos", movimientoRepository.findByUsuarioOrderByFechaDesc(usuario));

        return "tubanco";
    }

    @GetMapping("/movimientos")
    public String verMovimientosMes(Model model, Authentication authentication) {
        String email = authentication.getName();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        List<Movimiento> movimientos = movimientoRepository.findByUsuarioOrderByFechaDesc(usuario);
        LocalDateTime inicioMes = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        Double gastosEseMes = movimientoRepository.sumarGastosDelMes(usuario, inicioMes);

        model.addAttribute("movimientos", movimientos);
        model.addAttribute("gastosMes", gastosEseMes != null ? Math.abs(gastosEseMes) : 0.0);

        return "movimientosmes";
    }

    @Transactional
    @PostMapping("/ingresar-dinero")
    public String ingresarDinero(@RequestParam Double cantidad, Authentication authentication) {
        String email = authentication.getName();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        usuario.setSaldo(usuario.getSaldo() + cantidad);
        usuarioRepository.save(usuario);

        Movimiento ingreso = new Movimiento();
        ingreso.setImporte(cantidad);
        ingreso.setEstablecimiento("Ingreso Automático");
        ingreso.setFecha(LocalDateTime.now());
        ingreso.setUsuario(usuario);
        movimientoRepository.save(ingreso);

        return "redirect:/tubanco";
    }

    @GetMapping("/transferencias")
    public String pantallaTransferencias(Model model, Authentication auth) {
        Usuario usuario = usuarioRepository.findByEmail(auth.getName()).get();
        model.addAttribute("saldo", usuario.getSaldo());
        return "transferencias";
    }

    @Transactional
    @PostMapping("/transferir")
    public String realizarTransferencia(@RequestParam String emailDestino, @RequestParam Double cantidad,
            Authentication auth, RedirectAttributes ra) {

        Usuario emisor = usuarioRepository.findByEmail(auth.getName()).get();
        Optional<Usuario> receptorOpt = usuarioRepository.findByEmail(emailDestino);

        if (receptorOpt.isEmpty() || emisor.getEmail().equals(emailDestino) || emisor.getSaldo() < cantidad) {
            ra.addFlashAttribute("error", "Error en la operación: verifica saldo o destinatario.");
            return "redirect:/transferencias";
        }

        Usuario receptor = receptorOpt.get();
        emisor.setSaldo(emisor.getSaldo() - cantidad);
        receptor.setSaldo(receptor.getSaldo() + cantidad);
        usuarioRepository.save(emisor);
        usuarioRepository.save(receptor);

        movimientoRepository.save(
                new Movimiento(-cantidad, "Transferencia a " + receptor.getNombre(), LocalDateTime.now(), emisor));
        movimientoRepository.save(
                new Movimiento(cantidad, "Transferencia de " + emisor.getNombre(), LocalDateTime.now(), receptor));

        emailService.enviarCorreo(
                emisor.getEmail(),
                emisor.getNombre(),
                "Envío de transferencia",
                cantidad);

        // Notificación al receptor
        emailService.enviarCorreo(
                receptor.getEmail(),
                receptor.getNombre(),
                "Recepción de transferencia",
                cantidad);

        return "redirect:/tubanco?exito=Transferencia realizada";
    }

    @GetMapping("/perfil")
    public String mostrarPerfil(Model model, Authentication auth) {
        model.addAttribute("usuario", usuarioRepository.findByEmail(auth.getName()).get());
        return "perfil";
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/imagenes/**").addResourceLocations("file:uploads/");
    }

    @PostMapping("/perfil/actualizar-seguridad")
    public String actualizarSeguridad(@RequestParam String passwordActual,
            @RequestParam(required = false) String nuevoLimite,
            @RequestParam(required = false) String nuevaPassword, Authentication auth, RedirectAttributes ra) {
        Usuario usuario = usuarioRepository.findByEmail(auth.getName()).get();
        if (!passwordEncoder.matches(passwordActual, usuario.getPassword())) {
            ra.addFlashAttribute("error", "La contraseña actual no es correcta.");
            return "redirect:/perfil";
        }
        if (nuevoLimite != null && !nuevoLimite.isEmpty())
            usuario.setLimiteMensual(Double.parseDouble(nuevoLimite));
        if (nuevaPassword != null && !nuevaPassword.isEmpty())
            usuario.setPassword(passwordEncoder.encode(nuevaPassword));
        usuarioRepository.save(usuario);
        ra.addFlashAttribute("exito", "Cambios realizados correctamente.");
        return "redirect:/perfil";
    }

    @PostMapping("/perfil/subir-foto")
    public String subirFoto(@RequestParam("archivo") MultipartFile archivo, Authentication auth,
            RedirectAttributes ra) {
        if (archivo.isEmpty())
            return "redirect:/perfil";
        try {
            Usuario usuario = usuarioRepository.findByEmail(auth.getName()).get();
            Files.createDirectories(Paths.get("uploads"));
            String nombreArchivo = usuario.getId() + "_" + archivo.getOriginalFilename();
            Files.copy(archivo.getInputStream(), Paths.get("uploads").resolve(nombreArchivo),
                    StandardCopyOption.REPLACE_EXISTING);
            usuario.setFotoPerfil(nombreArchivo);
            usuarioRepository.save(usuario);
            ra.addFlashAttribute("exito", "Foto actualizada");
        } catch (IOException e) {
            ra.addFlashAttribute("error", "Error al subir la foto.");
        }
        return "redirect:/perfil";
    }

    @GetMapping("/tarjetas")
    public String mostrarFormularioTarjeta(Model model, Authentication auth) {
        Usuario usuario = usuarioRepository.findByEmail(auth.getName()).orElseThrow();
        List<Tarjeta> todas = tarjetaRepository.findByUsuario(usuario);
        todas.sort(Comparator.comparing(Tarjeta::getId));
        model.addAttribute("tarjetaPrincipal", todas.isEmpty() ? null : todas.get(0));
        model.addAttribute("otrasTarjetas", todas.size() > 1 ? todas.subList(1, todas.size()) : new ArrayList<>());
        model.addAttribute("nombre", usuario.getNombre());
        return "crearTarjeta";
    }

    @Transactional
    @PostMapping("/tarjetas/crear")
    public String procesarNuevaTarjeta(@RequestParam String tipo, @RequestParam(required = false) String alias,
            Authentication auth) {
        Usuario usuario = usuarioRepository.findByEmail(auth.getName()).orElseThrow();
        Tarjeta nueva = new Tarjeta();
        nueva.setTipo(tipo);
        nueva.setAlias((alias == null || alias.isEmpty()) ? "Tarjeta " + tipo : alias);
        nueva.setNumero("4000" + new Random().nextInt(1000000000));
        nueva.setCvv(String.format("%03d", new Random().nextInt(1000)));
        nueva.setFechaCaducidad("05/31");
        nueva.setUsuario(usuario);
        tarjetaRepository.save(nueva);
        return "redirect:/tarjetas?exito=tarjeta_creada";
    }

    @PostMapping("/tarjetas/verify-cvv")
    @ResponseBody
    public ResponseEntity<?> verificarYMostrarCVV(@RequestBody Map<String, Object> payload, Authentication auth) {
        String pwd = (String) payload.get("password");
        Long id = Long.valueOf(payload.get("tarjetaId").toString());
        Usuario usuario = usuarioRepository.findByEmail(auth.getName()).orElseThrow();

        if (passwordEncoder.matches(pwd, usuario.getPassword())) {
            return tarjetaRepository.findById(id)
                    .filter(t -> t.getUsuario().getId().equals(usuario.getId()))
                    .map(t -> ResponseEntity.ok((Object) Map.of("cvv", t.getCvv())))
                    .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}