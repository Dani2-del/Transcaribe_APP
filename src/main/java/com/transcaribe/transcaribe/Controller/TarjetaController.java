package com.transcaribe.transcaribe.Controller;

import com.transcaribe.transcaribe.Model.Tarjeta;
import com.transcaribe.transcaribe.Model.Usuario;
import com.transcaribe.transcaribe.Repository.UsuarioRepository;
import com.transcaribe.transcaribe.service.ServiceTranscaribe;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.HashMap;
import java.util.Map;

@Controller
public class TarjetaController {

    @Autowired
    private ServiceTranscaribe service;
    @Autowired
    private UsuarioRepository usuarioRepository;

    private Usuario obtenerUsuarioLogueado() {
        String correo = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByCorreo(correo).orElse(null);
    }

    @GetMapping("/saldo")
    public String saldo(Model model) {
        Usuario usuario = obtenerUsuarioLogueado();
        model.addAttribute("usuario", usuario);
        model.addAttribute("tarjetas", usuario.getTarjetas());
        return "saldo";
    }

    @GetMapping("/recarga")
    public String recarga(Model model) {
        Usuario usuario = obtenerUsuarioLogueado();
        model.addAttribute("usuario", usuario);
        model.addAttribute("tarjetas", usuario.getTarjetas());
        return "recarga";
    }

    @PostMapping("/recargar")
    public String recargar(@RequestParam double monto, @RequestParam String numeroTarjeta, Model model) {
        Usuario usuario = obtenerUsuarioLogueado();
        boolean ok = service.recargarEnTarjeta(usuario, numeroTarjeta, monto);
        if (!ok) {
            model.addAttribute("error", "Error en la recarga. Verifique los datos.");
            model.addAttribute("tarjetas", usuario.getTarjetas());
            return "recarga";
        }
        return "Recarga-exitosa";
    }

    @GetMapping("/tarjetas")
    public String tarjetas(Model model) {
        Usuario usuario = obtenerUsuarioLogueado();
        model.addAttribute("usuario", usuario);
        model.addAttribute("tarjetas", usuario.getTarjetas());
        return "tarjetas";
    }

    @PostMapping("/tarjetas/agregar")
    public String agregarTarjeta(@RequestParam String nuevoNumero, RedirectAttributes redirectAttributes) {
        Usuario usuario = obtenerUsuarioLogueado();
        String resultado = service.agregarNuevaTarjetaConValidacion(usuario.getId(), nuevoNumero);
        if (resultado.equals("OK")) {
            redirectAttributes.addFlashAttribute("mensaje", "Tarjeta añadida correctamente ✅");
        } else {
            redirectAttributes.addFlashAttribute("error", resultado + " ❌");
        }
        return "redirect:/tarjetas";
    }

    @PostMapping("/tarjetas/eliminar")
    public String eliminarTarjeta(@RequestParam String numeroTarjeta, RedirectAttributes redirectAttributes) {
        Usuario usuario = obtenerUsuarioLogueado();
        boolean eliminado = service.eliminarTarjetaDeUsuario(usuario.getId(), numeroTarjeta);
        if (eliminado) redirectAttributes.addFlashAttribute("mensaje", "Tarjeta eliminada ✅");
        else redirectAttributes.addFlashAttribute("error", "No se pudo eliminar ❌");
        return "redirect:/tarjetas";
    }
}