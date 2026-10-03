package com.transcaribe.transcaribe.Controller;

import com.transcaribe.transcaribe.Model.Usuario;
import com.transcaribe.transcaribe.Repository.UsuarioRepository;
import com.transcaribe.transcaribe.service.AvisoAdministrativoService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/avisos")
public class AvisoAdministrativoController {

    private final AvisoAdministrativoService avisoService;
    private final UsuarioRepository usuarioRepository;

    public AvisoAdministrativoController(AvisoAdministrativoService avisoService,
                                         UsuarioRepository usuarioRepository) {
        this.avisoService = avisoService;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    public String mostrarPanel(Model model, Authentication authentication) {
        Usuario administrador = usuarioRepository.findByCorreo(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("No se encontró la cuenta del administrador."));
        model.addAttribute("destinatarios", avisoService.obtenerDestinatariosElegibles());
        model.addAttribute("avisosRecientes", avisoService.obtenerAvisosRecientes());
        model.addAttribute("nombreAdministrador", administrador.getNombre());
        return "admin/avisos";
    }

    @GetMapping("/historial")
    public String mostrarHistorial(Model model) {
        model.addAttribute("avisos", avisoService.obtenerHistorialAvisos());
        return "admin/avisos-historial";
    }

    @PostMapping
    public String enviarAviso(@RequestParam String asunto,
                              @RequestParam String mensaje,
                              @RequestParam String tipoDestinatarios,
                              @RequestParam(required = false) List<String> roles,
                              @RequestParam(required = false) List<String> usuarios,
                              RedirectAttributes redirectAttributes) {
        try {
            boolean paraTodos = "roles".equals(tipoDestinatarios);
            if (!paraTodos && !"seleccionados".equals(tipoDestinatarios)) {
                throw new IllegalArgumentException("Selecciona destinatarios por tipo de cuenta o de forma individual.");
            }
            AvisoAdministrativoService.ResultadoEnvio resultado =
                    avisoService.crearAviso(asunto, mensaje, paraTodos, roles, usuarios);
            String reporte = "Aviso publicado para " + resultado.totalDestinatarios()
                    + " cuenta(s). Correos enviados: " + resultado.correosEnviados() + ".";
            if (!resultado.correosFallidos().isEmpty()) {
                reporte += " Correos no enviados: " + resultado.correosFallidos().size()
                        + ". El aviso sí aparecerá dentro de la aplicación.";
            }
            redirectAttributes.addFlashAttribute("mensajeAviso", reporte);
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorAviso", exception.getMessage());
        }
        return "redirect:/admin/avisos";
    }
}
