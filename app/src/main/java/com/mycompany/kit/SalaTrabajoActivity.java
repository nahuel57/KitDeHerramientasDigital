package herramienta.KitDeHerramientasDigital;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.content.Intent;

public class SalaTrabajoActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sala_trabajo);

        Button btnWord = findViewById(R.id.btnWord);
        Button btnPdf = findViewById(R.id.btnPdf);
        Button btnExcel = findViewById(R.id.btnExcel);
        Button btnBlocNotas = findViewById(R.id.btnBlocNotas);
        Button btnCarpeta = findViewById(R.id.btnCarpeta);
        Button btnPowerpoint = findViewById(R.id.btnPowerpoint);

        btnWord.setOnClickListener(new View.OnClickListener() {
    @Override
    public void onClick(View v) {
        Intent intent = new Intent(SalaTrabajoActivity.this, Carpeta.class);
        intent.putExtra("extension", ".docx");
        intent.putExtra("claseDestino", "LectorWord");
        startActivity(intent);
    }
});

        btnExcel.setOnClickListener(new View.OnClickListener() {
    @Override
    public void onClick(View v) {
        Intent intent = new Intent(SalaTrabajoActivity.this, Carpeta.class);
        intent.putExtra("extension", ".xlsx");
        intent.putExtra("claseDestino", "LectorExcel");
        startActivity(intent);
    }
});

        btnBlocNotas.setOnClickListener(new View.OnClickListener() {
    @Override
    public void onClick(View v) {
        Intent intent = new Intent(SalaTrabajoActivity.this, Carpeta.class);
        intent.putExtra("extension", ".txt");
        intent.putExtra("claseDestino", "BlocNotasActivity");
        startActivity(intent);
    }
});
            

  btnCarpeta.setOnClickListener(new View.OnClickListener() {
    @Override
    public void onClick(View v) {
        Intent intent = new Intent(SalaTrabajoActivity.this, Carpeta.class); 
        startActivity(intent);
    }
});

        btnPowerpoint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SalaTrabajoActivity.this, LectorPowerpoint.class));
            }
        });
    }
}