package com.transcaribe.transcaribe.service;

import com.transcaribe.transcaribe.Repository.TransaccionRepository;
import com.transcaribe.transcaribe.Repository.UsuarioRepository;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExcelReportServiceTest {

    @Test
    void generaUnLibroValidoConAnchosDeColumnaEstables() throws IOException {
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        TransaccionRepository transaccionRepository = mock(TransaccionRepository.class);
        when(usuarioRepository.findAll()).thenReturn(List.of());
        when(transaccionRepository.findAll()).thenReturn(List.of());

        ExcelReportService service = new ExcelReportService(usuarioRepository, transaccionRepository);

        try (var workbook = WorkbookFactory.create(
                service.generarReporte("anual", 2026, null, null))) {
            assertEquals(2, workbook.getNumberOfSheets());
            assertEquals("Resumen Usuarios", workbook.getSheetAt(0).getSheetName());
            assertEquals("Año 2026", workbook.getSheetAt(1).getSheetName());
            assertEquals("ID", workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue());
            assertEquals(18 * 256, workbook.getSheetAt(0).getColumnWidth(0));
        }
    }
}
