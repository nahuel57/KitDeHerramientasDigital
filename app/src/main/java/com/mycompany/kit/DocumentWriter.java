package com.mycompany.kit;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Genera un .docx válido desde cero, a partir de texto plano, SIN Apache POI.
 * Un .docx es un ZIP con XML adentro (formato OOXML). Esta clase arma la
 * estructura mínima que Word necesita para abrirlo sin marcarlo corrupto.
 *
 * Limitación: solo texto plano, un párrafo por cada línea del original.
 * No soporta negrita/cursiva/tamaños ni imágenes (eso sería una versión futura).
 */
public final class DocumentWriter {

    private DocumentWriter() {
    }

    public static void escribirDocx(File destino, String textoPlano) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(destino);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            // Los .docx suelen ir sin compresión de nivel alto en estos
            // archivos chicos de estructura; STORED evita problemas raros
            // de CRC en algunos lectores estrictos. Usamos DEFLATED simple,
            // que es lo habitual y funciona bien en Word/LibreOffice.
            zos.setMethod(ZipOutputStream.DEFLATED);

            escribirEntry(zos, "[Content_Types].xml", contentTypesXml());
            escribirEntry(zos, "_rels/.rels", relsRaizXml());
            escribirEntry(zos, "word/_rels/document.xml.rels", relsDocumentoXml());
            escribirEntry(zos, "word/document.xml", documentoXml(textoPlano));
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
                + "<Override PartName=\"/word/document.xml\" "
                + "ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/>"
                + "</Types>";
    }

    private static String relsRaizXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" "
                + "Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" "
                + "Target=\"word/document.xml\"/>"
                + "</Relationships>";
    }

    private static String relsDocumentoXml() {
        // Documento simple sin estilos/imágenes/numeración: no necesita
        // más relaciones que esta lista vacía, pero el archivo tiene que
        // existir igual porque Word lo espera.
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "</Relationships>";
    }

    private static String documentoXml(String textoPlano) {
        StringBuilder parrafos = new StringBuilder();
        String[] lineas = textoPlano.split("\n", -1);

        for (String linea : lineas) {
            parrafos.append("<w:p><w:r><w:t xml:space=\"preserve\">")
                    .append(escaparXml(linea))
                    .append("</w:t></w:r></w:p>");
        }

        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">"
                + "<w:body>"
                + parrafos
                + "</w:body>"
                + "</w:document>";
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
