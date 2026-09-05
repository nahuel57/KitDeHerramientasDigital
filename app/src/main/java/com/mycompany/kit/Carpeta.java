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

        // modo general: decidir destino según la extensión del archivo tocado
        if (destino == null) {
            if (nombreArchivo.endsWith(".txt")) destino = "BlocNotasActivity";
            else if (nombreArchivo.endsWith(".docx")) destino = "LectorWord";
            else if (nombreArchivo.endsWith(".xlsx")) destino = "LectorExcel";
            else if (nombreArchivo.endsWith(".pptx")) destino = "LectorPowrpoint";
            else if (nombreArchivo.endsWith(".pdf")) destino = "LectorPDF";
            else {
                Toast.makeText(this, "Tipo de archivo no reconocido", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        try {
            Class<?> clase = Class.forName("herramienta.KitDeHerramientasDigital." + destino);
            Intent intent = new Intent(this, clase);
            intent.putExtra("nombreArchivo", nombreArchivo);
            startActivity(intent);
        } catch (ClassNotFoundException e) {
            Toast.makeText(this, "No se encontró la pantalla destino", Toast.LENGTH_SHORT).show();
        }
    }
}