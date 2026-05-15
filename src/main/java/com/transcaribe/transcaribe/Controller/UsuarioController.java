package com.transcaribe.transcaribe.Controller;

import com.transcaribe.transcaribe.Model.Usuario;
import com.transcaribe.transcaribe.Repository.UsuarioRepository;
import com.transcaribe.transcaribe.service.ServiceTranscaribe;
import com.transcaribe.transcaribe.service.TransaccionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class UsuarioController {

    @Autowired
    private ServiceTranscaribe service;
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private TransaccionService transaccionService;

    /**
     * Obtiene el objeto Usuario completo desde la base de datos 
     * basado en el correo de la sesión actual de Spring Security.
     */
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

        // Si el usuario no ha completado el registro OTP, lo devolvemos
        if (!usuario.isVerificado()) {
            return "redirect:/verificar-otp?correo=" + usuario.getCorreo();
        }

        // Notificación opcional de inicio de sesión
        service.procesarNotificacionLogin(usuario.getCorreo());
        
        model.addAttribute("usuario", usuario);
        return "menu";
    }

    @GetMapping("/perfil")
    public String perfil(Model model) {
        Usuario usuario = obtenerUsuarioLogueado();
        
        if (usuario == null) {
            return "redirect:/login";
        }
        
        model.addAttribute("usuario", usuario);
        return "perfil";
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

        // El ServiceTranscaribe ya contiene la lógica que quitó al Moderador.
        // Solo permitirá el cambio si es ADMIN o si el ID es del mismo usuario.
        boolean ok = service.editarCredenciales(usuarioActual.getId(), nombre, correo, password, usuarioActual);
        
        if (!ok) {
            model.addAttribute("error", "No se pudo actualizar el perfil. Es posible que el correo ya esté en uso. ❌");
        } else {
            model.addAttribute("mensaje", "¡Datos actualizados correctamente! ✅");
        }
        
        // Refrescamos los datos del usuario para que la vista muestre los cambios
        Usuario usuarioRefrescado = usuarioRepository.findById(usuarioActual.getId()).orElse(usuarioActual);
        model.addAttribute("usuario", usuarioRefrescado);
        
        return "perfil";
    }

    @GetMapping("/historial")
    public String historial(Model model) {
        Usuario usuario = obtenerUsuarioLogueado();
        
        if (usuario == null) {
            return "redirect:/login";
        }

        model.addAttribute("usuario", usuario);
        model.addAttribute("transacciones", transaccionService.obtenerTransaccionesPorUsuario(usuario));
        return "historial";
    }
}