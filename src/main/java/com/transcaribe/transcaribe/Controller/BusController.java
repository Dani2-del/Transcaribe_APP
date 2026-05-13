package com.transcaribe.transcaribe.Controller;

import com.transcaribe.transcaribe.Model.Usuario;
import com.transcaribe.transcaribe.Repository.BusRepository;
import com.transcaribe.transcaribe.service.BusService;
import com.transcaribe.transcaribe.service.ServiceTranscaribe;
import com.transcaribe.transcaribe.service.EmailService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/api/buses")
public class BusController {

    @Autowired
    private BusService busService;

    @Autowired
    private ServiceTranscaribe service;

    @Autowired
    private BusRepository busRepository;

    @Autowired
    private EmailService emailService;

    /**
     * ENDPOINT PARA LA API (JSON)
     * Procesa el pago y envía notificación automática.
     */
    @PostMapping("/pagar/{usuarioId}/{busId}")
    @ResponseBody
    public ResponseEntity<String> pagar(
            @PathVariable String usuarioId, 
            @PathVariable String busId,
            @RequestParam String numeroTarjeta) {
        try {
            // 1. Ejecutar la lógica de pago (Costo $3900.00 definido en BusService)
            String resultado = busService.pagarPasajeConTarjetaEspecifica(usuarioId, busId, numeroTarjeta);
            
            // 2. Verificar errores de saldo o validación de tarjeta
            if (resultado.contains("Saldo insuficiente") || resultado.contains("no es válida")) {
                return ResponseEntity.badRequest().body(resultado);
            }

            // --- INTEGRACIÓN DEL CORREO DE GASTO ---
            try {
                // Buscamos el usuario en la BD para obtener nombre y correo
                Usuario usuarioDestino = service.buscarPorId(usuarioId);
                
                if (usuarioDestino != null) {
                    // Valor según tu BusService: 3900.0
                    double costoPasaje = 3900.0; 
                    
                    // Extraemos el saldo restante del string de respuesta para el correo
                    // Opcionalmente podrías llamar a un método "obtenerSaldo"
                    String saldoRestante = resultado.split("\\$")[1]; 

                    emailService.enviarNotificacionGasto(
                        usuarioDestino.getCorreo(),
                        usuarioDestino.getNombre(),
                        costoPasaje,
                        saldoRestante
                    );
                }
            } catch (Exception e) {
                System.err.println("Error al enviar el correo de ticket: " + e.getMessage());
            }
            // ---------------------------------------
            
            return ResponseEntity.ok(resultado);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al procesar el pago: " + e.getMessage());
        }
    }

    /**
     * CONFIGURACIÓN INICIAL
     * Crea el bus B-210 con las rutas de Cartagena.
     */
    @PostMapping("/setup")
    @ResponseBody
    public ResponseEntity<String> setup() {
        busService.crearBusDePrueba();
        return ResponseEntity.ok("Bus de prueba creado exitosamente con sus rutas originales");
    }

    /**
     * MÉTODO PARA LA VISTA (HTML)
     * Maneja la redirección y mensajes flash para la interfaz web.
     */
    @PostMapping("/simular-viaje")
    public String simularViaje(@RequestParam String usuarioId, 
                               @RequestParam String numeroTarjeta, 
                               RedirectAttributes redirectAttributes) {
        
        try {
            var buses = busRepository.findAll();
            if (buses.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "No hay buses configurados. Ejecuta el setup primero.");
                return "redirect:/menu";
            }

            // Tomamos el primer bus (B-210) para la simulación
            String busId = buses.get(0).getId(); 
            String resultado = busService.pagarPasajeConTarjetaEspecifica(usuarioId, busId, numeroTarjeta);

            if (resultado.contains("Cobro exitoso")) {
                // --- NOTIFICACIÓN EN SIMULACIÓN ---
                try {
                    Usuario u = service.buscarPorId(usuarioId);
                    if (u != null) {
                        String saldoRestante = resultado.split("\\$")[1];
                        emailService.enviarNotificacionGasto(
                            u.getCorreo(), 
                            u.getNombre(), 
                            3900.0, 
                            saldoRestante
                        );
                    }
                } catch (Exception ex) {
                    System.err.println("Error correo simulación: " + ex.getMessage());
                }
                // ----------------------------------
                
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