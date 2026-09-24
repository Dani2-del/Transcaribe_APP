package com.transcaribe.transcaribe.Controller;
 
import com.transcaribe.transcaribe.Model.Bus;
import com.transcaribe.transcaribe.Model.HorarioConductor;
import com.transcaribe.transcaribe.Model.Usuario;
import com.transcaribe.transcaribe.Repository.BusRepository;
import com.transcaribe.transcaribe.Repository.HorarioConductorRepository;
import com.transcaribe.transcaribe.Repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
 
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
 
@Controller
@RequestMapping("/admin/horarios")
public class HorarioController {
 
    @Autowired
    private HorarioConductorRepository horarioRepository;
 
    @Autowired
    private UsuarioRepository usuarioRepository;
 
    @Autowired
    private BusRepository busRepository;
 
    @GetMapping
    public String verHorarios(Model model) {
        List<Usuario> conductores = usuarioRepository.findByRoleAndActivoTrue(Usuario.ROLE_CONDUCTOR);
        List<Bus> buses = busRepository.findAll();
 
        Map<String, String> nombreConductor = new HashMap<>();
        for (Usuario c : conductores) {
            nombreConductor.put(c.getId(), c.getNombre() != null ? c.getNombre() : c.getCorreo());
        }
 
        // Mapa busId -> lista de rutas de ese bus (para llenar el <select> de ruta por JS)
        Map<String, List<String>> busRutasPorId = new HashMap<>();
        for (Bus b : buses) {
            busRutasPorId.put(b.getId(), b.getRutas() != null ? b.getRutas() : new ArrayList<>());
        }
 
        // Mapa conductorId -> busId asignado (para saber qué rutas mostrar al elegir conductor)
        Map<String, String> busPorConductor = new HashMap<>();
        for (Usuario c : conductores) {
            busPorConductor.put(c.getId(), c.getBusAsignado() != null ? c.getBusAsignado() : "");
        }
 
        model.addAttribute("conductores", conductores);
        model.addAttribute("buses", buses);
        model.addAttribute("horarios", horarioRepository.findAllByOrderByFechaAscHoraInicioAsc());
        model.addAttribute("nombreConductor", nombreConductor);
        model.addAttribute("busRutasPorId", busRutasPorId);
        model.addAttribute("busPorConductor", busPorConductor);
 
        return "admin/horarios";
    }

    @GetMapping("/historial")
    public String verHistorial(@RequestParam(required = false) String conductorId,
                               @RequestParam(required = false) String ruta,
                               @RequestParam(required = false) String fechaDesde,
                               @RequestParam(required = false) String fechaHasta,
                               Model model) {
        List<Usuario> conductores = usuarioRepository.findByRoleAndActivoTrue(Usuario.ROLE_CONDUCTOR);
        List<HorarioConductor> historial = horarioRepository.findAllByOrderByFechaAscHoraInicioAsc();
        LocalDate desde = parseFecha(fechaDesde);
        LocalDate hasta = parseFecha(fechaHasta);

        if (fechaDesde != null && !fechaDesde.isBlank() && desde == null) {
            model.addAttribute("error", "La fecha inicial no tiene un formato válido.");
        }
        if (fechaHasta != null && !fechaHasta.isBlank() && hasta == null) {
            model.addAttribute("error", "La fecha final no tiene un formato válido.");
        }
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            model.addAttribute("error", "La fecha inicial no puede ser posterior a la fecha final.");
        }

        String rutaFiltro = ruta == null ? "" : ruta.trim().toLowerCase();
        List<HorarioConductor> horariosFiltrados = historial.stream()
                .filter(h -> conductorId == null || conductorId.isBlank() || conductorId.equals(h.getConductorId()))
                .filter(h -> rutaFiltro.isEmpty() || (h.getRuta() != null && h.getRuta().toLowerCase().contains(rutaFiltro)))
                .filter(h -> desde == null || (h.getFecha() != null && !h.getFecha().isBefore(desde)))
                .filter(h -> hasta == null || (h.getFecha() != null && !h.getFecha().isAfter(hasta)))
                .toList();

        Map<String, String> nombreConductor = new HashMap<>();
        Map<String, String> busConductor = new HashMap<>();
        for (Usuario conductor : conductores) {
            nombreConductor.put(conductor.getId(),
                    conductor.getNombre() != null ? conductor.getNombre() : conductor.getCorreo());
            String busId = conductor.getBusAsignado();
            if (busId != null && !busId.isBlank()) {
                busRepository.findById(busId).ifPresent(bus -> busConductor.put(conductor.getId(), bus.getPlaca()));
            }
        }

