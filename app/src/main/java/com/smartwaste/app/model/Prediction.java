// File: app/src/main/java/com/smartwaste/app/model/Prediction.java
package com.smartwaste.app.model;

public class Prediction {
    public float x;          // Center X (normalized or in pixels)
    public float y;          // Center Y
    public float width;      // Width of box
    public float height;     // Height of box
    public String label;     // Class name (e.g., "Plastic")
    public float confidence; // [0.0 .. 1.0]

    public Prediction(float x, float y, float w, float h, String label, float conf) {
        this.x = x;
        this.y = y;
        this.width = w;
        this.height = h;
        this.label = label;
        this.confidence = conf;
    }
}
