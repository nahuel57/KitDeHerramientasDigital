package herramienta.KitDeHerramientasDigital;

import android.app.Activity;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Button;
import android.widget.Toast;
import android.view.View;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class LectorWord extends Activity {

    EditText editTexto;
    File archivoWord;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lector_word);

        editTexto = findViewById(R.id.editTexto);
        Button btnGuardar = findViewById(R.id.btnGuardar);

        archivoWord = new File(getFilesDir(), "documento.docx");

        cargarDocumento();

        btnGuardar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                guardarDocumento();
            }
        });
    }

    private void cargarDocumento() {
        if (!archivoWord.exists()) return;
        try {
            FileInputStream entrada = new FileInputStream(archivoWord);
            XWPFDocument documento = new XWPFDocument(entrada);

            StringBuilder contenido = new StringBuilder();
            for (XWPFParagraph parrafo : documento.getParagraphs()) {
                contenido.append(parrafo.getText()).append("\n");
            }
            editTexto.setText(contenido.toString());

            documento.close();
            entrada.close();
        } catch (IOException e) {
            Toast.makeText(this, "Error al leer el documento", Toast.LENGTH_SHORT).show();
        }
    }

    private void guardarDocumento() {
        try {
            XWPFDocument documento = new XWPFDocument();
            XWPFParagraph parrafo = documento.createParagraph();
            XWPFRun texto = parrafo.createRun();
            texto.setText(editTexto.getText().toString());

            FileOutputStream salida = new FileOutputStream(archivoWord);
            documento.write(salida);
            documento.close();
            salida.close();

            Toast.makeText(this, "Documento guardado", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "Error al guardar", Toast.LENGTH_SHORT).show();
        }
    }
}