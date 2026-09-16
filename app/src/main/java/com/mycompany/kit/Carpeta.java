package com.mycompany.kit;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import java.io.File;
import java.util.ArrayList;

public class Carpeta extends Activity {

    ListView listaArchivos;
    ArrayList<String> nombresArchivos;
    ArrayAdapter<String> adaptador;
    String extensionFiltro;
    String claseDestino;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_carpeta);

        extensionFiltro = getIntent().getStringExtra("extension");
        claseDestino = getIntent().getStringExtra("claseDestino");

        listaArchivos = findViewById(R.id.listaArchivos);
        Button btnNuevo = findViewById(R.id.btnNuevoArchivo);

        if (extensionFiltro == null) {
            btnNuevo.setVisibility(View.GONE);
        }

        cargarLista();

        listaArchivos.setOnItemClickListener((parent, view, position, id) -> {
            abrirArchivo(nombresArchivos.get(position));
        });

        btnNuevo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pedirNombreNuevoArchivo();
            }
        });
    }

    private void cargarLista() {
        nombresArchivos = new ArrayList<>();
        File carpeta = getFilesDir();
        File[] archivos = carpeta.listFiles();

        if (archivos != null) {
            for (File archivo : archivos) {
                if (extensionFiltro == null || archivo.getName().endsWith(extensionFiltro)) {
                    nombresArchivos.add(archivo.getName());
                }
            }
        }

        adaptador = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, nombresArchivos);
        listaArchivos.setAdapter(adaptador);
    }

    private void pedirNombreNuevoArchivo() {
        final EditText input = new EditText(this);
        input.setHint("nombre" + extensionFiltro);

        new AlertDialog.Builder(this)
            .setTitle("Nuevo archivo")
            .setView(input)
            .setPositiveButton("Crear", (dialog, which) -> {
                String nombre = input.getText().toString().trim();
                if (nombre.isEmpty()) {
                    Toast.makeText(this, "Ponele un nombre", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!nombre.endsWith(extensionFiltro)) nombre += extensionFiltro;
                abrirArchivo(nombre);
            })
            .setNegativeButton("Cancelar", null)
            .show();
    }

    private void abrirArchivo(String nombreArchivo) {
        String destino = claseDestino;

        if (destino == null) {
            if (nombreArchivo.endsWith(".txt")) destino = "BlocNotasActivity";
            else if (nombreArchivo.endsWith(".docx")) destino = "LectorWord";
            else if (nombreArchivo.endsWith(".xlsx")) destino = "LectorExcel";
            else if (nombreArchivo.endsWith(".pptx")) destino = "LectorPowerpoint";
            else if (nombreArchivo.endsWith(".pdf")) destino = "LectorPDF";
            else {
                Toast.makeText(this, "Tipo de archivo no reconocido", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        try {
            Class<?> clase = Class.forName("com.mycompany.kit." + destino);
            Intent intent = new Intent(this, clase);
            intent.putExtra("nombreArchivo", nombreArchivo);
            startActivity(intent);
        } catch (ClassNotFoundException e) {
            Toast.makeText(this, "No se encontró la pantalla destino", Toast.LENGTH_SHORT).show();
        }
    }
}
