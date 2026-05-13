package com.transcaribe.transcaribe.service;

import com.transcaribe.transcaribe.Model.Usuario;
import com.transcaribe.transcaribe.Model.Transaccion;
import com.transcaribe.transcaribe.Repository.UsuarioRepository;
import com.transcaribe.transcaribe.Repository.TransaccionRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExcelReportService {

    private final UsuarioRepository usuarioRepository;
    private final TransaccionRepository transaccionRepository;

    public ExcelReportService(UsuarioRepository usuarioRepository, TransaccionRepository transaccionRepository) {
        this.usuarioRepository = usuarioRepository;
        this.transaccionRepository = transaccionRepository;
    }

    /**
     * Genera el reporte filtrando por tipo:
     *  - "todo"     → todas las transacciones
     *  - "anual"    → transacciones del año indicado
     *  - "mensual"  → transacciones del año + mes indicados
     *  - "diario"   → transacciones del año + mes + día indicados
     */
    public ByteArrayInputStream generarReporte(String tipo, Integer anio, Integer mes, Integer dia) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // --- Estilos ---
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // --- Hoja 1: Resumen Usuarios ---
            Sheet sheetUsers = workbook.createSheet("Resumen Usuarios");
            String[] colUsers = {"ID", "Nombre", "Correo", "Rol"};
            Row headerUsers = sheetUsers.createRow(0);
            for (int i = 0; i < colUsers.length; i++) {
                Cell cell = headerUsers.createCell(i);
                cell.setCellValue(colUsers[i]);
                cell.setCellStyle(headerStyle);
            }
            List<Usuario> usuarios = usuarioRepository.findAll();
            int rowU = 1;
            for (Usuario u : usuarios) {
                Row row = sheetUsers.createRow(rowU++);
                row.createCell(0).setCellValue(u.getId());
                row.createCell(1).setCellValue(u.getNombre() != null ? u.getNombre() : "N/A");
                row.createCell(2).setCellValue(u.getCorreo());
                row.createCell(3).setCellValue(u.getRole());
            }
            for (int i = 0; i < colUsers.length; i++) sheetUsers.autoSizeColumn(i);

            // --- Hoja 2: Transacciones filtradas ---
            // Título dinámico según filtro
            String nombreHoja = buildNombreHoja(tipo, anio, mes, dia);
            Sheet sheetTrans = workbook.createSheet(nombreHoja);
            String[] colTrans = {"Fecha y Hora", "ID Transacción", "Usuario (Correo)", "Nombre", "Tipo", "Monto"};
            Row headerTrans = sheetTrans.createRow(0);
            for (int i = 0; i < colTrans.length; i++) {
                Cell cell = headerTrans.createCell(i);
                cell.setCellValue(colTrans[i]);
                cell.setCellStyle(headerStyle);
            }

            // Filtrar transacciones
            List<Transaccion> transacciones = transaccionRepository.findAll().stream()
                .filter(t -> {
                    if (t.getFecha() == null) return false;
                    if ("todo".equals(tipo)) return true;
                    if (anio != null && t.getFecha().getYear() != anio) return false;
                    if (("mensual".equals(tipo) || "diario".equals(tipo)) && mes != null
                            && t.getFecha().getMonthValue() != mes) return false;
                    if ("diario".equals(tipo) && dia != null
                            && t.getFecha().getDayOfMonth() != dia) return false;
                    return true;
                })
                .collect(Collectors.toList());

            int rowT = 1;
            for (Transaccion t : transacciones) {
                Row row = sheetTrans.createRow(rowT++);
                row.createCell(0).setCellValue(t.getFecha() != null ? t.getFecha().toString() : "Sin fecha");
                row.createCell(1).setCellValue(t.getId());
                if (t.getUsuario() != null) {
                    row.createCell(2).setCellValue(t.getUsuario().getCorreo());
                    row.createCell(3).setCellValue(t.getUsuario().getNombre());
                } else {
                    row.createCell(2).setCellValue("N/A");
                    row.createCell(3).setCellValue("N/A");
                }
                row.createCell(4).setCellValue(t.getTipo());
                row.createCell(5).setCellValue(t.getMonto());
            }
            for (int i = 0; i < colTrans.length; i++) sheetTrans.autoSizeColumn(i);

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    // Mantener compatibilidad con el método anterior
    public ByteArrayInputStream generarReporteGeneral() throws IOException {
        return generarReporte("todo", null, null, null);
    }

    private String buildNombreHoja(String tipo, Integer anio, Integer mes, Integer dia) {
        switch (tipo) {
            case "anual":   return "Año " + anio;
            case "mensual": return "Mes " + mes + "-" + anio;
            case "diario":  return "Día " + dia + "-" + mes + "-" + anio;
            default:        return "Historial Global";
        }
    }
}