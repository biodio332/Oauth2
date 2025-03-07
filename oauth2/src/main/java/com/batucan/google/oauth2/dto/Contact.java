package com.batucan.google.oauth2.dto;

public class Contact {
    private String resourceName;
    private String etag;
    private String name;
    private String email;
    private String phoneNumber;  // New field
    

    public Contact() {}

    public Contact(String resourceName, String etag, String name, String email, String phoneNumber, String birthday) {
        this.resourceName = resourceName;
        this.etag = etag;
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public String getEtag() {
        return etag;
    }

    public void setEtag(String etag) {
        this.etag = etag;
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

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    


    @Override
    public String toString() {
        return "Contact{" +
                "resourceName='" + resourceName + '\'' +
                ", etag='" + etag + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", birthday='" +  + '\'' +
                '}';
    }
}
