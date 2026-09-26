package com.mycompany.kit;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Button;
import android.widget.Toast;
import android.view.View;
import java.io.File;
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
            Uri uri = Uri.fromFile(archivoWord);
            DocumentReader.ReadResult resultado = DocumentReader.read(
                    getContentResolver(),
                    uri,
                    archivoWord.getName()
            );
            editTexto.setText(resultado.text);
        } catch (IOException e) {
            Toast.makeText(this, "Error al leer el documento", Toast.LENGTH_SHORT).show();
        }
    }

    private void guardarDocumento() {
        // Todavía usa POI (XWPFDocument) — esto es lo que sigue: armar
        // un DocumentWriter que genere el .docx a mano (ZIP + XML de
        // plantilla), como hablamos para "crear desde cero".
        Toast.makeText(this, "Guardado: pendiente de reescribir", Toast.LENGTH_SHORT).show();
    }
}