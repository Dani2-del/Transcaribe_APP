package com.transcaribe.transcaribe.Controller;

import com.transcaribe.transcaribe.Model.Usuario;
import com.transcaribe.transcaribe.Repository.UsuarioRepository;
import com.transcaribe.transcaribe.service.AvisoAdministrativoService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class AvisoUsuarioController {

    private final AvisoAdministrativoService avisoService;
    private final UsuarioRepository usuarioRepository;

    public AvisoUsuarioController(AvisoAdministrativoService avisoService,
                                  UsuarioRepository usuarioRepository) {
        this.avisoService = avisoService;
        this.usuarioRepository = usuarioRepository;
    }

    @PostMapping("/avisos/{avisoId}/cerrar")
    public String cerrarAviso(@PathVariable String avisoId,
                              @RequestParam(defaultValue = "/menu") String volverA,
                              Authentication authentication) {
        Usuario usuario = usuarioRepository.findByCorreo(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        try {
            avisoService.descartarParaUsuario(avisoId, usuario.getId());
        } catch (SecurityException exception) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, exception.getMessage());
        }
        return "redirect:" + rutaPermitida(volverA);
    }

    private String rutaPermitida(String ruta) {
        if ("/menu".equals(ruta) || "/choose-view".equals(ruta)
                || "/admin/dashboard".equals(ruta) || "/conductor/panel".equals(ruta)) {
            return ruta;
        }
        return "/menu";
    }
}
