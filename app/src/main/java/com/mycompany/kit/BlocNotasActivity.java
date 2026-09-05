    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bloc_notas);

        editTexto = findViewById(R.id.editTexto);
        Button btnGuardar = findViewById(R.id.btnGuardar);

        String nombreArchivo = getIntent().getStringExtra("nombreArchivo");
if (nombreArchivo == null) nombreArchivo = "nota.txt"; // por si se abre directo, sin pasar por Carpeta
archivoNota = new File(getFilesDir(), nombreArchivo);


        cargarNota();

        btnGuardar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                guardarNota();
            }
        });
    }

    private void cargarNota() {
        if (!archivoNota.exists()) return;
        try {
            StringBuilder contenido = new StringBuilder();
            BufferedReader lector = new BufferedReader(new FileReader(archivoNota));
            String linea;
            while ((linea = lector.readLine()) != null) {
                contenido.append(linea).append("\n");
            }
            lector.close();
            editTexto.setText(contenido.toString());
        } catch (IOException e) {
            Toast.makeText(this, "Error al leer la nota", Toast.LENGTH_SHORT).show();
        }
    }

    private void guardarNota() {
        try {
            FileWriter escritor = new FileWriter(archivoNota);
            escritor.write(editTexto.getText().toString());
            escritor.close();
            Toast.makeText(this, "Nota guardada", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "Error al guardar", Toast.LENGTH_SHORT).show();
        }
    }
}