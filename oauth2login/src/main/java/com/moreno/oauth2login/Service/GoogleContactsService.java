package com.moreno.oauth2login.Service;

import com.google.api.services.people.v1.PeopleService;
import com.google.api.services.people.v1.model.*;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;

import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class GoogleContactsService {

    private final OAuth2AuthorizedClientService authorizedClientService;

    public GoogleContactsService(OAuth2AuthorizedClientService authorizedClientService) {
        this.authorizedClientService = authorizedClientService;
    }

    public List<Person> getGoogleContacts(Authentication authentication) throws IOException, GeneralSecurityException {
        if (!(authentication instanceof OAuth2AuthenticationToken oauthToken)) {
            throw new IllegalStateException("User is not authenticated with OAuth2.");
        }

        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient("google", authentication.getName());

        if (client == null) {
            throw new IllegalStateException("OAuth2AuthorizedClient is null. User might not be authenticated properly.");
        }

        String accessToken = client.getAccessToken().getTokenValue();
        GoogleCredentials credentials = GoogleCredentials.create(new AccessToken(accessToken, null));

        HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

        PeopleService peopleService = new PeopleService.Builder(httpTransport, jsonFactory, new HttpCredentialsAdapter(credentials))
                .setApplicationName("Google Contacts App")
                .build();

        // ✅ Now requests phone numbers too
        ListConnectionsResponse response = peopleService.people().connections()
                .list("people/me")
                .setPageSize(10)
                .setPersonFields("names,emailAddresses,phoneNumbers")
                .execute();

        return response.getConnections();
    }

    public Person addGoogleContact(Authentication authentication, Person newContact) throws IOException, GeneralSecurityException {
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient("google", authentication.getName());
        if (client == null) {
            throw new IllegalStateException("OAuth2AuthorizedClient is null. User might not be authenticated properly.");
        }

        String accessToken = client.getAccessToken().getTokenValue();
        GoogleCredentials credentials = GoogleCredentials.create(new AccessToken(accessToken, null));

        HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

        PeopleService peopleService = new PeopleService.Builder(httpTransport, jsonFactory, new HttpCredentialsAdapter(credentials))
                .setApplicationName("Google Contacts App")
                .build();

        // Extract values safely
        String givenName = "";
        String email = "";
        String phoneNumber = "";

        if (newContact.getNames() != null && !newContact.getNames().isEmpty()) {
            Object nameObj = newContact.getNames().get(0);
            if (nameObj instanceof Name) {
                givenName = ((Name) nameObj).getGivenName();
            } else if (nameObj instanceof Map) {
                givenName = (String) ((Map<?, ?>) nameObj).get("givenName");
            }
        }

        if (newContact.getEmailAddresses() != null && !newContact.getEmailAddresses().isEmpty()) {
            Object emailObj = newContact.getEmailAddresses().get(0);
            if (emailObj instanceof EmailAddress) {
                email = ((EmailAddress) emailObj).getValue();
            } else if (emailObj instanceof Map) {
                email = (String) ((Map<?, ?>) emailObj).get("value");
            }
        }

        if (newContact.getPhoneNumbers() != null && !newContact.getPhoneNumbers().isEmpty()) {
            Object phoneObj = newContact.getPhoneNumbers().get(0);
            if (phoneObj instanceof PhoneNumber) {
                phoneNumber = ((PhoneNumber) phoneObj).getValue();
            } else if (phoneObj instanceof Map) {
                phoneNumber = (String) ((Map<?, ?>) phoneObj).get("value");
            }
        }

        // Create a new Person object with proper structure
        Person contactToCreate = new Person()
                .setNames(Collections.singletonList(new Name().setGivenName(givenName)))
                .setEmailAddresses(Collections.singletonList(new EmailAddress().setValue(email)))
                .setPhoneNumbers(Collections.singletonList(new PhoneNumber().setValue(phoneNumber))); // Ensure correct phone number structure

        return peopleService.people().createContact(contactToCreate).execute();
    }


    public Person updateGoogleContact(Authentication authentication, String resourceName, Person updatedContact) throws IOException, GeneralSecurityException {
        if (updatedContact == null) {
            throw new IllegalArgumentException("Updated contact data cannot be null");
        }

        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient("google", authentication.getName());
        if (client == null) {
            throw new IllegalStateException("OAuth2AuthorizedClient is null. User might not be authenticated properly.");
        }

        String accessToken = client.getAccessToken().getTokenValue();
        GoogleCredentials credentials = GoogleCredentials.create(new AccessToken(accessToken, null));

        PeopleService peopleService = new PeopleService.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName("Google Contacts App")
                .build();

        // ✅ Only request valid fields
        Person existingContact = peopleService.people().get(resourceName)
                .setPersonFields("names,emailAddresses,phoneNumbers,metadata") // etag is included in metadata
                .execute();



        // ✅ Extract etag from metadata correctly
        String etag = existingContact.getEtag();
        updatedContact.setEtag(etag);

        return peopleService.people().updateContact(resourceName, updatedContact)
                .setUpdatePersonFields("names,emailAddresses,phoneNumbers") // Now includes phoneNumbers
                .execute();
    }




    public void deleteGoogleContact(Authentication authentication, String resourceName) throws IOException, GeneralSecurityException {
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient("google", authentication.getName());
        if (client == null) {
            throw new IllegalStateException("OAuth2AuthorizedClient is null. User might not be authenticated properly.");
        }

        String accessToken = client.getAccessToken().getTokenValue();
        GoogleCredentials credentials = GoogleCredentials.create(new AccessToken(accessToken, null));

        HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

        PeopleService peopleService = new PeopleService.Builder(httpTransport, jsonFactory, new HttpCredentialsAdapter(credentials))
                .setApplicationName("Google Contacts App")
                .build();

        System.out.println("Calling Google API to delete: " + resourceName);

        try {
            peopleService.people().deleteContact(resourceName).execute();
        } catch (Exception e) {
            System.err.println("Error from Google API: " + e.getMessage());
            throw e;
        }
    }
}
