package com.kitlectores.android;

import android.content.ContentResolver;
import android.net.Uri;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Reads Office files (.docx / .xlsx) from a Content URI SIN Apache POI.
 * Un .docx/.xlsx es un ZIP con XML adentro (formato OOXML), así que esto
 * usa java.util.zip + XmlPullParser, ambos nativos de Android, sin
 * dependencias externas y sin conflictos con D8.
 *
 * OJO: el .xls viejo (binario, formato OLE2, pre-2007) NO es un ZIP,
 * así que este motor no lo puede leer. Si necesitás .xls habría que
 * agregar un parser binario aparte (mucho más complejo). Por ahora
 * solo se admite .docx y .xlsx.
 *
 * Bytes: quedan en el dispositivo. No se sube nada a ningún lado.
 */
public final class DocumentReader {

    private DocumentReader() {
    }

    public enum Kind {
        WORD,
        EXCEL
    }

    public static final class ReadResult {
        public final Kind kind;
        public final String title;
        public final String text;
        public final LinkedHashMap<String, List<List<String>>> sheets;

        private ReadResult(
                Kind kind,
                String title,
                String text,
                LinkedHashMap<String, List<List<String>>> sheets
        ) {
            this.kind = kind;
            this.title = title;
            this.text = text;
            this.sheets = sheets;
        }

        public static ReadResult word(String title, String text) {
            return new ReadResult(
                    Kind.WORD,
                    title,
                    text,
                    new LinkedHashMap<>()
            );
        }

        public static ReadResult excel(
                String title,
                LinkedHashMap<String, List<List<String>>> sheets
        ) {
            return new ReadResult(
                    Kind.EXCEL,
                    title,
                    "",
                    sheets
            );
        }
    }

    public static ReadResult read(
            ContentResolver resolver,
            Uri uri,
            String displayName
    ) throws IOException {
        String lowerName = displayName.toLowerCase(Locale.ROOT);

        if (lowerName.endsWith(".docx")) {
            return readWord(resolver, uri, displayName);
        }

        if (lowerName.endsWith(".xlsx")) {
            return readExcel(resolver, uri, displayName);
        }

        if (lowerName.endsWith(".xls")) {
            throw new IOException(
                    "El .xls viejo (binario) todavía no está soportado por este lector. Usá .xlsx."
            );
        }

        throw new IOException("Formato no compatible. Usa DOCX o XLSX.");
    }

