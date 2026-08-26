package com.cmhk.business.module.admin;

import com.cmhk.business.module.admin.service.TabularFileReader;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class TabularFileReaderTests {
    @Test void readsUtf8CsvAndQuotedComma() {
        String csv = "ICCID,批次,备注\n8986000000000000001,B01,正常\n8986000000000000002,B01,\"含,逗号\"\n";
        var file = new MockMultipartFile("file", "cards.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));
        var rows = new TabularFileReader().read(file);
        assertEquals(2, rows.size());
        assertEquals("8986000000000000001", rows.getFirst().get("ICCID"));
        assertEquals("含,逗号", rows.get(1).get("备注"));
    }

    @Test void readsXlsxWithIccidColumn() throws Exception {
        byte[] content;
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("ICCID");
            sheet.createRow(0).createCell(0).setCellValue("ICCID");
            sheet.createRow(1).createCell(0).setCellValue("8986000000000000003");
            workbook.write(output); content = output.toByteArray();
        }
        var file = new MockMultipartFile("file", "cards.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", content);
        var rows = new TabularFileReader().read(file);
        assertEquals(1, rows.size());
        assertEquals("8986000000000000003", rows.getFirst().get("ICCID"));
    }

    @Test void readsPlainTextIccidListWithXlsxExtension() {
        String content = "89852122604294476221\r\n89852122604294476544\r\n";
        var file = new MockMultipartFile("file", "ICCID.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", content.getBytes(StandardCharsets.UTF_8));
        var rows = new TabularFileReader().read(file);
        assertEquals(2, rows.size());
        assertEquals("89852122604294476221", rows.getFirst().get("ICCID"));
    }
}
