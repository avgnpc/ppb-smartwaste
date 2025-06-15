package com.smartwaste.app.model;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.List;
import java.util.Map;

public class Capture {
    private String id;
    private String timestamp;
    private Map<String, Object> location;
    private List<Map<String, Object>> detections;
    private String imageUrl;
    private String userId;
    private boolean dibersihkan;

    private transient DocumentSnapshot snapshot;

    public Capture() {

    }

    public Capture(String id, String timestamp, Map<String, Object> location,
                   List<Map<String, Object>> detections, String imageUrl,
                   String userId, boolean dibersihkan) {
        this.id = id;
        this.timestamp = timestamp;
        this.location = location;
        this.detections = detections;
        this.imageUrl = imageUrl;
        this.userId = userId;
        this.dibersihkan = dibersihkan;
    }

    // Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public Map<String, Object> getLocation() { return location; }
    public void setLocation(Map<String, Object> location) { this.location = location; }

    public List<Map<String, Object>> getDetections() { return detections; }
    public void setDetections(List<Map<String, Object>> detections) { this.detections = detections; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public boolean isDibersihkan() { return dibersihkan; }
    public void setDibersihkan(boolean dibersihkan) { this.dibersihkan = dibersihkan; }

    public DocumentSnapshot getSnapshot() { return snapshot; }
    public void setSnapshot(DocumentSnapshot snapshot) { this.snapshot = snapshot; }
}
