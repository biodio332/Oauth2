package com.batucan.google.oauth2.dto;

public class Contact {
    private String resourceName;  // Add this field
    private String name;
    private String email;

    public Contact() {}

    public Contact(String resourceName, String name, String email) {
        this.resourceName = resourceName;
        this.name = name;
        this.email = email;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String toString() {
        return "Contact{resourceName='" + resourceName + "', name='" + name + "', email='" + email + "'}";
    }
}
