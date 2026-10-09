package com.transcaribe.transcaribe.service;

import com.transcaribe.transcaribe.Repository.TransaccionRepository;
import com.transcaribe.transcaribe.Repository.UsuarioRepository;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExcelReportServiceTest {

    @Test
    void generaUnLibroValidoConAnchosDeColumnaEstables() throws IOException {
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        TransaccionRepository transaccionRepository = mock(TransaccionRepository.class);
        when(usuarioRepository.streamAll()).thenReturn(Stream.empty());
        when(transaccionRepository.streamByFechaEnRango(
                LocalDate.of(2026, 1, 1).atStartOfDay(),
                LocalDate.of(2027, 1, 1).atStartOfDay())).thenReturn(Stream.empty());

        ExcelReportService service = new ExcelReportService(usuarioRepository, transaccionRepository);
        ByteArrayOutputStream report = new ByteArrayOutputStream();
        service.generarReporte("anual", 2026, null, null, report);

        try (var workbook = WorkbookFactory.create(
                new ByteArrayInputStream(report.toByteArray()))) {
            assertEquals(2, workbook.getNumberOfSheets());
            assertEquals("Resumen Usuarios", workbook.getSheetAt(0).getSheetName());
            assertEquals("Año 2026", workbook.getSheetAt(1).getSheetName());
            assertEquals("ID", workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue());
            assertEquals(18 * 256, workbook.getSheetAt(0).getColumnWidth(0));
        }
        verify(transaccionRepository).streamByFechaEnRango(
                LocalDate.of(2026, 1, 1).atStartOfDay(),
                LocalDate.of(2027, 1, 1).atStartOfDay());
    }
}
