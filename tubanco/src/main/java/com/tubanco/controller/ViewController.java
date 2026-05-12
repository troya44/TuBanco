package com.tubanco.controller;

import java.util.ArrayList;
import java.util.List; // ESTA ES LA QUE FALTA
import com.tubanco.model.Usuario;
import com.tubanco.model.Tarjeta; // Importante para listar las tarjetas del usuario
import com.tubanco.model.Movimiento; // Importante
import com.tubanco.repository.UsuarioRepository;
import com.tubanco.repository.TarjetaRepository; // Importante para listar las tarjetas del usuario

import jakarta.servlet.http.HttpSession;

import com.tubanco.repository.MovimientoRepository; // Importante
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
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
import java.util.Random;
import java.io.IOException;
import java.util.Comparator;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.Map;

@Controller
public class ViewController implements WebMvcConfigurer {

    // Redirección automática de la raíz al login
    @GetMapping("/")
    public String index() {
        return "login"; // Devuelve la vista directamente en lugar de redirigir
    }

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private MovimientoRepository movimientoRepository; // 1. Tienes que añadir esta línea

    @Autowired
    private TarjetaRepository tarjetaRepository; // Asegúrate de que el nombre sea este

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @GetMapping("/crearTarjeta") // <--- Asegúrate de que sea igual a la URL
    public String nuevaTarjetaPage() {
        return "crearTarjeta"; // <--- Este es el nombre del archivo .html en templates
    }

    @GetMapping("/tubanco")
    public String mostrarDashboard(Model model, Authentication authentication) {
        String email = authentication.getName();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // 1. Obtener todas las tarjetas del usuario
        List<Tarjeta> todasLasTarjetas = tarjetaRepository.findByUsuario(usuario);

        // 2. Identificar la principal (ID más bajo) y las secundarias
        Tarjeta principal = null;
        List<Tarjeta> secundarias = new ArrayList<>();

        if (!todasLasTarjetas.isEmpty()) {
            // Ordenamos por ID de menor a mayor
            todasLasTarjetas.sort(Comparator.comparing(Tarjeta::getId));

            // La primera es la principal
            principal = todasLasTarjetas.get(0);

            // El resto van al carrusel (desde el índice 1 hasta el final)
            if (todasLasTarjetas.size() > 1) {
                secundarias = todasLasTarjetas.subList(1, todasLasTarjetas.size());
            }
        }

        // 3. Pasar datos al modelo
        model.addAttribute("usuario", usuario);
        model.addAttribute("nombre", usuario.getNombre());
        model.addAttribute("saldo", usuario.getSaldo());
        model.addAttribute("cuenta", usuario.getNumeroCuenta());

        // Objeto tarjeta principal (para la sección de arriba)
        model.addAttribute("tarjetaPrincipal", principal);

        // Lista de tarjetas restantes (para el carrusel)
        model.addAttribute("misTarjetas", secundarias);

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

    // Dentro de ViewController.java

    // 1. Mostrar el formulario
    @GetMapping("/tarjetas") // Cambiado a /tarjetas para coincidir con tu sidebar
    public String mostrarFormularioTarjeta(Model model, Authentication auth) {
        Usuario usuario = usuarioRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // 1. Buscamos todas sus tarjetas
        List<Tarjeta> todas = tarjetaRepository.findByUsuario(usuario);

        // 2. Ordenamos por ID de menor a mayor (la de ID más bajo es la principal)
        todas.sort(Comparator.comparing(Tarjeta::getId));

        // 3. Separamos para la vista
        Tarjeta principal = todas.isEmpty() ? null : todas.get(0);
        List<Tarjeta> secundarias = todas.size() > 1 ? todas.subList(1, todas.size()) : new ArrayList<>();

        // 4. Agregamos al modelo con los nombres exactos que usa el HTML
        model.addAttribute("tarjetaPrincipal", principal);
        model.addAttribute("otrasTarjetas", secundarias);
        model.addAttribute("nombre", usuario.getNombre()); // Necesario para el titular en el HTML

        return "crearTarjeta"; // Asegúrate de que tu archivo HTML se llame exactamente así
    }

    @Transactional
    @PostMapping("/tarjetas/crear")
    public String procesarNuevaTarjeta(@RequestParam String tipo,
            @RequestParam(required = false) String alias,
            Authentication auth) {
        String email = auth.getName();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Lógica de generación (Mantenemos la tuya que está perfecta)
        String prefijo = "4000";
        if ("PREMIUM".equals(tipo))
            prefijo = "5555";
        if ("VIRTUAL".equals(tipo))
            prefijo = "4912";

        String numeroGenerado = prefijo + String.format("%012d", Math.abs(new Random().nextLong() % 1000000000000L));
        String cvv = String.format("%03d", new Random().nextInt(1000));
        String caducidad = "05/31";

        Tarjeta nueva = new Tarjeta();
        nueva.setNumero(numeroGenerado);
        nueva.setCvv(cvv);
        nueva.setFechaCaducidad(caducidad);
        nueva.setTipo(tipo);
        nueva.setAlias((alias == null || alias.isEmpty()) ? "Tarjeta " + tipo : alias);
        nueva.setUsuario(usuario);

        tarjetaRepository.save(nueva);

        // Redirigimos a la misma página de tarjetas para ver la nueva creada
        return "redirect:/tarjetas?exito=tarjeta_creada";
    }

    // Cambia la ruta para que coincida con el JS: /tarjetas/verify-cvv
    @PostMapping("/tarjetas/verify-cvv")
    @ResponseBody
    public ResponseEntity<?> verificarYMostrarCVV(@RequestBody Map<String, Object> payload, Authentication auth) {
        String passwordIngresada = (String) payload.get("password");
        // Obtenemos el ID de la tarjeta desde el JSON
        Long tarjetaId = Long.valueOf(payload.get("tarjetaId").toString());

        Usuario usuario = usuarioRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (passwordEncoder.matches(passwordIngresada, usuario.getPassword())) {
            // Buscamos la tarjeta específica y verificamos que pertenezca al usuario
            return tarjetaRepository.findById(tarjetaId)
                    .filter(t -> t.getUsuario().getId().equals(usuario.getId()))
                    .map(t -> ResponseEntity.ok((Object) Map.of("cvv", t.getCvv()))) // Cast a Object aquí
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("Tarjeta no encontrada"));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Contraseña incorrecta");
    }

}