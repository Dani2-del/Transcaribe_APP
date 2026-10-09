package com.transcaribe.transcaribe.service;

import com.transcaribe.transcaribe.Model.Usuario;
import com.transcaribe.transcaribe.Model.Transaccion;
import com.transcaribe.transcaribe.Repository.UsuarioRepository;
import com.transcaribe.transcaribe.Repository.TransaccionRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.stream.Stream;

@Service
public class ExcelReportService {

    private final UsuarioRepository usuarioRepository;
    private final TransaccionRepository transaccionRepository;

    public ExcelReportService(UsuarioRepository usuarioRepository, TransaccionRepository transaccionRepository) {
        this.usuarioRepository = usuarioRepository;
        this.transaccionRepository = transaccionRepository;
    }

    public void generarReporte(String tipo, Integer anio, Integer mes, Integer dia, OutputStream out) throws IOException {
        SXSSFWorkbook workbook = new SXSSFWorkbook(100);
        workbook.setCompressTempFiles(true);
        try (workbook) {

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
            try (Stream<Usuario> usuarios = usuarioRepository.streamAll()) {
                Iterator<Usuario> iterator = usuarios.iterator();
                int rowU = 1;
                while (iterator.hasNext()) {
                    Usuario u = iterator.next();
                    Row row = sheetUsers.createRow(rowU++);
                    row.createCell(0).setCellValue(u.getId());
                    row.createCell(1).setCellValue(u.getNombre() != null ? u.getNombre() : "N/A");
                    row.createCell(2).setCellValue(u.getCorreo());
                    row.createCell(3).setCellValue(u.getRole());
                }
            }
            setColumnWidths(sheetUsers, 18, 28, 36, 20);

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

            DateRange rango = buildRango(tipo, anio, mes, dia);
            try (Stream<Transaccion> transacciones = rango == null
                    ? transaccionRepository.streamAll()
                    : transaccionRepository.streamByFechaEnRango(rango.inicio(), rango.fin())) {
                Iterator<Transaccion> iterator = transacciones.iterator();
                int rowT = 1;
                while (iterator.hasNext()) {
                    Transaccion t = iterator.next();
                    if (!coincideConFiltro(t, tipo, anio, mes, dia)) {
                        continue;
                    }
                    Row row = sheetTrans.createRow(rowT++);
                    row.createCell(0).setCellValue(t.getFecha().toString());
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
            }
            setColumnWidths(sheetTrans, 22, 26, 36, 28, 18, 18);

            workbook.write(out);
        } finally {
            workbook.dispose();
        }
    }

    private boolean coincideConFiltro(Transaccion transaccion, String tipo, Integer anio, Integer mes, Integer dia) {
        if (transaccion.getFecha() == null) return false;
        if ("todo".equals(tipo)) return true;
        if (anio != null && transaccion.getFecha().getYear() != anio) return false;
        if (("mensual".equals(tipo) || "diario".equals(tipo)) && mes != null
                && transaccion.getFecha().getMonthValue() != mes) return false;
        if ("diario".equals(tipo) && dia != null
                && transaccion.getFecha().getDayOfMonth() != dia) return false;
        return true;
    }

    private DateRange buildRango(String tipo, Integer anio, Integer mes, Integer dia) {
        if ("todo".equals(tipo) || anio == null) return null;
        try {
            LocalDate inicio;
            if (("mensual".equals(tipo) || "diario".equals(tipo)) && mes != null) {
                inicio = LocalDate.of(anio, mes, 1);
                if ("diario".equals(tipo) && dia != null) {
                    inicio = LocalDate.of(anio, mes, dia);
                    return new DateRange(inicio.atStartOfDay(), inicio.plusDays(1).atStartOfDay());
                }
                return new DateRange(inicio.atStartOfDay(), inicio.plusMonths(1).atStartOfDay());
            }
            inicio = LocalDate.of(anio, 1, 1);
            return new DateRange(inicio.atStartOfDay(), inicio.plusYears(1).atStartOfDay());
        } catch (DateTimeException e) {
            return null;
        }
    }

    private record DateRange(LocalDateTime inicio, LocalDateTime fin) {}

    private String buildNombreHoja(String tipo, Integer anio, Integer mes, Integer dia) {
        switch (tipo) {
            case "anual":   return "Año " + anio;
            case "mensual": return "Mes " + mes + "-" + anio;
            case "diario":  return "Día " + dia + "-" + mes + "-" + anio;
            default:        return "Historial Global";
        }
    }

    private void setColumnWidths(Sheet sheet, int... widthsInCharacters) {
        for (int i = 0; i < widthsInCharacters.length; i++) {
            sheet.setColumnWidth(i, widthsInCharacters[i] * 256);
        }
    }
}