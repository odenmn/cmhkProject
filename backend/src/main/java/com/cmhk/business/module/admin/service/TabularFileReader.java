package com.cmhk.business.module.admin.service;

import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class TabularFileReader {
    private static final Logger log = LoggerFactory.getLogger(TabularFileReader.class);
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    public List<Map<String, String>> read(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("请选择需要导入的文件");
        if (file.getSize() > MAX_FILE_SIZE) throw new IllegalArgumentException("导入文件不能超过10MB");
        String name = Optional.ofNullable(file.getOriginalFilename()).orElse("").toLowerCase(Locale.ROOT);
        try {
            byte[] content = file.getBytes();
            if (name.endsWith(".csv")) return readCsv(new ByteArrayInputStream(content));
            if (name.endsWith(".xlsx") || name.endsWith(".xls") || name.endsWith(".xlsm")) {
                if (looksLikePlainText(content)) {
                    log.info("检测到扩展名为 Excel 的纯文本卡号文件，按逐行 ICCID 清单处理，fileName={}", file.getOriginalFilename());
                    return readPlainIccidList(content);
                }
                return readExcel(new ByteArrayInputStream(content));
            }
            throw new IllegalArgumentException("仅支持 XLSX、XLS、XLSM 和 CSV 文件");
        } catch (IOException | RuntimeException exception) {
            log.warn("导入文件读取失败，fileName={}, contentType={}, reason={}", file.getOriginalFilename(), file.getContentType(), exception.getMessage());
            String detail = exception.getMessage();
            if (detail != null && detail.toLowerCase(Locale.ROOT).contains("encrypted")) {
                throw new IllegalArgumentException("Excel 文件已加密，请取消密码保护后再导入");
            }
            throw new IllegalArgumentException("无法读取文件：" + (detail == null || detail.isBlank() ? "请确认文件未损坏且为有效 Excel/CSV" : detail));
        }
    }

    private List<Map<String, String>> readPlainIccidList(byte[] content) {
        String text = new String(content, StandardCharsets.UTF_8).replace("\uFEFF", "");
        List<Map<String, String>> result = new ArrayList<>();
        String[] lines = text.split("\\R");
        for (int index = 0; index < lines.length; index++) {
            String value = lines[index].trim();
            if (value.isBlank()) continue;
            LinkedHashMap<String, String> row = new LinkedHashMap<>();
            row.put("ICCID", value);
            row.put("__rowNumber", String.valueOf(index + 1));
            result.add(row);
        }
        return result;
    }

    private boolean looksLikePlainText(byte[] content) {
        if (content.length == 0) return false;
        if (content.length >= 2 && content[0] == 'P' && content[1] == 'K') return false;
        if (content.length >= 8 && content[0] == (byte) 0xD0 && content[1] == (byte) 0xCF) return false;
        int printable = 0;
        for (byte current : content) {
            int value = Byte.toUnsignedInt(current);
            if (value == 9 || value == 10 || value == 13 || (value >= 32 && value <= 126) || value >= 0x80) printable++;
        }
        return printable * 100 / content.length >= 95;
    }

    private List<Map<String, String>> readExcel(InputStream input) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(input)) {
            Sheet sheet = workbook.getNumberOfSheets() == 0 ? null : workbook.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() == 0) return List.of();
            DataFormatter formatter = new DataFormatter();
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            List<String> headers = new ArrayList<>();
            for (int i = 0; i < headerRow.getLastCellNum(); i++) headers.add(clean(formatter.formatCellValue(headerRow.getCell(i))));
            List<Map<String, String>> result = new ArrayList<>();
            for (int rowIndex = sheet.getFirstRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex); if (row == null) continue;
                LinkedHashMap<String, String> values = new LinkedHashMap<>(); boolean hasValue = false;
                for (int i = 0; i < headers.size(); i++) { String value = formatter.formatCellValue(row.getCell(i)).trim(); values.put(headers.get(i), value); hasValue |= !value.isBlank(); }
                if (hasValue) { values.put("__rowNumber", String.valueOf(rowIndex + 1)); result.add(values); }
            }
            return result;
        }
    }

    private List<Map<String, String>> readCsv(InputStream input) throws IOException {
        List<Map<String, String>> result = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine(); if (headerLine == null) return result;
            List<String> headers = parseCsv(headerLine); headers.replaceAll(this::clean);
            String line; int row = 1;
            while ((line = reader.readLine()) != null) { row++; List<String> cells = parseCsv(line); LinkedHashMap<String,String> values=new LinkedHashMap<>(); boolean has=false;
                for(int i=0;i<headers.size();i++){String value=i<cells.size()?cells.get(i).trim():"";values.put(headers.get(i),value);has|=!value.isBlank();}
                if(has){values.put("__rowNumber",String.valueOf(row));result.add(values);} }
        }
        return result;
    }

    private List<String> parseCsv(String line) {
        List<String> values = new ArrayList<>(); StringBuilder current = new StringBuilder(); boolean quoted = false;
        for (int i=0;i<line.length();i++){char c=line.charAt(i);if(c=='"'){if(quoted&&i+1<line.length()&&line.charAt(i+1)=='"'){current.append('"');i++;}else quoted=!quoted;}else if(c==','&&!quoted){values.add(current.toString());current.setLength(0);}else current.append(c);} values.add(current.toString()); return values;
    }

    private String clean(String value) { return value == null ? "" : value.replace("\uFEFF", "").trim(); }
}
