package com.batucan.google.oauth2.Controller;

import com.batucan.google.oauth2.dto.Contact;
import com.batucan.google.oauth2.Service.GoogleContactsService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
@RequestMapping("/contacts")
public class GoogleContactsController {

    private final GoogleContactsService googleContactsService;
    private final OAuth2AuthorizedClientService authorizedClientService;

    @Autowired
    public GoogleContactsController(GoogleContactsService googleContactsService, 
                                    OAuth2AuthorizedClientService authorizedClientService) {
        this.googleContactsService = googleContactsService;
        this.authorizedClientService = authorizedClientService;
    }

    @GetMapping
    public String getContacts(Model model, @AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return "redirect:/login";
        }

        OAuth2AuthenticationToken authToken = (OAuth2AuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(
                authToken.getAuthorizedClientRegistrationId(),
                authToken.getName()
        );

        String accessToken = authorizedClient.getAccessToken().getTokenValue();
        System.out.println("Access Token: " + accessToken); // DEBUG: Check if token is valid

        List<Contact> contacts = googleContactsService.getContacts(accessToken);
        model.addAttribute("contacts", contacts);
        return "contacts";
    }
    
 // Show Add Contact Form
    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("contact", new Contact());
        return "add-contact";
    }

    // Add New Contact
    @PostMapping("/add")
    public String addContact(@ModelAttribute Contact contact, @AuthenticationPrincipal OAuth2User principal) {
        String accessToken = getAccessToken(principal);
        googleContactsService.addContact(contact, accessToken);
        return "redirect:/contacts";
    }

    // Show Edit Contact Form
    @GetMapping("/edit")
    public String showEditForm(@RequestParam("id") String id, Model model, @AuthenticationPrincipal OAuth2User principal) {
        try {
            id = java.net.URLDecoder.decode(id, StandardCharsets.UTF_8);
            System.out.println("Received Encoded ID: " + id);

            String accessToken = getAccessToken(principal);
            Contact contact = googleContactsService.getContactById(id, accessToken);

            if (contact == null) {
                System.out.println("Error: Contact not found for ID: " + id);
                return "redirect:/contacts?error=notfound";
            }

            model.addAttribute("contact", contact);
            return "edit-contact";
        } catch (Exception e) {
            System.err.println("Error processing edit request: " + e.getMessage());
            return "redirect:/contacts?error=badrequest";
        }
    }


    // Update Contact
    @PostMapping("/edit")
    public String updateContact(@RequestParam("id") String id, @ModelAttribute Contact contact, @AuthenticationPrincipal OAuth2User principal) {
        System.out.println("Received request to update contact with ID: " + id);

        String accessToken = getAccessToken(principal);
        googleContactsService.updateContact(id, contact, accessToken);

        System.out.println("Successfully updated contact with ID: " + id);
        return "redirect:/contacts";
    }

    // Delete Contact
    @GetMapping("/delete")
    public String deleteContact(@RequestParam String id, @AuthenticationPrincipal OAuth2User principal) {
        try {
            id = URLDecoder.decode(id, StandardCharsets.UTF_8);
            System.out.println("Decoded Resource Name: " + id);

            String accessToken = getAccessToken(principal);
            googleContactsService.deleteContact(id, accessToken);
        } catch (Exception e) {
            System.out.println("Error deleting contact: " + e.getMessage());
        }
        return "redirect:/contacts";
    }

    private String getAccessToken(OAuth2User principal) {
        OAuth2AuthenticationToken authToken = (OAuth2AuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(
                authToken.getAuthorizedClientRegistrationId(),
                authToken.getName()
        );
        return authorizedClient.getAccessToken().getTokenValue();
       
    }
}
