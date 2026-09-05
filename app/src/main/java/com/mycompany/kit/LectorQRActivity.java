package herramienta.KitDeHerramientasDigital;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class LectorQRActivity extends Activity {

    private static final int REQUEST_PERMISO_CAMARA = 200;

    private TextureView textureView;
    private CameraDevice cameraDevice;
    private ImageReader imageReader;
    private MultiFormatReader qrReader;
    private boolean procesando = false;
    private boolean resultadoEncontrado = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lector_qr);

        qrReader = new MultiFormatReader();
        textureView = findViewById(R.id.textureCamara);

        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                pedirPermisoYAbrirCamara();
            }
            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {}
            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) { return true; }
            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {}
        });
    }

    private void pedirPermisoYAbrirCamara() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_PERMISO_CAMARA);
        } else {
            abrirCamara();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] resultados) {
        super.onRequestPermissionsResult(requestCode, permissions, resultados);
        if (requestCode == REQUEST_PERMISO_CAMARA) {
            if (resultados.length > 0 && resultados[0] == PackageManager.PERMISSION_GRANTED) {
                abrirCamara();
            } else {
                Toast.makeText(this, "Se necesita permiso de cámara", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void abrirCamara() {
        CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            String idCamaraTrasera = null;
            for (String id : manager.getCameraIdList()) {
                Integer lente = manager.getCameraCharacteristics(id)
                        .get(android.hardware.camera2.CameraCharacteristics.LENS_FACING);
                if (lente != null && lente == android.hardware.camera2.CameraCharacteristics.LENS_FACING_BACK) {
                    idCamaraTrasera = id;
                    break;
                }
            }
            if (idCamaraTrasera == null) idCamaraTrasera = manager.getCameraIdList()[0];

            imageReader = ImageReader.newInstance(640, 480, ImageFormat.YUV_420_888, 2);
            imageReader.setOnImageAvailableListener(reader -> {
                Image image = reader.acquireLatestImage();
                if (image == null) return;
                procesarImagen(image);
                image.close();
            }, null);

            manager.openCamera(idCamaraTrasera, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(CameraDevice camera) {
                    cameraDevice = camera;
                    iniciarPreview();
                }
                @Override
                public void onDisconnected(CameraDevice camera) {
                    camera.close();
                    cameraDevice = null;
                }
                @Override
                public void onError(CameraDevice camera, int error) {
                    camera.close();
                    cameraDevice = null;
                    Toast.makeText(LectorQRActivity.this, "Error al abrir la cámara", Toast.LENGTH_LONG).show();
                    finish();
                }
            }, null);

        } catch (CameraAccessException | SecurityException e) {
            Toast.makeText(this, "No se pudo acceder a la cámara", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void iniciarPreview() {
        try {
            SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
            surfaceTexture.setDefaultBufferSize(640, 480);
            Surface previewSurface = new Surface(surfaceTexture);
            Surface readerSurface = imageReader.getSurface();

            CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            builder.addTarget(previewSurface);
            builder.addTarget(readerSurface);

            cameraDevice.createCaptureSession(Arrays.asList(previewSurface, readerSurface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(CameraCaptureSession session) {
                            try {
                                session.setRepeatingRequest(builder.build(), null, null);
                            } catch (CameraAccessException e) {
                                Toast.makeText(LectorQRActivity.this, "Error iniciando la vista previa", Toast.LENGTH_LONG).show();
                            }
                        }
                        @Override
                        public void onConfigureFailed(CameraCaptureSession session) {
                            Toast.makeText(LectorQRActivity.this, "No se pudo configurar la cámara", Toast.LENGTH_LONG).show();
                            finish();
                        }
                    }, null);

        } catch (CameraAccessException e) {
            Toast.makeText(this, "Error iniciando la cámara", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void procesarImagen(Image image) {
        if (procesando || resultadoEncontrado) return;
        procesando = true;

        int width = image.getWidth();
        int height = image.getHeight();
        Image.Plane yPlane = image.getPlanes()[0];
        ByteBuffer buffer = yPlane.getBuffer();
        int rowStride = yPlane.getRowStride();

        byte[] datosY = new byte[width * height];
        if (rowStride == width) {
            buffer.get(datosY);
        } else {
            byte[] fila = new byte[rowStride];
            for (int i = 0; i < height; i++) {
                buffer.get(fila, 0, Math.min(rowStride, buffer.remaining()));
                System.arraycopy(fila, 0, datosY, i * width, width);
            }
        }

        PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(
                datosY, width, height, 0, 0, width, height, false);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

        try {
            Result result = qrReader.decodeWithState(bitmap);
            resultadoEncontrado = true;
            String contenido = result.getText();
            runOnUiThread(() -> mostrarResultado(contenido));
        } catch (Exception e) {
            qrReader.reset();
        } finally {
            procesando = false;
        }
    }

    private void mostrarResultado(String contenido) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Código QR leído");
        builder.setMessage(contenido);

        boolean esLink = contenido.startsWith("http://") || contenido.startsWith("https://");

        if (esLink) {
            builder.setPositiveButton("Abrir enlace", (dialog, which) -> {
                android.content.Intent abrir = new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(contenido));
                startActivity(abrir);
                finish();
            });
        }

        builder.setNegativeButton("Cerrar", (dialog, which) -> finish());
        builder.setCancelable(false);
        builder.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (imageReader != null) {
            imageReader.close();
        }
    }
}