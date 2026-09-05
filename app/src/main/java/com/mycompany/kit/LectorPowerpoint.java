package herramienta.KitDeHerramientasDigital;

import android.app.Activity;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Button;
import android.widget.Toast;
import android.view.View;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class LectorPowrpoint extends Activity {

    EditText editTexto;
    File archivoPPT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lector_powerpoint);

        editTexto = findViewById(R.id.editTexto);
        Button btnGuardar = findViewById(R.id.btnGuardar);

        String nombreArchivo = getIntent().getStringExtra("nombreArchivo");
        if (nombreArchivo == null) nombreArchivo = "presentacion.pptx";
        archivoPPT = new File(getFilesDir(), nombreArchivo);

        cargarPPT();

        btnGuardar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                guardarPPT();
            }
        });
    }

    private void cargarPPT() {
        if (!archivoPPT.exists()) return;
        try {
            FileInputStream entrada = new FileInputStream(archivoPPT);
            XMLSlideShow ppt = new XMLSlideShow(entrada);

            StringBuilder contenido = new StringBuilder();
            List<XSLFSlide> diapositivas = ppt.getSlides();
            for (XSLFSlide diapo : diapositivas) {
                for (Object forma : diapo.getShapes()) {
                    if (forma instanceof XSLFTextBox) {
                        contenido.append(((XSLFTextBox) forma).getText()).append("\n");
                    }
                }
                contenido.append("---\n");
            }
            editTexto.setText(contenido.toString());

            ppt.close();
            entrada.close();
        } catch (IOException e) {
            Toast.makeText(this, "Error al leer la presentación", Toast.LENGTH_SHORT).show();
        }
    }

    private void guardarPPT() {
        try {
            XMLSlideShow ppt = new XMLSlideShow();
            String[] diapositivas = editTexto.getText().toString().split("---\n");

            for (String textoDiapo : diapositivas) {
                if (textoDiapo.trim().isEmpty()) continue;

                XSLFSlide diapo = ppt.createSlide();
                XSLFTextBox cajaTexto = diapo.createTextBox();
                cajaTexto.setText(textoDiapo.trim());
            }

            FileOutputStream salida = new FileOutputStream(archivoPPT);
            ppt.write(salida);
            ppt.close();
            salida.close();

            Toast.makeText(this, "Presentación guardada", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "Error al guardar", Toast.LENGTH_SHORT).show();
        }
    }
}