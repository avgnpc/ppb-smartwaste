package com.smartwaste.app.analysis;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.core.app.ActivityCompat;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.smartwaste.app.model.Prediction;
import com.smartwaste.app.ui.views.OverlayView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import android.location.Location;
import android.location.LocationManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;


/**
 * Analyzer that sends each camera frame (rotated upright) to Roboflow's hosted inference API.
 */
public class RoboflowAnalyzer implements ImageAnalysis.Analyzer {
    private static final MediaType JPEG = MediaType.parse("image/jpeg");

    private final Context context;
    private final OverlayView overlayView;
    private final String roboflowUrl;    // e.g. "https://detect.roboflow.com/your-model/1"
    private final String roboflowApiKey; // your Roboflow API key

    private final OkHttpClient httpClient;
    private final Handler mainHandler;
    private final Gson gson = new Gson();
    private final FusedLocationProviderClient fusedLocationClient;


    public RoboflowAnalyzer(Context context,
                            OverlayView overlayView,
                            String roboflowUrl,
                            String roboflowApiKey) {
        this.context = context;
        this.overlayView = overlayView;
        this.roboflowUrl = roboflowUrl;
        this.roboflowApiKey = roboflowApiKey;

        // Create an OkHttpClient with a short timeout
        this.httpClient = new OkHttpClient.Builder()
                .callTimeout(5, TimeUnit.SECONDS)
                .build();

        // Handler to post results back to main/UI thread
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    @Override
    public void analyze(@NonNull ImageProxy imageProxy) {
        Image mediaImage = imageProxy.getImage();
        if (mediaImage == null) {
            imageProxy.close();
            return;
        }

        // 1) Grab rotationDegrees from CameraX
        int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();

        // 2) Convert and rotate the raw YUV image into a JPEG byte[] that is upright
        byte[] jpegBytes = imageProxyToRotatedJpeg(mediaImage, rotationDegrees);
        Log.d("RFAnalyzer", "Sending rotated frame to RF: " +
                (rotationDegrees % 180 == 0
                        ? mediaImage.getWidth() + "×" + mediaImage.getHeight()
                        : mediaImage.getHeight() + "×" + mediaImage.getWidth())
        );

        // 3) Build the multipart request to Roboflow
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                // Roboflow expects the field name "file"
                .addFormDataPart("file", "frame.jpg",
                        RequestBody.create(jpegBytes, JPEG))
                .build();

        // 4) Compose full URL including API key
        String urlWithKey = roboflowUrl + "?api_key=" + roboflowApiKey;

        Request request = new Request.Builder()
                .url(urlWithKey)
                .post(requestBody)
                .header("Accept", "application/json")
                .build();

        // 5) Enqueue asynchronous call
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // Network error or timeout
                e.printStackTrace();
                imageProxy.close();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try {
                    if (!response.isSuccessful()) {
                        response.close();
                        imageProxy.close();
                        return;
                    }
                    // 6) Parse JSON
                    String json = response.body().string();
                    response.close();

                    ResponseWrapper wrapper = gson.fromJson(json, ResponseWrapper.class);

                    // 7) Convert to our List<Prediction>
                    List<Prediction> preds = new ArrayList<>();
                    for (Det det : wrapper.predictions) {
                        preds.add(new Prediction(
                                det.x,
                                det.y,
                                det.width,
                                det.height,
                                det.name,
                                det.confidence
                        ));
                    }

                    // 8) Post predictions to overlay on the main thread
                    mainHandler.post(() -> {
                        overlayView.setPredictions(preds);
                        captureMetadataAndGenerateJson(preds);
                    });


                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    // 9) Always close the frame so CameraX can send the next one
                    imageProxy.close();
                }
            }
        });
    }

    private void captureMetadataAndGenerateJson(List<Prediction> predictions) {
        // Step 1: Get timestamp
        String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                .format(new Date());

        // Step 2: Check location permission first
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w("RoboflowJSON", "Location permission not granted");
            return; // Exit early if permission is not granted
        }

        // Step 3: Get current location
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        JSONObject resultJson = new JSONObject();
                        try {
                            resultJson.put("timestamp", timestamp);

                            JSONObject locationJson = new JSONObject();
                            locationJson.put("latitude", location.getLatitude());
                            locationJson.put("longitude", location.getLongitude());
                            resultJson.put("location", locationJson);

                            // Count trash items
                            Map<String, Integer> trashCounts = new HashMap<>();
                            for (Prediction p : predictions) {
                                trashCounts.put(p.label, trashCounts.getOrDefault(p.label, 0) + 1);
                            }

                            JSONArray trashArray = new JSONArray();
                            for (Map.Entry<String, Integer> entry : trashCounts.entrySet()) {
                                JSONObject item = new JSONObject();
                                item.put("class", entry.getKey());
                                item.put("count", entry.getValue());
                                trashArray.put(item);
                            }

                            resultJson.put("detections", trashArray);

                            Log.d("RoboflowJSON", resultJson.toString(2)); // pretty print

                            // TODO: save to file or send to API

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        Log.w("RoboflowJSON", "Location not available.");
                    }
                })
                .addOnFailureListener(e -> Log.e("RoboflowJSON", "Location fetch failed", e));
    }

    /**
     * Converts an Image (YUV) to an upright JPEG byte array using rotationDegrees.
     * Steps:
     *   1) Copy YUV planes into NV21 byte[].
     *   2) Compress NV21 → temporary JPEG (YuvImage).
     *   3) Decode that JPEG → Bitmap.
     *   4) Rotate Bitmap by rotationDegrees.
     *   5) Compress rotated Bitmap → final JPEG byte[].
     */
    private byte[] imageProxyToRotatedJpeg(Image mediaImage, int rotationDegrees) {
        // 1a) Extract YUV planes into NV21 byte[]
        ByteBuffer yBuffer = mediaImage.getPlanes()[0].getBuffer();
        ByteBuffer uBuffer = mediaImage.getPlanes()[1].getBuffer();
        ByteBuffer vBuffer = mediaImage.getPlanes()[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        // 1b) Build a YuvImage from NV21
        YuvImage yuvImage = new YuvImage(
                nv21,
                android.graphics.ImageFormat.NV21,
                mediaImage.getWidth(),
                mediaImage.getHeight(),
                null
        );

        // 2) Compress YuvImage to a temporary JPEG in memory
        ByteArrayOutputStream tempStream = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(
                new Rect(0, 0, mediaImage.getWidth(), mediaImage.getHeight()),
                80, // JPEG quality
                tempStream
        );
        byte[] tempJpeg = tempStream.toByteArray();

        // 3) Decode that temp JPEG into a Bitmap
        Bitmap unrotatedBmp = BitmapFactory.decodeByteArray(tempJpeg, 0, tempJpeg.length);

        // 4) Rotate the Bitmap by the rotationDegrees
        Matrix matrix = new Matrix();
        matrix.postRotate(rotationDegrees);

        Bitmap rotatedBmp = Bitmap.createBitmap(
                unrotatedBmp,
                0, 0,
                unrotatedBmp.getWidth(),
                unrotatedBmp.getHeight(),
                matrix,
                true
        );

        // 5) Compress the rotated Bitmap into a final JPEG byte[]
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        rotatedBmp.compress(Bitmap.CompressFormat.JPEG, 80, out);
        byte[] rotatedJpegBytes = out.toByteArray();

        // 6) Clean up Bitmaps
        unrotatedBmp.recycle();
        rotatedBmp.recycle();

        return rotatedJpegBytes;
    }

    /** Classes matching Roboflow’s JSON schema **/
    private static class ResponseWrapper {
        @SerializedName("predictions")
        List<Det> predictions;
    }

    private static class Det {
        @SerializedName("x")
        float x;
        @SerializedName("y")
        float y;
        @SerializedName("width")
        float width;
        @SerializedName("height")
        float height;
        @SerializedName("confidence")
        float confidence;
        @SerializedName("class")
        String name;
    }
}
