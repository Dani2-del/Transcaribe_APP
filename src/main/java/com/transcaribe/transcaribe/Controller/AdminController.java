package com.transcaribe.transcaribe.Controller;

import java.io.IOException;
import java.util.*;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.transcaribe.transcaribe.Model.Usuario;
import com.transcaribe.transcaribe.Repository.UsuarioRepository;
import com.transcaribe.transcaribe.service.ExcelReportService;
import com.transcaribe.transcaribe.service.ServiceTranscaribe;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UsuarioRepository usuarioRepository;
    private final ServiceTranscaribe serviceTranscaribe;
    private final ExcelReportService excelService;

    public AdminController(UsuarioRepository usuarioRepository, 
                           ServiceTranscaribe serviceTranscaribe, 
                           ExcelReportService excelService) {
        this.usuarioRepository = usuarioRepository;
        this.serviceTranscaribe = serviceTranscaribe;
        this.excelService = excelService;
    }

    // --- VISTA PRINCIPAL ---
    @GetMapping("/dashboard")
    public String mostrarDashboard(Model model) {
        List<Usuario> usuarios = usuarioRepository.findAll();
        model.addAttribute("usuarios", usuarios);
        return "admin/dashboard"; 
    }

    // --- MÉTODO RECARGA/COBRO (Original) ---
    @PostMapping("/charge")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> chargePassenger(@RequestParam String userId, @RequestParam double amount) {
        Map<String, Object> response = new HashMap<>();
        boolean ok = serviceTranscaribe.cobrarPasajePorAdmin(userId, amount);

        if (!ok) {
            response.put("status", "error");
            response.put("message", "Saldo insuficiente o usuario no encontrado");
            return ResponseEntity.badRequest().body(response);
        }

        Usuario actualizado = usuarioRepository.findById(userId).get();
        response.put("status", "ok");
        response.put("usuarioSaldo", actualizado.getSaldoTotal());
        // Agregamos el saldo de la primera tarjeta por si lo necesitas en el JS
        if (!actualizado.getTarjetas().isEmpty()) {
            response.put("tarjetaSaldo", actualizado.getTarjetas().get(0).getSaldo());
        }

        return ResponseEntity.ok(response);
    }

    // --- EDITAR USUARIO (Original) ---
    @PostMapping("/editUser")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> editUser(
            @RequestParam String userId,
            @RequestParam String nombre,
            @RequestParam String correo,
            @RequestParam String role,
            @RequestParam(required = false) String password) {

        Map<String, Object> response = new HashMap<>();
        try {
            Optional<Usuario> opt = usuarioRepository.findById(userId);
            if (opt.isPresent()) {
                Usuario u = opt.get();
                u.setNombre(nombre);
                u.setCorreo(correo);
                u.setRole(role);

                if (password != null && !password.trim().isEmpty()) {
                    u.setPasswordHash(password); 
                }

                usuarioRepository.save(u);
                response.put("status", "ok");
                return ResponseEntity.ok(response);
            } else {
                response.put("status", "error");
                response.put("message", "Usuario no encontrado");
                return ResponseEntity.status(404).body(response);
            }
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // --- ELIMINAR USUARIO (Original) ---
    @PostMapping("/deleteUser")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteUser(@RequestParam String userId) {
        Map<String, Object> response = new HashMap<>();
        try {
            usuarioRepository.deleteById(userId);
            response.put("status", "ok");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            return ResponseEntity.status(500).body(response);
        }
    }

    // --- NUEVO: DESCARGAR EXCEL ---
    @GetMapping("/reporte/excel")
    public ResponseEntity<InputStreamResource> descargarExcel() throws IOException {
        String filename = "Reporte_Transcaribe.xlsx";
        InputStreamResource file = new InputStreamResource(excelService.generarReporteGeneral());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(file);
    }
}