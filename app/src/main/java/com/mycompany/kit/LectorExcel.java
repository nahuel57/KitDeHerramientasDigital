package herramienta.KitDeHerramientasDigital;

import android.app.Activity;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Button;
import android.widget.Toast;
import android.view.View;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

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
            FileInputStream entrada = new FileInputStream(archivoExcel);
            XSSFWorkbook libro = new XSSFWorkbook(entrada);
            XSSFSheet hoja = libro.getSheetAt(0);

            StringBuilder contenido = new StringBuilder();
            for (Row fila : hoja) {
                Cell celda = fila.getCell(0);
                if (celda != null) {
                    contenido.append(celda.getStringCellValue()).append("\n");
                }
            }
            editTexto.setText(contenido.toString());

            libro.close();
            entrada.close();
        } catch (IOException e) {
            Toast.makeText(this, "Error al leer el Excel", Toast.LENGTH_SHORT).show();
        }
    }

    private void guardarExcel() {
        try {
            XSSFWorkbook libro = new XSSFWorkbook();
            XSSFSheet hoja = libro.createSheet("Hoja1");

            String[] lineas = editTexto.getText().toString().split("\n");
            for (int i = 0; i < lineas.length; i++) {
                Row fila = hoja.createRow(i);
                Cell celda = fila.createCell(0);
                celda.setCellValue(lineas[i]);
            }

            FileOutputStream salida = new FileOutputStream(archivoExcel);
            libro.write(salida);
            libro.close();
            salida.close();

            Toast.makeText(this, "Excel guardado", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "Error al guardar", Toast.LENGTH_SHORT).show();
        }
    }
}
