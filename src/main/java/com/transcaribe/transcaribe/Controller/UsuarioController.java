package com.transcaribe.transcaribe.Controller;

import com.transcaribe.transcaribe.Model.Usuario;
import com.transcaribe.transcaribe.Repository.UsuarioRepository;
import com.transcaribe.transcaribe.service.ServiceTranscaribe;
import com.transcaribe.transcaribe.service.TransaccionService;
import com.transcaribe.transcaribe.service.BusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Locale;

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
}