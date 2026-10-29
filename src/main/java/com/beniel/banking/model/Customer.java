package com.beniel.banking.model;

public class Customer {
    private long customerId;
    private final String name;
    private final String email;
    private Role role;

    public Customer(long customerId, String name, String email, Role role) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name is required.");
        }

        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("A valid email is required.");
        }

        if (role == null) {
            throw new IllegalArgumentException("Role is required.");
        }

        this.customerId = customerId;
        this.name = name.trim();
        this.email = email.trim().toLowerCase();
        this.role = role;
    }

    public long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(long customerId) {
        this.customerId = customerId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        if (role == null) {
            throw new IllegalArgumentException("Role is required.");
        }

        this.role = role;
    }
}