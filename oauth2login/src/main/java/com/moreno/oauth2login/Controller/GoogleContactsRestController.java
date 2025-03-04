package com.moreno.oauth2login.Controller;

import com.moreno.oauth2login.Service.GoogleContactsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.services.people.v1.model.EmailAddress;
import com.google.api.services.people.v1.model.Name;
import com.google.api.services.people.v1.model.Person;
import com.google.api.services.people.v1.model.PhoneNumber;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/contacts")
public class GoogleContactsRestController {

    private final GoogleContactsService googleContactsService;

    public GoogleContactsRestController(GoogleContactsService googleContactsService) {
        this.googleContactsService = googleContactsService;
    }

    // retrieve all contacts
    @GetMapping
    public ResponseEntity<List<Person>> getAllContacts(Authentication authentication) {
        try {
            List<Person> contacts = googleContactsService.getGoogleContacts(authentication);
            return  ResponseEntity.ok(contacts);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(null);
        }
    }

    // add a new contact
    @PostMapping
    public ResponseEntity<?> addContact(Authentication authentication, @RequestBody Person newContact) {
        try {
            Person createdContact = googleContactsService.addGoogleContact(authentication, newContact);
            return ResponseEntity.ok(createdContact);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error adding contact: " + e.getMessage());
        }
    }

    // update an existing contact
    @PutMapping
    public ResponseEntity<?> updateContact(
            Authentication authentication,
            @RequestParam String resourceName,
            @RequestBody Map<String, Object> updatedContactData // Receives data as a Map
    ) {
        try {
            System.out.println("Update Request Received for: " + resourceName);
            System.out.println("Received Data: " + updatedContactData);

            // Extract name safely
            String givenName = "";
            if (updatedContactData.containsKey("names")) {
                List<Map<String, Object>> namesList = (List<Map<String, Object>>) updatedContactData.get("names");
                if (namesList != null && !namesList.isEmpty()) {
                    givenName = (String) namesList.get(0).get("givenName");
                }
            }

            // Extract email safely
            String email = "";
            if (updatedContactData.containsKey("emailAddresses")) {
                List<Map<String, Object>> emailList = (List<Map<String, Object>>) updatedContactData.get("emailAddresses");
                if (emailList != null && !emailList.isEmpty()) {
                    email = (String) emailList.get(0).get("value");
                }
            }

            String phoneNumber = "";
            if (updatedContactData.containsKey("phoneNumbers")) {
                List <Map<String, Object>> phoneList = (List<Map<String, Object>>) updatedContactData.get("phoneNumbers");
                if(phoneList != null && !phoneList.isEmpty()) {
                    phoneNumber = (String) phoneList.get(0).get("value");
                }
            }

            // Create a Person object with extracted data
            Person updatedContact = new Person()
                    .setNames(Collections.singletonList(new Name().setGivenName(givenName)))
                    .setEmailAddresses(Collections.singletonList(new EmailAddress().setValue(email)))
                    .setPhoneNumbers(Collections.singletonList(new PhoneNumber().setValue(phoneNumber)));

            // Pass the updated Person object to the service
            Person updated = googleContactsService.updateGoogleContact(authentication, resourceName, updatedContact);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error updating contact: " + e.getMessage());
        }
    }



    // delete a contact
    @DeleteMapping("/**")
    public ResponseEntity<?> deleteContact(Authentication authentication, HttpServletRequest request) {
        try {
            // Extract the full path after /api/contacts/
            String fullPath = request.getRequestURI();
            String resourceName = fullPath.substring("/api/contacts/".length());

            // Log what we're trying to delete
            System.out.println("Attempting to delete resource: " + resourceName);

            googleContactsService.deleteGoogleContact(authentication, resourceName);
            return ResponseEntity.ok("Contact deleted successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error deleting contact: " + e.getMessage());
        }
    }
}
