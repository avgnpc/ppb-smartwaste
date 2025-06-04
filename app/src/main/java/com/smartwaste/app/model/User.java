package com.smartwaste.app.model;

import com.google.firebase.firestore.PropertyName;

public class User {
    private String uid;
    private String email;
    private String location;
    private long createdAt;

    private String firstName;
    private String lastName;
    private String birthDate;
    private String profileImage;

    public User() {
        // Required empty constructor for Firestore
    }

    public User(String uid, String firstName, String lastName, String email, String birthDate, long createdAt, String location) {
        this.uid = uid;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.birthDate = birthDate;
        this.createdAt = createdAt;
        this.location = location;
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    @PropertyName("first_name")
    public String getFirstName() { return firstName; }

    @PropertyName("first_name")
    public void setFirstName(String firstName) { this.firstName = firstName; }

    @PropertyName("last_name")
    public String getLastName() { return lastName; }

    @PropertyName("last_name")
    public void setLastName(String lastName) { this.lastName = lastName; }

    @PropertyName("birth_date")
    public String getBirthDate() { return birthDate; }

    @PropertyName("birth_date")
    public void setBirthDate(String birthDate) { this.birthDate = birthDate; }

    @PropertyName("created_at")
    public long getCreatedAt() { return createdAt; }

    @PropertyName("created_at")
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public String getProfileImage() { return profileImage; }
    public void setProfileImage(String profileImage) { this.profileImage = profileImage; }

    public String getName() {
        return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
    }
}
