package com.batucan.google.oauth2.Service;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.batucan.google.oauth2.dto.Contact;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class GoogleContactsService {

    private final RestTemplate restTemplate = new RestTemplate();

    // ✅ Fetch all contacts (Now includes phone numbers & birthdays)
    public List<Contact> getContacts(String accessToken) {
        String url = "https://people.googleapis.com/v1/people/me/connections?personFields=names,emailAddresses,phoneNumbers,birthdays,metadata";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.set("Accept", "application/json"); 
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<GoogleContactsResponse> response = restTemplate.exchange(url, HttpMethod.GET, entity, GoogleContactsResponse.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                GoogleContactsResponse contactsResponse = response.getBody();

                if (contactsResponse.getConnections() == null) {
                    System.out.println("⚠️ No contacts found.");
                    return List.of();
                }

                return contactsResponse.getConnections().stream()
                    .map(person -> {
                        String name = (person.getNames() != null && !person.getNames().isEmpty()) 
                                ? person.getNames().get(0).getDisplayName() : "Unknown";
                        String email = (person.getEmailAddresses() != null && !person.getEmailAddresses().isEmpty()) 
                                ? person.getEmailAddresses().get(0).getValue() : "No Email";
                        String phone = (person.getPhoneNumbers() != null && !person.getPhoneNumbers().isEmpty()) 
                                ? person.getPhoneNumbers().get(0).getValue() : "No Phone";
                        
                        String birthday = "No Birthday";
                        if (person.getBirthdays() != null && !person.getBirthdays().isEmpty()) {
                            GoogleContactsResponse.Person.Date date = person.getBirthdays().get(0).getDate();
                            if (date != null) {
                                birthday = (date.getYear() != null ? date.getYear() + "-" : "") +
                                        String.format("%02d", date.getMonth()) + "-" +
                                        String.format("%02d", date.getDay());
                            }
                        }

                        return new Contact(person.getResourceName(), person.getEtag(), name, email, phone, birthday);
                    })
                    .toList();
            }
        } catch (Exception e) {
            System.out.println("❌ Error fetching contacts: " + e.getMessage());
        }

        return List.of();
    }

    // ✅ Fetch a single contact (Now includes phone & birthday)
    public Contact getContactById(String resourceName, String accessToken) {
        String url = "https://people.googleapis.com/v1/" + resourceName + "?personFields=names,emailAddresses,phoneNumbers,birthdays";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Contact> response = restTemplate.exchange(url, HttpMethod.GET, entity, Contact.class);
            return response.getBody();
        } catch (Exception e) {
            System.out.println("❌ Error fetching contact: " + e.getMessage());
            return null;
        }
    }

    // ✅ Add a new contact (Supports name, email, and phone)
    public void addContact(Contact contact, String accessToken) {
        String url = "https://people.googleapis.com/v1/people:createContact";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("names", List.of(Map.of("givenName", contact.getName())));
        requestBody.put("emailAddresses", List.of(Map.of("value", contact.getEmail())));
        requestBody.put("phoneNumbers", List.of(Map.of("value", contact.getPhoneNumber())));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            System.out.println("✅ Add Contact Response: " + response.getBody());
        } catch (Exception e) {
            System.out.println("❌ Error adding contact: " + e.getMessage());
        }
    }

    // ✅ Update a contact (Supports name, email, and phone)
    public void updateContact(String resourceName, Contact contact, String accessToken) {
        resourceName = java.net.URLDecoder.decode(resourceName, StandardCharsets.UTF_8);
        String url = "https://people.googleapis.com/v1/" + resourceName + 
                     ":updateContact?updatePersonFields=names,emailAddresses,phoneNumbers";

        if (contact.getEtag() == null || contact.getEtag().isEmpty()) {
            System.err.println("❌ Error: etag is missing! Update will fail.");
            return;
        }

        HttpPatch httpPatch = new HttpPatch(url);
        httpPatch.setHeader("Authorization", "Bearer " + accessToken);
        httpPatch.setHeader("Content-Type", "application/json");
        httpPatch.setHeader("Accept", "application/json");

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonBody = objectMapper.writeValueAsString(Map.of(
                    "etag", contact.getEtag(),
                    "names", List.of(Map.of("givenName", contact.getName())),
                    "emailAddresses", List.of(Map.of("value", contact.getEmail())),
                    "phoneNumbers", contact.getPhoneNumber() != null && !contact.getPhoneNumber().isEmpty()
                            ? List.of(Map.of("value", contact.getPhoneNumber()))
                            : Collections.emptyList()
            ));

            httpPatch.setEntity(new StringEntity(jsonBody, StandardCharsets.UTF_8));

            try (CloseableHttpResponse response = httpClient.execute(httpPatch)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String responseString = new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);

                if (statusCode == HttpStatus.OK.value() || statusCode == HttpStatus.NO_CONTENT.value()) {
                    System.out.println("✅ Contact updated successfully: " + resourceName);
                } else {
                    System.err.println("❌ Failed to update contact. Status: " + statusCode + ", Response: " + responseString);
                }
            }
        } catch (Exception e) {
            System.out.println("❌ Error updating contact: " + e.getMessage());
        }
    }

    // ✅ Delete a contact (Checks metadata before deleting)
    public void deleteContact(String resourceName, String accessToken) {
        String metadataUrl = "https://people.googleapis.com/v1/" + resourceName + "?personFields=metadata";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> metadataResponse = restTemplate.exchange(metadataUrl, HttpMethod.GET, entity, Map.class);

            if (metadataResponse.getBody() != null && metadataResponse.getBody().containsKey("metadata")) {
                Map<String, Object> metadata = (Map<String, Object>) metadataResponse.getBody().get("metadata");
                List<Map<String, Object>> sources = (List<Map<String, Object>>) metadata.get("sources");

                if (sources != null && sources.stream().anyMatch(s -> "CONTACT".equals(s.get("type")))) {
                    String deleteUrl = "https://people.googleapis.com/v1/" + resourceName + ":deleteContact";
                    restTemplate.exchange(deleteUrl, HttpMethod.DELETE, entity, Void.class);
                    System.out.println("✅ Contact deleted: " + resourceName);
                } else {
                    System.out.println("⚠️ Error: Contact cannot be deleted (not an owned contact).");
                }
            }
        } catch (Exception e) {
            System.out.println("❌ Error deleting contact: " + e.getMessage());
        }
    }
}
