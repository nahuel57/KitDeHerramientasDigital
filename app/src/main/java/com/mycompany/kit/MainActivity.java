package herramienta.KitDeHerramientasDigital;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.content.Intent;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnSalaTrabajo = findViewById(R.id.btnSalaTrabajo);
        Button btnLectorQR = findViewById(R.id.btnLectorQR);

        btnSalaTrabajo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SalaTrabajoActivity.class));
            }
        });

        btnLectorQR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, LectorQRActivity.class));
            }
        });
    }
}