    // ---------------------------------------------------------------
    // Utilidad común: cargar todas las entradas del ZIP en memoria,
    // indexadas por nombre de archivo interno (ej: "word/document.xml").
    // ---------------------------------------------------------------
    private static Map<String, byte[]> unzipAll(InputStream input) throws IOException {
        Map<String, byte[]> files = new HashMap<>();
        try (ZipInputStream zis = new ZipInputStream(input)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    baos.write(buffer, 0, len);
                }
                files.put(entry.getName(), baos.toByteArray());
            }
        }
        return files;
    }

    private static String bytesToUtf8(byte[] bytes) throws IOException {
        if (bytes == null) {
            return null;
        }
        return new String(bytes, "UTF-8");
    }

    private static XmlPullParser newParser(String xmlContent)
            throws XmlPullParserException {
        XmlPullParser parser = Xml.newPullParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
        try {
            parser.setInput(new StringReader(xmlContent));
        } catch (Exception e) {
            // setInput(Reader) no debería tirar checked exceptions acá
        }
        return parser;
    }

    // =================================================================
    // WORD (.docx)
    // =================================================================

    private static ReadResult readWord(
            ContentResolver resolver,
            Uri uri,
            String displayName
    ) throws IOException {
        try (InputStream input = resolver.openInputStream(uri)) {
            Map<String, byte[]> files = unzipAll(input);
            byte[] docXmlBytes = files.get("word/document.xml");
            if (docXmlBytes == null) {
                throw new IOException("El .docx no tiene word/document.xml (¿archivo corrupto?).");
            }
            String xml = bytesToUtf8(docXmlBytes);
            String texto = parseWordXml(xml);
            return ReadResult.word(displayName, texto.trim());
        } catch (XmlPullParserException e) {
            throw new IOException("Error parseando el XML del documento Word.", e);
        }
    }

    /**
     * Recorre document.xml en orden, respetando párrafos sueltos y tablas,
     * igual que hacía XWPFDocument.getBodyElements() con POI.
     */
    private static String parseWordXml(String xml) throws XmlPullParserException, IOException {
        XmlPullParser parser = newParser(xml);

        StringBuilder output = new StringBuilder();
        StringBuilder paragraphBuffer = new StringBuilder();

        boolean insideTable = false;
        List<List<String>> tableRows = new ArrayList<>();
        List<String> currentRowCells = new ArrayList<>();
        StringBuilder cellBuffer = new StringBuilder();

        int eventType = parser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            String tag = parser.getName();

            if (eventType == XmlPullParser.START_TAG) {
                if ("w:tbl".equals(tag)) {
                    insideTable = true;
                    tableRows = new ArrayList<>();
                } else if (insideTable && "w:tr".equals(tag)) {
                    currentRowCells = new ArrayList<>();
                } else if (insideTable && "w:tc".equals(tag)) {
                    cellBuffer = new StringBuilder();
                } else if ("w:p".equals(tag) && !insideTable) {
                    paragraphBuffer = new StringBuilder();
                }
            } else if (eventType == XmlPullParser.TEXT) {
                if (insideTable) {
                    cellBuffer.append(parser.getText());
                } else {
                    paragraphBuffer.append(parser.getText());
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if ("w:tbl".equals(tag)) {
                    for (List<String> row : tableRows) {
                        output.append(String.join("\t", row)).append("\n");
                    }
                    output.append("\n");
                    insideTable = false;
                } else if (insideTable && "w:tc".equals(tag)) {
                    currentRowCells.add(cellBuffer.toString().replace('\n', ' ').trim());
                } else if (insideTable && "w:tr".equals(tag)) {
                    tableRows.add(currentRowCells);
                } else if ("w:p".equals(tag) && !insideTable) {
                    String paragraph = paragraphBuffer.toString().trim();
                    if (!paragraph.isEmpty()) {
                        output.append(paragraph).append("\n\n");
                    }
                }
            }
            eventType = parser.next();
        }

        return output.toString();
    }

    // =================================================================
    // EXCEL (.xlsx)
    // =================================================================

    private static ReadResult readExcel(
            ContentResolver resolver,
            Uri uri,
            String displayName
    ) throws IOException {
        try (InputStream input = resolver.openInputStream(uri)) {
            Map<String, byte[]> files = unzipAll(input);

            byte[] workbookXmlBytes = files.get("xl/workbook.xml");
            byte[] relsXmlBytes = files.get("xl/_rels/workbook.xml.rels");
            if (workbookXmlBytes == null || relsXmlBytes == null) {
                throw new IOException("El .xlsx no tiene xl/workbook.xml o sus rels (¿archivo corrupto?).");
            }

            // 1) Strings compartidos (Excel separa texto de números en sharedStrings.xml)
            List<String> sharedStrings = new ArrayList<>();
            byte[] sharedStringsBytes = files.get("xl/sharedStrings.xml");
            if (sharedStringsBytes != null) {
                sharedStrings = parseSharedStrings(bytesToUtf8(sharedStringsBytes));
            }

            // 2) Nombre de hoja -> r:id (orden tal cual aparece en workbook.xml)
            LinkedHashMap<String, String> sheetNameToRid =
                    parseWorkbookSheets(bytesToUtf8(workbookXmlBytes));

            // 3) r:id -> archivo real (ej: "rId2" -> "worksheets/sheet2.xml")
            Map<String, String> ridToTarget = parseRels(bytesToUtf8(relsXmlBytes));

            LinkedHashMap<String, List<List<String>>> sheets = new LinkedHashMap<>();

            for (Map.Entry<String, String> entry : sheetNameToRid.entrySet()) {
                String sheetName = entry.getKey();
                String rid = entry.getValue();
                String target = ridToTarget.get(rid);
                if (target == null) {
                    continue;
                }
                String sheetPath = "xl/" + target;
                byte[] sheetBytes = files.get(sheetPath);
                if (sheetBytes == null) {
                    continue;
                }
                List<List<String>> rows = parseSheetXml(bytesToUtf8(sheetBytes), sharedStrings);
                sheets.put(sheetName, rows);
            }

            return ReadResult.excel(displayName, sheets);
        } catch (XmlPullParserException e) {
            throw new IOException("Error parseando el XML de la planilla Excel.", e);
        }
    }

    private static List<String> parseSharedStrings(String xml)
            throws XmlPullParserException, IOException {
        XmlPullParser parser = newParser(xml);
        List<String> strings = new ArrayList<>();
        StringBuilder currentSi = new StringBuilder();
        boolean insideSi = false;

        int eventType = parser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            String tag = parser.getName();
            if (eventType == XmlPullParser.START_TAG && "si".equals(tag)) {
                insideSi = true;
                currentSi = new StringBuilder();
            } else if (eventType == XmlPullParser.TEXT && insideSi) {
                currentSi.append(parser.getText());
            } else if (eventType == XmlPullParser.END_TAG && "si".equals(tag)) {
                strings.add(currentSi.toString());
                insideSi = false;
            }
            eventType = parser.next();
        }
        return strings;
    }

    private static LinkedHashMap<String, String> parseWorkbookSheets(String xml)
            throws XmlPullParserException, IOException {
        XmlPullParser parser = newParser(xml);
        LinkedHashMap<String, String> sheetNameToRid = new LinkedHashMap<>();

        int eventType = parser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && "sheet".equals(parser.getName())) {
                String name = null;
                String rid = null;
                for (int i = 0; i < parser.getAttributeCount(); i++) {
                    String attrName = parser.getAttributeName(i);
                    if ("name".equals(attrName)) {
                        name = parser.getAttributeValue(i);
                    } else if (attrName != null && attrName.endsWith(":id")) {
                        // suele venir como "r:id"
                        rid = parser.getAttributeValue(i);
                    }
                }
                if (name != null && rid != null) {
                    sheetNameToRid.put(name, rid);
                }
            }
            eventType = parser.next();
        }
        return sheetNameToRid;
    }

    private static Map<String, String> parseRels(String xml)
            throws XmlPullParserException, IOException {
        XmlPullParser parser = newParser(xml);
        Map<String, String> ridToTarget = new HashMap<>();

        int eventType = parser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && "Relationship".equals(parser.getName())) {
                String id = parser.getAttributeValue(null, "Id");
                String target = parser.getAttributeValue(null, "Target");
                if (id != null && target != null) {
                    // Target normalmente ya viene como "worksheets/sheet1.xml"
                    ridToTarget.put(id, target);
                }
            }
            eventType = parser.next();
        }
        return ridToTarget;
    }

    private static List<List<String>> parseSheetXml(String xml, List<String> sharedStrings)
            throws XmlPullParserException, IOException {
        XmlPullParser parser = newParser(xml);
        List<List<String>> rows = new ArrayList<>();

        List<String> currentRow = null;
        String currentCellType = null;
        int currentCellColumn = -1;
        StringBuilder valueBuffer = new StringBuilder();
        boolean insideValue = false;

        int eventType = parser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            String tag = parser.getName();

            if (eventType == XmlPullParser.START_TAG) {
                if ("row".equals(tag)) {
                    currentRow = new ArrayList<>();
                } else if ("c".equals(tag)) {
                    String ref = parser.getAttributeValue(null, "r"); // ej: "C4"
                    currentCellType = parser.getAttributeValue(null, "t"); // "s", "str", "inlineStr" o null
                    currentCellColumn = ref != null ? columnIndexFromRef(ref) : currentCellColumn + 1;
                } else if ("v".equals(tag) || "t".equals(tag)) {
                    insideValue = true;
                    valueBuffer = new StringBuilder();
                }
            } else if (eventType == XmlPullParser.TEXT && insideValue) {
                valueBuffer.append(parser.getText());
            } else if (eventType == XmlPullParser.END_TAG) {
                if (("v".equals(tag) || "t".equals(tag))) {
                    insideValue = false;
                } else if ("c".equals(tag) && currentRow != null) {
                    String rawValue = valueBuffer.toString();
                    String cellText;
                    if ("s".equals(currentCellType)) {
                        int idx = rawValue.isEmpty() ? -1 : Integer.parseInt(rawValue.trim());
                        cellText = (idx >= 0 && idx < sharedStrings.size()) ? sharedStrings.get(idx) : "";
                    } else {
                        cellText = rawValue;
                    }
                    // Rellenar huecos si saltea columnas (ej: A1 y C1 sin B1)
                    while (currentRow.size() < currentCellColumn) {
                        currentRow.add("");
                    }
                    if (currentRow.size() == currentCellColumn) {
                        currentRow.add(cellText);
                    } else {
                        currentRow.set(currentCellColumn, cellText);
                    }
                    valueBuffer = new StringBuilder();
                } else if ("row".equals(tag) && currentRow != null) {
                    trimTrailingEmpty(currentRow);
                    if (!currentRow.isEmpty()) {
                        rows.add(currentRow);
                    }
                    currentRow = null;
                    currentCellColumn = -1;
                }
            }
            eventType = parser.next();
        }

        return rows;
    }

    private static void trimTrailingEmpty(List<String> values) {
        while (!values.isEmpty() && values.get(values.size() - 1).isEmpty()) {
            values.remove(values.size() - 1);
        }
    }

    /** Convierte una referencia de celda tipo "C4" en índice de columna 0-based (C -> 2). */
    private static int columnIndexFromRef(String ref) {
        int index = 0;
        for (int i = 0; i < ref.length(); i++) {
            char c = ref.charAt(i);
            if (c < 'A' || c > 'Z') {
                break;
            }
            index = index * 26 + (c - 'A' + 1);
        }
        return index - 1;
    }

    // =================================================================
    // Utilidades compartidas (sin cambios respecto a la versión con POI)
    // =================================================================

    public static List<List<String>> filterRows(
            List<List<String>> rows,
            String query
    ) {
        if (query == null || query.trim().isEmpty()) {
            return rows;
        }

        String normalizedQuery = query.trim().toLowerCase(Locale.ROOT);
        List<List<String>> filtered = new ArrayList<>();
        for (List<String> row : rows) {
            boolean matches = false;
            for (String value : row) {
                if (value.toLowerCase(Locale.ROOT).contains(normalizedQuery)) {
                    matches = true;
                    break;
                }
            }
            if (matches) {
                filtered.add(row);
            }
        }
        return filtered;
    }

    public static String rowsToText(List<List<String>> rows) {
        List<String> lines = new ArrayList<>();
        for (List<String> row : rows) {
            lines.add(String.join("\t", row));
        }
        return String.join("\n", lines);
    }
}
