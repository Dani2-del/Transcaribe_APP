package com.transcaribe.transcaribe.Controller;

import com.transcaribe.transcaribe.service.ServiceTranscaribe;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {

    @Autowired
    private ServiceTranscaribe service;

    @GetMapping("/login")
    public String login() {
        return "usuarios/auth/login"; 
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/registro")
    public String registro() {
        return "usuarios/auth/registro";
    }

    @PostMapping("/registro")
    public String registrar(@RequestParam("username") String nombre,
                            @RequestParam String correo,
                            @RequestParam("password") String contrasena,
                            @RequestParam String numeroTarjeta,
                            Model model) {
        boolean ok = service.registrar(nombre, correo, contrasena, numeroTarjeta);
        if (ok) {
            return "redirect:/verificar-otp?correo=" + correo;
        }
        model.addAttribute("error", "Error al registrar. El correo ya existe o la tarjeta es inválida.");
        return "usuarios/auth/registro";
    }

    @GetMapping("/verificar-otp")
    public String mostrarVerificacion(@RequestParam String correo, Model model) {
        model.addAttribute("correo", correo);
        return "usuarios/auth/verificar-codigo";
    }

    @PostMapping("/verificar-otp")
    public String verificarOtp(@RequestParam String correo, @RequestParam String codigo, Model model) {
        boolean validado = service.verificarCodigo(correo, codigo);
        if (validado) {
            model.addAttribute("mensaje", "¡Cuenta activada! Ya puedes iniciar sesión ✅");
            return "usuarios/auth/login";
        }
        model.addAttribute("error", "El código ingresado es incorrecto.");
        model.addAttribute("correo", correo);
        return "usuarios/auth/verificar-codigo";
    }
}