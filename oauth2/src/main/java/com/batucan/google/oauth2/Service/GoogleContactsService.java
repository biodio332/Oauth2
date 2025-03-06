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

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class GoogleContactsService {

    private final RestTemplate restTemplate = new RestTemplate();

    // ✅ Fetch all contacts (Still using RestTemplate)
    public List<Contact> getContacts(String accessToken) {
        String url = "https://people.googleapis.com/v1/people/me/connections?personFields=names,emailAddresses,metadata";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<GoogleContactsResponse> response = restTemplate.exchange(url, HttpMethod.GET, entity, GoogleContactsResponse.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                GoogleContactsResponse contactsResponse = response.getBody();
                System.out.println("API Response: " + contactsResponse);

                if (contactsResponse.getConnections() == null) {
                    return List.of();
                }

                return contactsResponse.getConnections().stream()
                        .map(person -> new Contact(
                                person.getResourceName(),
                                (person.getNames() != null && !person.getNames().isEmpty())
                                        ? person.getNames().get(0).getDisplayName()
                                        : "Unknown",
                                (person.getEmailAddresses() != null && !person.getEmailAddresses().isEmpty())
                                        ? person.getEmailAddresses().get(0).getValue()
                                        : "No Email"
                        ))
                        .toList();
            }
        } catch (Exception e) {
            System.out.println("❌ Error fetching contacts: " + e.getMessage());
        }

        return List.of();
    }

    // ✅ Fetch a single contact
    public Contact getContactById(String resourceName, String accessToken) {
        String url = "https://people.googleapis.com/v1/" + resourceName + "?personFields=names,emailAddresses";

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

    // ✅ Add a new contact
    public void addContact(Contact contact, String accessToken) {
        String url = "https://people.googleapis.com/v1/people:createContact";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("names", List.of(Map.of("givenName", contact.getName())));
        requestBody.put("emailAddresses", List.of(Map.of("value", contact.getEmail())));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            System.out.println("✅ Add Contact Response: " + response.getBody());
        } catch (Exception e) {
            System.out.println("❌ Error adding contact: " + e.getMessage());
        }
    }

    // ✅ Update a contact using CloseableHttpClient
    public void updateContact(String resourceName, Contact contact, String accessToken) {
        String url = "https://people.googleapis.com/v1/" + resourceName + "?updateMask=names,emailAddresses";

        HttpPatch httpPatch = new HttpPatch(url);
        httpPatch.setHeader("Authorization", "Bearer " + accessToken);
        httpPatch.setHeader("Content-Type", "application/json");
        

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonBody = objectMapper.writeValueAsString(Map.of(
                    "names", List.of(Map.of("givenName", contact.getName())),
                    "emailAddresses", List.of(Map.of("value", contact.getEmail()))
            ));
            httpPatch.setEntity(new StringEntity(jsonBody, StandardCharsets.UTF_8));

            try (CloseableHttpResponse response = httpClient.execute(httpPatch)) {
                InputStream responseStream = response.getEntity().getContent();
                System.out.println("✅ Contact updated successfully: " + new String(responseStream.readAllBytes(), StandardCharsets.UTF_8));
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
            System.out.println("📝 Metadata Response: " + metadataResponse.getBody());

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