        model.addAttribute("conductores", conductores);
        model.addAttribute("historial", horariosFiltrados);
        model.addAttribute("nombreConductor", nombreConductor);
        model.addAttribute("busConductor", busConductor);
        model.addAttribute("conductorIdSeleccionado", conductorId == null ? "" : conductorId);
        model.addAttribute("rutaSeleccionada", ruta == null ? "" : ruta);
        model.addAttribute("fechaDesde", fechaDesde == null ? "" : fechaDesde);
        model.addAttribute("fechaHasta", fechaHasta == null ? "" : fechaHasta);
        model.addAttribute("totalHistorial", horariosFiltrados.size());
        return "admin/historial-horarios";
    }

    private LocalDate parseFecha(String fecha) {
        if (fecha == null || fecha.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(fecha);
        } catch (java.time.format.DateTimeParseException e) {
            return null;
        }
    }
 
    @PostMapping("/crear")
    public String crearHorario(@RequestParam String conductorId,
                                @RequestParam String ruta,
                                @RequestParam String fecha,
                                @RequestParam String horaInicio,
                                @RequestParam String horaFin) {
 
        HorarioConductor horario = new HorarioConductor(
                conductorId,
                ruta,
                LocalDate.parse(fecha),
                LocalTime.parse(horaInicio),
                LocalTime.parse(horaFin)
        );
 
        horarioRepository.save(horario);
        return "redirect:/admin/horarios";
    }
 
    @PostMapping("/eliminar")
    public String eliminarHorario(@RequestParam String id) {
        horarioRepository.deleteById(id);
        return "redirect:/admin/horarios";
    }
 
    @GetMapping("/buses/nuevo")
    public String formularioNuevoBus(Model model) {
        model.addAttribute("tiposBus", List.of(Bus.TIPO_TRONCAL, Bus.TIPO_PETRONCAL, Bus.TIPO_ALIMENTADOR));
        return "admin/bus-nuevo";
    }

    @PostMapping("/buses/crear")
    public String crearBus(@RequestParam String placa,
                            @RequestParam(required = false) String rutas,
                            @RequestParam(defaultValue = "TRONCAL", required = false) String tipo,
                            RedirectAttributes redirectAttributes) {
 
        String placaLimpia = placa.trim().toUpperCase();
 
        if (busRepository.findByPlaca(placaLimpia).isPresent()) {
            redirectAttributes.addFlashAttribute("error", "Ya existe un bus registrado con la placa " + placaLimpia + " ❌");
            return "redirect:/admin/horarios";
        }
 
        String rutasTexto = rutas == null ? "" : rutas;
        List<String> listaRutas = Arrays.stream(rutasTexto.split(","))
                .map(String::trim)
                .filter(r -> !r.isEmpty())
                .collect(Collectors.toList());

        Bus nuevoBus = new Bus(placaLimpia, listaRutas, tipo);
        busRepository.save(nuevoBus);
 
        redirectAttributes.addFlashAttribute("mensaje", "Bus " + placaLimpia + " registrado correctamente ✅");
        return "redirect:/admin/horarios";
    }
 
    @PostMapping("/buses/eliminar")
    public String eliminarBus(@RequestParam String id, RedirectAttributes redirectAttributes) {
        boolean tieneConductorAsignado = usuarioRepository.findByRoleAndActivoTrue(Usuario.ROLE_CONDUCTOR)
                .stream()
                .anyMatch(c -> id.equals(c.getBusAsignado()));
 
        if (tieneConductorAsignado) {
            redirectAttributes.addFlashAttribute("error", "No se puede eliminar: hay un conductor con este bus asignado ❌");
            return "redirect:/admin/horarios";
        }
 
        busRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("mensaje", "Bus eliminado correctamente ✅");
        return "redirect:/admin/horarios";
    }
 
    @PostMapping("/buses/editar")
    public String editarBus(@RequestParam String id,
                             @RequestParam String placa,
                             @RequestParam(required = false) String rutas,
                             @RequestParam(defaultValue = "TRONCAL", required = false) String tipo,
                             RedirectAttributes redirectAttributes) {
 
        Bus bus = busRepository.findById(id).orElse(null);
        if (bus == null) {
            redirectAttributes.addFlashAttribute("error", "Ese bus no existe ❌");
            return "redirect:/admin/horarios";
        }
 
        String placaLimpia = placa.trim().toUpperCase();
 
        // Si cambió la placa, verificar que no choque con otro bus existente
        if (!placaLimpia.equals(bus.getPlaca()) && busRepository.findByPlaca(placaLimpia).isPresent()) {
            redirectAttributes.addFlashAttribute("error", "Ya existe otro bus con la placa " + placaLimpia + " ❌");
            return "redirect:/admin/horarios";
        }
 
        String rutasTexto = rutas == null ? "" : rutas;
        List<String> listaRutas = Arrays.stream(rutasTexto.split(","))
                .map(String::trim)
                .filter(r -> !r.isEmpty())
                .collect(Collectors.toList());

        bus.setPlaca(placaLimpia);
        bus.setRutas(listaRutas);
        bus.setTipo(tipo);
        busRepository.save(bus);
 
        redirectAttributes.addFlashAttribute("mensaje", "Bus " + placaLimpia + " actualizado correctamente ✅");
        return "redirect:/admin/horarios";
    }
}