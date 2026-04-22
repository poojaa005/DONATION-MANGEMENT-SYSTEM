package com.ngo.donation_management.entity;

// entity/Ngo.java
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "ngo")
public class Ngo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ngo_id")
    private Integer ngoId;

    @NotBlank(message = "NGO name is required")
    @Size(max = 150, message = "NGO name must be at most 150 characters")
    @Column(name = "ngo_name", nullable = false, length = 150)
    private String ngoName;

    @NotBlank(message = "Address is required")
    @Size(max = 1000, message = "Address must be at most 1000 characters")
    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City must be at most 100 characters")
    @Column(name = "city", length = 100)
    private String city;

    @NotBlank(message = "State is required")
    @Size(max = 100, message = "State must be at most 100 characters")
    @Column(name = "state", length = 100)
    private String state;

    @NotBlank(message = "Phone is required")
    @Pattern(
            regexp = "^[0-9+()\\-\\s]{7,20}$",
            message = "Phone number format is invalid"
    )
    @Column(name = "phone", length = 20)
    private String phone;

    @NotBlank(message = "Email is required")
    @Email(message = "Email format is invalid")
    @Size(max = 150, message = "Email must be at most 150 characters")
    @Column(name = "email", length = 150)
    private String email;

    @NotBlank(message = "Description is required")
    @Size(max = 2000, message = "Description must be at most 2000 characters")
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // Default Constructor
    public Ngo() {
    }

    // Parameterized Constructor
    public Ngo(Integer ngoId, String ngoName, String address, String city,
               String state, String phone, String email, String description) {
        this.ngoId = ngoId;
        this.ngoName = ngoName;
        this.address = address;
        this.city = city;
        this.state = state;
        this.phone = phone;
        this.email = email;
        this.description = description;
    }

    // Getters and Setters
    public Integer getNgoId() {
        return ngoId;
    }

    public void setNgoId(Integer ngoId) {
        this.ngoId = ngoId;
    }

    public String getNgoName() {
        return ngoName;
    }

    public void setNgoName(String ngoName) {
        this.ngoName = normalize(ngoName);
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = normalize(address);
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = normalize(city);
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = normalize(state);
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = normalize(phone);
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = normalize(email);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = normalize(description);
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
