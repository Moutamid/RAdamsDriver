package com.moutamid.radamsdriver;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.fxn.stash.Stash;
import com.moutamid.radamsdriver.databinding.ActivityCameraBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class CameraActivity extends AppCompatActivity {
    ActivityCameraBinding binding;
    int width, height;
    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSession;
    private CameraCharacteristics cameraCharacteristics;
    private CaptureRequest.Builder captureRequestBuilder;
    private ImageReader imageReader;
    private Handler backgroundHandler;
    StreamConfigurationMap streamConfigurationMap;
    boolean isVertical;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCameraBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Window window = this.getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.black));

        binding.back.setOnClickListener(v -> onBackPressed());

        SimpleOrientationListener mOrientationListener = new SimpleOrientationListener(this) {

            @Override
            public void onSimpleOrientationChanged(int orientation) {
                Log.d("CHECK123", "orientation   " + orientation);
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    isVertical = false;
                    Log.d("CHECK123", "LANDSCAPE");
                } else if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                    isVertical = true;
                    Log.d("CHECK123", "PORTRAIT");
                }
            }
        };
        mOrientationListener.enable();


        width = 1280;
        height = 720;

        // Step 4: Camera Initialization
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        new Handler().postDelayed(() -> {
            try {
                String cameraId = null;
                cameraId = cameraManager.getCameraIdList()[0]; // Use the first camera (rear camera)
                cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId); // Initialize cameraCharacteristics
                streamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                    @Override
                    public void onOpened(@NonNull CameraDevice camera) {
                        cameraDevice = camera;
                        imageReader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
                        imageReader.setOnImageAvailableListener(reader -> {
                            // Handle available images
                        }, null);
                        createCameraPreview(); // Create the camera preview
//                    configureTextureViewSize(height);
                    }

                    @Override
                    public void onDisconnected(@NonNull CameraDevice camera) {
                        // Handle camera disconnect
                    }

                    @Override
                    public void onError(@NonNull CameraDevice camera, int error) {
                        // Handle camera error
                    }
                }, null);
            } catch (
                    CameraAccessException e) {
                e.printStackTrace();
            }
        }, 1000);

        binding.camera.setOnClickListener(v -> captureImage());

    }

    private static Size chooseOptimalSize(Size[] choices, int desiredWidth, int desiredHeight) {
        List<Size> suitableSizes = new ArrayList<>();

        for (Size option : choices) {
            if (option.getWidth() == desiredWidth && option.getHeight() == desiredHeight) {
                return option; // Exact match, return it
            }

            if (option.getHeight() == option.getWidth() * desiredHeight / desiredWidth &&
                    option.getWidth() >= desiredWidth && option.getHeight() >= desiredHeight) {
                suitableSizes.add(option);
            }
        }

        // If no suitable size is found, return the largest available size
        if (suitableSizes.size() > 0) {
            return Collections.min(suitableSizes, new CompareSizesByArea());
        } else {
            return choices[0]; // Fallback to the first available size
        }
    }

    private static class CompareSizesByArea implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs) {
            // Compare sizes by their areas
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() - (long) rhs.getWidth() * rhs.getHeight());
        }
    }


    private void saveCapturedImage(Image img) {
        if (img == null) {
            return;
        }

        // Convert Image to byte array
        ByteBuffer buffer = img.getPlanes()[0].getBuffer();
        byte[] imageBytes = new byte[buffer.remaining()];
        buffer.get(imageBytes);

        // Save the image to storage
        File dir;
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        } else {
            dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        }
        File storageDir = new File(dir, "R_Adams_Driver");
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String newFileName = "JPEG_" + timeStamp + ".jpg";
//        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
//        String imageFileName = "IMG_" + timeStamp + ".jpg";
        File imageFile = new File(storageDir, newFileName);

//      File imageFile = new File(storageDir, name);

        try (OutputStream os = new FileOutputStream(imageFile)) {
            os.write(imageBytes);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Close the image
        img.close();
        String image = imageFile.getAbsolutePath();
        Stash.put("img", image);
        Log.d(TAG, "saveCapturedImage: " + image);
        startActivity(new Intent(CameraActivity.this, PictureResultActivity.class));
        finish();
    }

    private static final String TAG = "CameraActivity";

    private void createCameraPreview() {

        Size[] outputSizes = streamConfigurationMap.getOutputSizes(SurfaceTexture.class);
        Size bestSize = chooseOptimalSize(outputSizes, width, height);

        try {
            SurfaceTexture texture;
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                texture = binding.textureView.getSurfaceTexture();
                texture.setDefaultBufferSize(bestSize.getWidth(), bestSize.getHeight());
                camera(texture);
            } else {
                binding.textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                    @Override
                    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
                        surface.setDefaultBufferSize(bestSize.getWidth(), bestSize.getHeight());
                        camera(surface);
                    }

                    @Override
                    public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {

                    }

                    @Override
                    public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
                        return false;
                    }

                    @Override
                    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {

                    }
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void camera(SurfaceTexture texture) {
        try {
            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureRequestBuilder.addTarget(surface);
            captureRequestBuilder.set(CaptureRequest.JPEG_QUALITY, (byte) 100);
            cameraDevice.createCaptureSession(Collections.singletonList(surface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            if (cameraDevice == null) {
                                return;
                            }

                            cameraCaptureSession = session;
                            // configureAutoFocus();
                            try {
                                captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                                session.setRepeatingRequest(captureRequestBuilder.build(), null, null);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            // Handle configuration failure
                        }
                    }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void captureImage() {
        try {
            // Create an image capture request
            CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(imageReader.getSurface());

            // Set the appropriate image quality parameters
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            captureBuilder.set(CaptureRequest.JPEG_QUALITY, (byte) 100);
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
            captureBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);


            int ROTATION = Stash.getInt(Constants.ROTATION, 90);

            if (isVertical) {
                if (ROTATION == 0) {
                    captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, 90);
                } else if (ROTATION == 180) {
                    captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, 270);
                }
            } else {
                if (ROTATION == 90) {
                    captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, 180);
                }
            }

            // Create a CaptureCallback to handle the capture result
            CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    Image capturedImage = imageReader.acquireLatestImage();
                    saveCapturedImage(capturedImage);
                }
            };

            // Create a CaptureSession and capture the image
            cameraDevice.createCaptureSession(Arrays.asList(imageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try {
                        session.capture(captureBuilder.build(), captureCallback, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    // Handle configuration failure
                }
            }, null);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
}