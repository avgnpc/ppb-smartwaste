package com.smartwaste.app.services;

import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Credentials;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONObject;

public class ImageKitService {
    private static final String TAG = "ImageKitService";
    private static final String UPLOAD_URL = "https://upload.imagekit.io/api/v1/files/upload";
    private static final String PRIVATE_API_KEY = "private_+FSQIw1s7z93vV3Akvg1i/r4ptU="; // Replace this

    public interface UploadCallback {
        void onSuccess(String imageUrl);
        void onFailure(Exception e);
    }

    public void uploadImage(Bitmap bitmap, String fileName, UploadCallback callback) {
        OkHttpClient client = new OkHttpClient();

        // Convert Bitmap to base64
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
        byte[] imageBytes = outputStream.toByteArray();
        String base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP);

        // Multipart body
        MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);
        builder.addFormDataPart("file", base64Image);
        builder.addFormDataPart("fileName", fileName);
        builder.addFormDataPart("folder", "/smartwaste");
        builder.addFormDataPart("tags", "smartwaste,android");

        RequestBody requestBody = builder.build();

        // Basic Auth
        String credential = Credentials.basic(PRIVATE_API_KEY, "");

        Request request = new Request.Builder()
                .url(UPLOAD_URL)
                .addHeader("Authorization", credential)
                .post(requestBody)
                .build();

        // Async call
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Upload failed: ", e);
                callback.onFailure(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    try {
                        JSONObject json = new JSONObject(responseBody);
                        String imageUrl = json.getString("url");
                        Log.d(TAG, "Upload successful: " + imageUrl);
                        callback.onSuccess(imageUrl);
                    } catch (Exception e) {
                        callback.onFailure(e);
                    }
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    Log.e(TAG, "Upload failed: " + errorBody);
                    callback.onFailure(new Exception("Upload failed: " + errorBody));
                }
            }
        });
    }
}
