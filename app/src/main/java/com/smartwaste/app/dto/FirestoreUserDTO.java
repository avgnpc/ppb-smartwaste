package com.smartwaste.app.dto;

import com.smartwaste.app.model.User;

public class FirestoreUserDTO {
    private String uid;
    private String email;
    private String location;
    private long createdAt;
    private String firstName;
    private String lastName;
    private String birthDate;
    private String profileImage;

    // Required empty constructor for Firestore
    public FirestoreUserDTO() {}

    // Getters and Setters
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getBirthDate() { return birthDate; }
    public void setBirthDate(String birthDate) { this.birthDate = birthDate; }

    public String getProfileImage() { return profileImage; }
    public void setProfileImage(String profileImage) { this.profileImage = profileImage; }

    // Convert to domain model
    public User toUser() {
        return new User(uid, firstName, lastName, email, birthDate, createdAt, location, profileImage);
    }

    public static FirestoreUserDTO fromUser(User user) {
        FirestoreUserDTO dto = new FirestoreUserDTO();
        dto.setUid(user.getUid());
        dto.setEmail(user.getEmail());
        dto.setLocation(user.getLocation());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setBirthDate(user.getBirthDate());
        dto.setProfileImage(user.getProfileImage());
        return dto;
    }
}
