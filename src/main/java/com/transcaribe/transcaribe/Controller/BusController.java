package com.transcaribe.transcaribe.Controller;

import com.transcaribe.transcaribe.Repository.BusRepository;
import com.transcaribe.transcaribe.service.BusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller; // Cambiamos a Controller para manejar vistas y API
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller // Usamos Controller general para permitir tanto redirecciones como ResponseBody
@RequestMapping("/api/buses")
public class BusController {

    @Autowired
    private BusService busService;

    @Autowired
    private BusRepository busRepository;

    /**
     * ENDPOINT PARA LA API (JSON)
     * Usado si haces peticiones desde JavaScript/Postman
     */
    @PostMapping("/pagar/{usuarioId}/{busId}")
    @ResponseBody // Esto permite que devuelva texto/JSON en lugar de buscar un HTML
    public ResponseEntity<String> pagar(
            @PathVariable String usuarioId, 
            @PathVariable String busId,
            @RequestParam String numeroTarjeta) {
        try {
            String resultado = busService.pagarPasajeConTarjetaEspecifica(usuarioId, busId, numeroTarjeta);
            
            if (resultado.contains("Saldo insuficiente") || resultado.contains("no es válida")) {
                return ResponseEntity.badRequest().body(resultado);
            }
            
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al procesar el pago: " + e.getMessage());
        }
    }

    /**
     * CONFIGURACIÓN INICIAL
     */
    @PostMapping("/setup")
    @ResponseBody
    public ResponseEntity<String> setup() {
        busService.crearBusDePrueba();
        return ResponseEntity.ok("Bus de prueba creado exitosamente con sus rutas originales");
    }

    /**
     * MÉTODO PARA LA VISTA (HTML)
     * Este es el que tenías en el controlador anterior para el botón de "Simular Viaje"
     */
    @PostMapping("/simular-viaje")
    public String simularViaje(@RequestParam String usuarioId, 
                               @RequestParam String numeroTarjeta, 
                               RedirectAttributes redirectAttributes) {
        
        try {
            // Buscamos el primer bus disponible para la simulación
            var buses = busRepository.findAll();
            if (buses.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "No hay buses configurados. Ejecuta el setup primero.");
                return "redirect:/menu";
            }

            String busId = buses.get(0).getId(); 
            String resultado = busService.pagarPasajeConTarjetaEspecifica(usuarioId, busId, numeroTarjeta);

            if (resultado.contains("confirmado")) {
                redirectAttributes.addFlashAttribute("mensaje", resultado + " ✅");
            } else {
                redirectAttributes.addFlashAttribute("error", resultado + " ❌");
            }

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error en la simulación: " + e.getMessage());
        }

        return "redirect:/menu";
    }
}