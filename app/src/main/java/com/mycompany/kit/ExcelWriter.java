package com.mycompany.kit;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Genera un .xlsx válido desde cero, a partir de una lista de líneas de
 * texto (una por celda de la columna A), SIN Apache POI.
 * Igual que con Word, un .xlsx es un ZIP con XML adentro (OOXML).
 *
 * Simplificación a propósito: en vez de usar sharedStrings.xml (la tabla
 * de strings compartidos que usa Excel para no repetir texto), cada celda
 * lleva su texto directo con t="str" (inline). Es menos "prolijo" que como
 * lo hace Excel/POI normalmente, pero es 100% válido y mucho más simple
 * de generar a mano.
 *
 * Limitación: solo una hoja, solo columna A, sin fórmulas/estilos.
 */
public final class ExcelWriter {

    private ExcelWriter() {
    }

    public static void escribirXlsx(File destino, String[] lineas) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(destino);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            zos.setMethod(ZipOutputStream.DEFLATED);

            escribirEntry(zos, "[Content_Types].xml", contentTypesXml());
            escribirEntry(zos, "_rels/.rels", relsRaizXml());
            escribirEntry(zos, "xl/workbook.xml", workbookXml());
            escribirEntry(zos, "xl/_rels/workbook.xml.rels", workbookRelsXml());
            escribirEntry(zos, "xl/worksheets/sheet1.xml", sheetXml(lineas));
        }
    }

    private static void escribirEntry(ZipOutputStream zos, String nombre, String contenido)
            throws IOException {
        byte[] bytes = contenido.getBytes("UTF-8");
        ZipEntry entry = new ZipEntry(nombre);
        zos.putNextEntry(entry);
        zos.write(bytes);
        zos.closeEntry();
    }

    private static String contentTypesXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                + "<Override PartName=\"/xl/workbook.xml\" "
                + "ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
                + "<Override PartName=\"/xl/worksheets/sheet1.xml\" "
                + "ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
                + "</Types>";
    }

    private static String relsRaizXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" "
                + "Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" "
                + "Target=\"xl/workbook.xml\"/>"
                + "</Relationships>";
    }

    private static String workbookXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" "
                + "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
                + "<sheets>"
                + "<sheet name=\"Hoja1\" sheetId=\"1\" r:id=\"rId1\"/>"
                + "</sheets>"
                + "</workbook>";
    }

    private static String workbookRelsXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" "
                + "Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" "
                + "Target=\"worksheets/sheet1.xml\"/>"
                + "</Relationships>";
    }

    private static String sheetXml(String[] lineas) {
        StringBuilder filas = new StringBuilder();

        for (int i = 0; i < lineas.length; i++) {
            int numeroFila = i + 1; // Excel numera filas desde 1
            String celdaRef = "A" + numeroFila;
            filas.append("<row r=\"").append(numeroFila).append("\">")
                    .append("<c r=\"").append(celdaRef).append("\" t=\"str\">")
                    .append("<v>").append(escaparXml(lineas[i])).append("</v>")
                    .append("</c>")
                    .append("</row>");
        }

        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
                + "<sheetData>"
                + filas
                + "</sheetData>"
                + "</worksheet>";
    }

    private static String escaparXml(String texto) {
        return texto
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
