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

@Service
public class ExcelReportService {

    private final UsuarioRepository usuarioRepository;
    private final TransaccionRepository transaccionRepository;

    public ExcelReportService(UsuarioRepository usuarioRepository, TransaccionRepository transaccionRepository) {
        this.usuarioRepository = usuarioRepository;
        this.transaccionRepository = transaccionRepository;
    }

    public ByteArrayInputStream generarReporteGeneral() throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            // --- 1. Estilos de encabezado ---
            CellStyle headerCellStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerCellStyle.setFont(headerFont);
            headerCellStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // --- 2. HOJA 1: RESUMEN DE USUARIOS ---
            Sheet sheetUsers = workbook.createSheet("Resumen Usuarios");
            String[] columnasUsers = {"ID", "Nombre", "Correo", "Rol"};
            
            Row headerRowUsers = sheetUsers.createRow(0);
            for (int i = 0; i < columnasUsers.length; i++) {
                Cell cell = headerRowUsers.createCell(i);
                cell.setCellValue(columnasUsers[i]);
                cell.setCellStyle(headerCellStyle);
            }

            List<Usuario> usuarios = usuarioRepository.findAll();
            int rowIdxUser = 1;
            for (Usuario u : usuarios) {
                Row row = sheetUsers.createRow(rowIdxUser++);
                row.createCell(0).setCellValue(u.getId());
                row.createCell(1).setCellValue(u.getNombre() != null ? u.getNombre() : "N/A");
                row.createCell(2).setCellValue(u.getCorreo());
                row.createCell(3).setCellValue(u.getRole());
            }
            
            // Autoajustar Hoja 1
            for (int i = 0; i < columnasUsers.length; i++) sheetUsers.autoSizeColumn(i);

            // --- 3. HOJA 2: TODAS LAS TRANSACCIONES ---
            Sheet sheetTrans = workbook.createSheet("Historial Global");
            String[] columnasTrans = {"Fecha y Hora", "ID Transacción", "Usuario (Correo)", "Nombre", "Tipo", "Monto"};

            Row headerRowTrans = sheetTrans.createRow(0);
            for (int i = 0; i < columnasTrans.length; i++) {
                Cell cell = headerRowTrans.createCell(i);
                cell.setCellValue(columnasTrans[i]);
                cell.setCellStyle(headerCellStyle);
            }

            List<Transaccion> todasLasTransacciones = transaccionRepository.findAll();
            int rowIdxTrans = 1;
            for (Transaccion t : todasLasTransacciones) {
                Row row = sheetTrans.createRow(rowIdxTrans++);
                
                // Fecha (Convertir a String para evitar errores de formato)
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

            // Autoajustar Hoja 2
            for (int i = 0; i < columnasTrans.length; i++) sheetTrans.autoSizeColumn(i);

            // --- 4. Escritura final ---
            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }
}