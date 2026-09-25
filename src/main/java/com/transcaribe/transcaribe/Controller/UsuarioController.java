package com.transcaribe.transcaribe.Controller;

import com.transcaribe.transcaribe.Model.Usuario;
import com.transcaribe.transcaribe.Model.Transaccion;
import com.transcaribe.transcaribe.Model.PreferenciaRutaNotificacion;
import com.transcaribe.transcaribe.Repository.UsuarioRepository;
import com.transcaribe.transcaribe.service.ReciboPdfService;
import com.transcaribe.transcaribe.service.ServiceTranscaribe;
import com.transcaribe.transcaribe.service.TransaccionService;
import com.transcaribe.transcaribe.service.BusService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Locale;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

@Controller
public class UsuarioController {

    @Autowired
    private ServiceTranscaribe service;
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private TransaccionService transaccionService;

    @Autowired
    private BusService busService;

    @Autowired
    private ReciboPdfService reciboPdfService;

    private Usuario obtenerUsuarioLogueado() {
        String correo = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByCorreo(correo).orElse(null);
    }

    @GetMapping("/menu")
    public String menu(Model model) {
        Usuario usuario = obtenerUsuarioLogueado();
        
        if (usuario == null) {
            return "redirect:/login";
        }

        if (!usuario.isVerificado()) {
            return "redirect:/verificar-otp?correo=" + usuario.getCorreo();
        }

        service.procesarNotificacionLogin(usuario.getCorreo());
        
        model.addAttribute("usuario", usuario);
        return "usuarios/cuenta/menu";
    }

    @GetMapping("/perfil")
    public String perfil(Model model) {
        Usuario usuario = obtenerUsuarioLogueado();
        
        if (usuario == null) {
            return "redirect:/login";
        }
        
        model.addAttribute("usuario", usuario);
        return "usuarios/cuenta/perfil";
    }

    @PostMapping("/perfil")
    public String actualizarPerfil(@RequestParam String nombre, 
                                   @RequestParam String correo, 
                                   @RequestParam(required = false) String password, 
                                   Model model) {
        Usuario usuarioActual = obtenerUsuarioLogueado();
        
        if (usuarioActual == null) {
            return "redirect:/login";
        }

        boolean ok = service.editarCredenciales(usuarioActual.getId(), nombre, correo, password, usuarioActual);
        
        if (!ok) {
            model.addAttribute("error", "No se pudo actualizar el perfil. Es posible que el correo ya esté en uso. ❌");
        } else {
            model.addAttribute("mensaje", "¡Datos actualizados correctamente! ✅");
        }
        
        Usuario usuarioRefrescado = usuarioRepository.findById(usuarioActual.getId()).orElse(usuarioActual);
        model.addAttribute("usuario", usuarioRefrescado);
        
        return "usuarios/cuenta/perfil";
    }

    @GetMapping("/historial")
    public String historial(@RequestParam(defaultValue = "") String buscar,
                            @RequestParam(defaultValue = "") String tipo,
                            @RequestParam(defaultValue = "0") int page,
                            Model model) {
        Usuario usuario = obtenerUsuarioLogueado();
        
        if (usuario == null) {
            return "redirect:/login";
        }

        model.addAttribute("usuario", usuario);
        List<com.transcaribe.transcaribe.Model.Transaccion> filtradas =
                transaccionService.obtenerTransaccionesPorUsuario(usuario).stream()
                        .filter(t -> buscar.isBlank()
                                || (t.getTipo() != null && t.getTipo().toLowerCase(Locale.ROOT)
                                .contains(buscar.trim().toLowerCase(Locale.ROOT))))
                        .filter(t -> tipo.isBlank() || coincideTipo(t.getTipo(), tipo))
                        .toList();
        int totalPages = Math.max(1, (int) Math.ceil(filtradas.size() / 10.0));
        int paginaSegura = Math.min(Math.max(page, 0), totalPages - 1);
        int desde = paginaSegura * 10;
        int hasta = Math.min(desde + 10, filtradas.size());
        model.addAttribute("transacciones", filtradas.subList(desde, hasta));
        model.addAttribute("buscar", buscar);
        model.addAttribute("tipoSeleccionado", tipo);
        model.addAttribute("currentPage", paginaSegura);
        model.addAttribute("totalPages", totalPages);
        return "usuarios/cuenta/historial";
    }

