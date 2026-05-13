package com.transcaribe.transcaribe.Controller;

import com.transcaribe.transcaribe.Model.Usuario;
import com.transcaribe.transcaribe.Repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ChooseViewController {

    private final UsuarioRepository usuarioRepository;

    public ChooseViewController(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/choose-view")
    public String chooseView(Authentication authentication, Model model) {
        if (authentication == null) {
            return "redirect:/login";
        }

        String correo = authentication.getName();
        Usuario usuario = usuarioRepository.findByCorreo(correo).orElse(null);

        if (usuario == null) {
            return "redirect:/login";
        }

        // Si es solo ROLE_USER, va directo al menú sin pasar por choose-view
        if (Usuario.ROLE_USER.equals(usuario.getRole())) {
            return "redirect:/menu";
        }

        // Si es ADMIN o MODERADOR, mostramos la pantalla de selección
        model.addAttribute("nombre", usuario.getNombre() != null ? usuario.getNombre() : usuario.getCorreo());
        model.addAttribute("usuario", usuario);
        return "choose-view";
    }
}
