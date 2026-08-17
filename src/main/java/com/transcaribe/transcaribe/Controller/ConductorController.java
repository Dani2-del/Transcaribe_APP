package com.transcaribe.transcaribe.Controller;

import com.transcaribe.transcaribe.Model.Bus;
import com.transcaribe.transcaribe.Model.HorarioConductor;
import com.transcaribe.transcaribe.Model.Usuario;
import com.transcaribe.transcaribe.Repository.BusRepository;
import com.transcaribe.transcaribe.Repository.HorarioConductorRepository;
import com.transcaribe.transcaribe.Repository.UsuarioRepository;
import com.transcaribe.transcaribe.service.RutaNotificacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class ConductorController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private BusRepository busRepository;

    @Autowired
    private RutaNotificacionService rutaNotificacionService;

    @Autowired
    private HorarioConductorRepository horarioRepository;

    private Usuario obtenerUsuarioLogueado() {
        String correo = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByCorreo(correo).orElse(null);
    }

    @GetMapping("/choose-view")
    public String chooseView(Model model) {
        Usuario usuario = obtenerUsuarioLogueado();

        if (usuario == null) {
            return "redirect:/login";
        }

        boolean esAdmin = Usuario.ROLE_ADMIN.equals(usuario.getRole());
        boolean esConductor = Usuario.ROLE_CONDUCTOR.equals(usuario.getRole());

        // Un usuario normal no tiene nada que elegir, va directo a su menú
        if (!esAdmin && !esConductor) {
            return "redirect:/menu";
        }

        model.addAttribute("nombre", usuario.getNombre());
        model.addAttribute("usuario", usuario);
        model.addAttribute("esAdmin", esAdmin);
        model.addAttribute("esConductor", esConductor);
        return "choose-view";
    }

    @GetMapping("/conductor/panel")
    public String panel(@RequestParam(required = false) String mensaje,
                         @RequestParam(required = false) String error,
                         @RequestParam(required = false) Integer notificados,
                         Model model) {
        Usuario conductor = obtenerUsuarioLogueado();

        if (conductor == null) {
            return "redirect:/login";
        }

        model.addAttribute("usuario", conductor);

        Bus bus = null;
        if (conductor.getBusAsignado() != null) {
            bus = busRepository.findById(conductor.getBusAsignado()).orElse(null);
        }
        model.addAttribute("bus", bus);

        model.addAttribute("horarios",
                horarioRepository.findByConductorIdOrderByFechaAscHoraInicioAsc(conductor.getId()));

        if ("ok".equals(mensaje)) {
            model.addAttribute("mensaje", "¡Ruta iniciada! Se notificó a " + notificados + " usuario(s).");
        }
        if ("fin".equals(mensaje)) {
            model.addAttribute("mensaje", "Ruta finalizada correctamente.");
        }
        if ("sinbus".equals(error)) {
            model.addAttribute("error", "No tienes un bus asignado. Contacta al administrador.");
        }
        if ("horarioinvalido".equals(error)) {
            model.addAttribute("error", "Ese horario no existe o no te pertenece.");
        }
        if ("estadoinvalido".equals(error)) {
            model.addAttribute("error", "Esa ruta no está en un estado válido para esa acción.");
        }

        return "conductor/panel";
    }

    @PostMapping("/conductor/iniciar-ruta")
    public String iniciarRuta(@RequestParam String horarioId) {
        Usuario conductor = obtenerUsuarioLogueado();

        if (conductor == null) {
            return "redirect:/login";
        }

        if (conductor.getBusAsignado() == null) {
            return "redirect:/conductor/panel?error=sinbus";
        }

        HorarioConductor horario = horarioRepository.findByIdAndConductorId(horarioId, conductor.getId()).orElse(null);

        if (horario == null) {
            return "redirect:/conductor/panel?error=horarioinvalido";
        }

        if (!HorarioConductor.ESTADO_PENDIENTE.equals(horario.getEstado())) {
            return "redirect:/conductor/panel?error=estadoinvalido";
        }

        Bus bus = busRepository.findById(conductor.getBusAsignado()).orElse(null);
        if (bus == null) {
            return "redirect:/conductor/panel?error=sinbus";
        }

        horario.setEstado(HorarioConductor.ESTADO_EN_CURSO);
        horarioRepository.save(horario);

        int notificados = rutaNotificacionService.notificarInicioRuta(horario.getRuta(), bus.getPlaca());

        return "redirect:/conductor/panel?mensaje=ok&notificados=" + notificados;
    }

    @PostMapping("/conductor/terminar-ruta")
    public String terminarRuta(@RequestParam String horarioId) {
        Usuario conductor = obtenerUsuarioLogueado();

        if (conductor == null) {
            return "redirect:/login";
        }

        HorarioConductor horario = horarioRepository.findByIdAndConductorId(horarioId, conductor.getId()).orElse(null);

        if (horario == null) {
            return "redirect:/conductor/panel?error=horarioinvalido";
        }

        if (!HorarioConductor.ESTADO_EN_CURSO.equals(horario.getEstado())) {
            return "redirect:/conductor/panel?error=estadoinvalido";
        }

        horarioRepository.deleteById(horario.getId());

        return "redirect:/conductor/panel?mensaje=fin";
    }
}