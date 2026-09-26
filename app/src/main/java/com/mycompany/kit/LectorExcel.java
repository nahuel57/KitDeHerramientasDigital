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
import java.util.List;

public class LectorExcel extends Activity {

    EditText editTexto;
    File archivoExcel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lector_excel);

        editTexto = findViewById(R.id.editTexto);
        Button btnGuardar = findViewById(R.id.btnGuardar);

        String nombreArchivo = getIntent().getStringExtra("nombreArchivo");
        if (nombreArchivo == null) nombreArchivo = "hoja.xlsx";
        archivoExcel = new File(getFilesDir(), nombreArchivo);

        cargarExcel();

        btnGuardar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                guardarExcel();
            }
        });
    }

    private void cargarExcel() {
        if (!archivoExcel.exists()) return;
        try {
            Uri uri = Uri.fromFile(archivoExcel);
            DocumentReader.ReadResult resultado = DocumentReader.read(
                    getContentResolver(),
                    uri,
                    archivoExcel.getName()
            );

            StringBuilder contenido = new StringBuilder();
            for (List<String> fila : resultado.sheets.values().iterator().next()) {
                if (!fila.isEmpty()) {
                    contenido.append(fila.get(0)).append("\n");
                }
            }
            editTexto.setText(contenido.toString());
        } catch (IOException e) {
            Toast.makeText(this, "Error al leer el Excel", Toast.LENGTH_SHORT).show();
        }
    }

    private void guardarExcel() {
        try {
            String[] lineas = editTexto.getText().toString().split("\n");
            ExcelWriter.escribirXlsx(archivoExcel, lineas);
            Toast.makeText(this, "Excel guardado", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "Error al guardar", Toast.LENGTH_SHORT).show();
        }
    }
}