    @GetMapping("/historial/{id}/recibo")
    public ResponseEntity<byte[]> descargarRecibo(@PathVariable String id) throws IOException {
        Usuario usuario = obtenerUsuarioLogueado();
        if (usuario == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        Transaccion transaccion = transaccionService.obtenerTransaccionDeUsuario(id, usuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        byte[] pdf = reciboPdfService.generarRecibo(transaccion);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("recibo-" + transaccion.getId() + ".pdf", StandardCharsets.UTF_8)
                .build());
        headers.setContentLength(pdf.length);
        headers.setCacheControl("no-store");
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

    private boolean coincideTipo(String tipoTransaccion, String filtro) {
        if (tipoTransaccion == null) {
            return false;
        }
        String tipoNormalizado = tipoTransaccion.toLowerCase(Locale.ROOT);
        return switch (filtro.toLowerCase(Locale.ROOT)) {
            case "recarga" -> tipoNormalizado.contains("recarga");
            case "pasaje" -> tipoNormalizado.contains("pasaje");
            default -> true;
        };
    }

    @GetMapping("/rutas")
    public String rutas(@RequestParam(defaultValue = "") String buscar,
                        @RequestParam(defaultValue = "0") int page,
                        Model model) {
        Usuario usuario = obtenerUsuarioLogueado();

        if (usuario == null) {
            return "redirect:/login";
        }

        model.addAttribute("usuario", usuario);
        List<PreferenciaRutaNotificacion> rutasFavoritasConPreferencia = usuario.getRutasFavoritas().stream()
                .map(usuario::obtenerPreferenciaNotificacionRuta)
                .toList();
        model.addAttribute("rutasFavoritasConPreferencia", rutasFavoritasConPreferencia);
        List<String> rutas = busService.obtenerTodasLasRutas().stream()
                .filter(r -> buscar.isBlank() || r.toLowerCase(Locale.ROOT)
                        .contains(buscar.trim().toLowerCase(Locale.ROOT)))
                .toList();
        int totalPages = Math.max(1, (int) Math.ceil(rutas.size() / 20.0));
        int paginaSegura = Math.min(Math.max(page, 0), totalPages - 1);
        int desde = paginaSegura * 20;
        int hasta = Math.min(desde + 20, rutas.size());
        model.addAttribute("rutasDisponibles", rutas.subList(desde, hasta));
        model.addAttribute("buscar", buscar);
        model.addAttribute("currentPage", paginaSegura);
        model.addAttribute("totalPages", totalPages);
        return "usuarios/cuenta/rutas";
    }

    @PostMapping("/rutas/favorito/agregar")
    public String agregarFavorito(@RequestParam String ruta, Model model) {
        Usuario usuario = obtenerUsuarioLogueado();

        if (usuario == null) {
            return "redirect:/login";
        }

        usuario.agregarRutaFavorita(ruta);
        usuarioRepository.save(usuario);

        return "redirect:/rutas";
    }

    @PostMapping("/rutas/favorito/eliminar")
    public String eliminarFavorito(@RequestParam String ruta, Model model) {
        Usuario usuario = obtenerUsuarioLogueado();

        if (usuario == null) {
            return "redirect:/login";
        }

        usuario.eliminarRutaFavorita(ruta);
        usuarioRepository.save(usuario);

        return "redirect:/rutas";
    }

    @PostMapping("/rutas/favorito/notificaciones")
    public String guardarPreferenciaNotificacion(
            @RequestParam String ruta,
            @RequestParam(defaultValue = "false") boolean notificacionesActivas,
            @RequestParam String horaDesde,
            @RequestParam String horaHasta,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        Usuario usuario = obtenerUsuarioLogueado();
        if (usuario == null) {
            return "redirect:/login";
        }
        if (!usuario.getRutasFavoritas().contains(ruta)) {
            redirectAttributes.addFlashAttribute("error", "Solo puedes configurar notificaciones para rutas favoritas.");
            return "redirect:/rutas";
        }

        try {
            LocalTime desde = LocalTime.parse(horaDesde);
            LocalTime hasta = LocalTime.parse(horaHasta);
            if (!desde.isBefore(hasta)) {
                redirectAttributes.addFlashAttribute("error",
                        "La hora inicial debe ser anterior a la hora final.");
                return "redirect:/rutas";
            }

            usuario.guardarPreferenciaNotificacionRuta(
                    new PreferenciaRutaNotificacion(ruta, notificacionesActivas, desde, hasta));
            usuarioRepository.save(usuario);
            redirectAttributes.addFlashAttribute("mensaje",
                    notificacionesActivas
                            ? "Horario de notificaciones guardado para " + ruta + "."
                            : "Notificaciones desactivadas para " + ruta + ".");
        } catch (DateTimeParseException e) {
            redirectAttributes.addFlashAttribute("error", "Selecciona un rango horario válido.");
        }
        return "redirect:/rutas";
    }
}