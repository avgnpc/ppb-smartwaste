package com.smartwaste.app.analysis;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

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
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Analyzer that sends each camera frame to Roboflow's hosted inference API.
 */
public class RoboflowAnalyzer implements ImageAnalysis.Analyzer {
    private static final MediaType JPEG = MediaType.parse("image/jpeg");

    private final Context context;
    private final OverlayView overlayView;
    private final String roboflowUrl;    // e.g. "https://detect.roboflow.com/model/1"
    private final String roboflowApiKey; // your Roboflow API key

    private final OkHttpClient httpClient;
    private final Handler mainHandler;
    private final Gson gson = new Gson();

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

        // Handler for posting results back to main/UI thread
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    @Override
    public void analyze(ImageProxy imageProxy) {
        Image mediaImage = imageProxy.getImage();
        if (mediaImage == null) {
            imageProxy.close();
            return;
        }

        // 1) Convert ImageProxy to JPEG byte[]
        byte[] jpegBytes = imageProxyToJpeg(mediaImage);

        // 2) Build the multipart request to Roboflow
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                // The field name "file" is what Roboflow expects
                .addFormDataPart("file", "frame.jpg",
                        RequestBody.create(jpegBytes, JPEG))
                .build();

        // 3) Build the full URL including API key
        // e.g. "https://detect.roboflow.com/model/1?api_key=YOUR_KEY"
        String urlWithKey = roboflowUrl + "?api_key=" + roboflowApiKey;

        Request request = new Request.Builder()
                .url(urlWithKey)
                .post(requestBody)
                .header("Accept", "application/json")
                .build();

        // 4) Enqueue Asynchronous call
        httpClient.newCall(request).enqueue(new Callback() {
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // Network error or timeout
                e.printStackTrace();
                // Always close the frame so next frame can be analyzed
                imageProxy.close();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try {
                    if (!response.isSuccessful()) {
                        // Non‐200 status code
                        // Close the response and imageProxy
                        response.close();
                        imageProxy.close();
                        return;
                    }
                    // 5) Parse JSON body into ResponseWrapper
                    String json = response.body().string();
                    response.close();

                    ResponseWrapper wrapper = gson.fromJson(json, ResponseWrapper.class);

                    // 6) Convert to our List<Prediction>
                    List<Prediction> preds = new ArrayList<>();
                    for (Det det : wrapper.predictions) {
                        // Roboflow returns "x", "y", "width", "height" in PIXEL coords
                        preds.add(new Prediction(
                                det.x,
                                det.y,
                                det.width,
                                det.height,
                                det.name,
                                det.confidence
                        ));
                    }

                    // 7) Post the results to overlayView on main thread
                    mainHandler.post(() -> overlayView.setPredictions(preds));
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    // 8) Close the frame so CameraX can hand you the next one
                    imageProxy.close();
                }
            }
        });
    }

    /** Converts an Image (in YUV) to JPEG byte array. */
    private byte[] imageProxyToJpeg(Image mediaImage) {
        // 1) Get YUV planes
        Image.Plane[] planes = mediaImage.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];
        // Y
        yBuffer.get(nv21, 0, ySize);
        // V
        vBuffer.get(nv21, ySize, vSize);
        // U
        uBuffer.get(nv21, ySize + vSize, uSize);

        // 2) Convert NV21 to Bitmap
        YuvImage yuvImage = new YuvImage(
                nv21,
                android.graphics.ImageFormat.NV21,
                mediaImage.getWidth(),
                mediaImage.getHeight(),
                null
        );
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // 3) Compress to JPEG at 80% quality
        yuvImage.compressToJpeg(
                new android.graphics.Rect(0, 0,
                        mediaImage.getWidth(), mediaImage.getHeight()),
                80,
                out
        );
        return out.toByteArray();
    }

    /** Classes to mirror Roboflow's JSON response schema **/
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
