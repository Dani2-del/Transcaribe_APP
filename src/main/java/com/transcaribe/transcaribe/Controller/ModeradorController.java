package com.transcaribe.transcaribe.Controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.transcaribe.transcaribe.Model.Usuario;
import com.transcaribe.transcaribe.Repository.UsuarioRepository;

@Controller
@RequestMapping("/mod")
public class ModeradorController {

    private final UsuarioRepository usuarioRepository;


    public ModeradorController(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {


        List<Usuario> usuarios = usuarioRepository.findByRoleAndActivoTrue(Usuario.ROLE_USER);

        model.addAttribute("usuarios", usuarios);

        return "mod/dashboard"; 
    }
}