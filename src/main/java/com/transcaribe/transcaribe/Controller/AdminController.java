package com.transcaribe.transcaribe.Controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.web.bind.annotation.*;

import com.transcaribe.transcaribe.Model.Usuario;
import com.transcaribe.transcaribe.Model.Transaccion;
import com.transcaribe.transcaribe.Repository.BusRepository;
import com.transcaribe.transcaribe.Repository.UsuarioRepository;
import com.transcaribe.transcaribe.service.ExcelReportService;
import com.transcaribe.transcaribe.service.ServiceTranscaribe;
import org.springframework.http.ContentDisposition;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UsuarioRepository usuarioRepository;
    private final ServiceTranscaribe serviceTranscaribe;
    private final ExcelReportService excelService;
    private final BusRepository busRepository;
    private final MongoTemplate mongoTemplate;

    public AdminController(UsuarioRepository usuarioRepository,
                           ServiceTranscaribe serviceTranscaribe,
                           ExcelReportService excelService,
                           BusRepository busRepository,
                           MongoTemplate mongoTemplate) {
        this.usuarioRepository = usuarioRepository;
        this.serviceTranscaribe = serviceTranscaribe;
        this.excelService = excelService;
        this.busRepository = busRepository;
        this.mongoTemplate = mongoTemplate;
    }

    private static final int TAMANO_PAGINA = 20;

    @GetMapping("/dashboard")
    public String mostrarDashboard(Model model) {
        model.addAttribute("totalUsuarios", usuarioRepository.count());
        return "admin/inicio";
    }

    @GetMapping("/usuarios")
    public String mostrarUsuarios(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String search,
            Model model) {

        PageRequest pageable = PageRequest.of(page, TAMANO_PAGINA, Sort.by("nombre").ascending());

        Page<Usuario> resultado = (search != null && !search.isBlank())
                ? usuarioRepository.findByNombreContainingIgnoreCaseOrCorreoContainingIgnoreCase(
                search.trim(), search.trim(), pageable)
                : usuarioRepository.findAll(pageable);

        model.addAttribute("usuarios", resultado.getContent());
        model.addAttribute("currentPage", resultado.getNumber());
        model.addAttribute("totalPages", resultado.getTotalPages());
        model.addAttribute("totalUsuarios", resultado.getTotalElements());
        model.addAttribute("search", search != null ? search : "");
        model.addAttribute("buses", busRepository.findAll());
        return "admin/dashboard";
    }

    @GetMapping("/transacciones")
    public String mostrarTransacciones(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "") String usuario,
            @RequestParam(defaultValue = "") String tipo,
            @RequestParam(required = false) Double montoMin,
            @RequestParam(required = false) Double montoMax,
            Model model) {
        int paginaSolicitada = Math.max(page, 0);
        Sort orden = Sort.by(Sort.Direction.DESC, "fecha");
        PageRequest pageable = PageRequest.of(paginaSolicitada, TAMANO_PAGINA, orden);

        String usuarioBuscado = usuario.trim();
        String tipoBuscado = tipo.trim().toLowerCase(Locale.ROOT);
        boolean rangoInvalido = (montoMin != null && montoMin < 0)
                || (montoMax != null && montoMax < 0)
                || (montoMin != null && montoMax != null && montoMin > montoMax);
        boolean tipoInvalido = !List.of("", "recarga", "pasaje", "otros").contains(tipoBuscado);
        List<Transaccion> contenido = List.of();
        long total = 0;

        if (!rangoInvalido && !tipoInvalido) {
            Query filtro = construirFiltroTransacciones(usuarioBuscado, tipoBuscado, montoMin, montoMax);
            total = mongoTemplate.count(filtro, Transaccion.class);
            int totalPaginas = (int) Math.ceil((double) total / TAMANO_PAGINA);
            if (totalPaginas > 0 && paginaSolicitada >= totalPaginas) {
                paginaSolicitada = totalPaginas - 1;
            }
            pageable = PageRequest.of(paginaSolicitada, TAMANO_PAGINA, orden);
            contenido = mongoTemplate.find(filtro.with(pageable), Transaccion.class);
        }

        Page<Transaccion> resultado = new PageImpl<>(contenido, pageable, total);
        model.addAttribute("transacciones", resultado.getContent());
        model.addAttribute("currentPage", resultado.getNumber());
        model.addAttribute("totalPages", resultado.getTotalPages());
        model.addAttribute("totalTransacciones", resultado.getTotalElements());
        model.addAttribute("usuarioBuscado", usuario);
        model.addAttribute("tipoBuscado", tipoBuscado);
        model.addAttribute("montoMin", montoMin);
        model.addAttribute("montoMax", montoMax);
        model.addAttribute("errorFiltro", rangoInvalido
                ? "Verifica los montos: deben ser positivos y el mínimo no puede superar el máximo."
                : tipoInvalido
                        ? "El tipo de transacción seleccionado no es válido."
                        : null);
        return "admin/transacciones";
    }

    private Query construirFiltroTransacciones(
            String usuario, String tipo, Double montoMin, Double montoMax) {
        Query filtro = new Query();
        if (!usuario.isBlank()) {
            List<Usuario> usuarios = usuarioRepository
                    .findByNombreContainingIgnoreCaseOrCorreoContainingIgnoreCase(usuario, usuario)
                    .stream()
                    .filter(resultado -> resultado.getId() != null)
                    .toList();
            filtro.addCriteria(Criteria.where("usuario").in(usuarios));
        }
        switch (tipo) {
            case "recarga" -> filtro.addCriteria(Criteria.where("tipo").regex("recarga", "i"));
            case "pasaje" -> filtro.addCriteria(Criteria.where("tipo").regex("pasaje", "i"));
            case "otros" -> filtro.addCriteria(new Criteria().andOperator(
                    Criteria.where("tipo").not().regex("recarga", "i"),
                    Criteria.where("tipo").not().regex("pasaje", "i")));
            default -> { }
        }
        if (montoMin != null || montoMax != null) {
            Criteria monto = Criteria.where("monto");
            if (montoMin != null) monto.gte(montoMin);
            if (montoMax != null) monto.lte(montoMax);
            filtro.addCriteria(monto);
        }
        return filtro;
    }

    @GetMapping("/reporte/excel")
    public ResponseEntity<InputStreamResource> descargarExcel(
            @RequestParam(defaultValue = "todo") String tipo,
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer dia,
            @RequestParam(required = false) String nombreReporte) throws IOException {

        String nombrePredeterminado = switch (tipo) {
            case "anual"   -> "Reporte_" + anio + ".xlsx";
            case "mensual" -> "Reporte_" + anio + "-" + String.format("%02d", mes) + ".xlsx";
            case "diario"  -> "Reporte_" + anio + "-" + String.format("%02d", mes) + "-" + String.format("%02d", dia) + ".xlsx";
            default        -> "Reporte_Transcaribe.xlsx";
        };
        String nombreArchivo = nombreReporte == null || nombreReporte.isBlank()
                ? nombrePredeterminado
                : nombreReporte.trim()
                        .replaceAll("[\\p{Cntrl}<>:\"/\\\\|?*]", "_")
                        .replaceAll("[. ]+$", "");
        if (nombreArchivo.length() > 100) {
            nombreArchivo = nombreArchivo.substring(0, 100).replaceAll("[. ]+$", "");
        }
        if (nombreArchivo.isBlank()) {
            nombreArchivo = "Reporte_Transcaribe";
        }
        if (!nombreArchivo.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            nombreArchivo += ".xlsx";
        }

        InputStreamResource file = new InputStreamResource(
                excelService.generarReporte(tipo, anio, mes, dia)
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(nombreArchivo, StandardCharsets.UTF_8)
                                .build().toString())
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(file);
    }

    @PostMapping("/charge")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> chargePassenger(
            @RequestParam String userId, @RequestParam double amount) {
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
        if (!actualizado.getTarjetas().isEmpty())
            response.put("tarjetaSaldo", actualizado.getTarjetas().get(0).getSaldo());
        return ResponseEntity.ok(response);
    }

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
                if (password != null && !password.trim().isEmpty()) u.setPasswordHash(password);

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
}