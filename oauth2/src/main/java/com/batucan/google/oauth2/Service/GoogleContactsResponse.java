package com.batucan.google.oauth2.Service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GoogleContactsResponse {
    private List<Person> connections;

    public List<Person> getConnections() {
        return connections;
    }

    public void setConnections(List<Person> connections) {
        this.connections = connections;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Person {
    	private String resourceName; 
        private List<Name> names;
        private List<EmailAddress> emailAddresses;
        
        public String getResourceName() {  
            return resourceName;
        }

        public List<Name> getNames() {
            return names;
        }

        public List<EmailAddress> getEmailAddresses() {
            return emailAddresses;
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Name {
            private String displayName;

            public String getDisplayName() {
                return displayName;
            }
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class EmailAddress {
            private String value;

            public String getValue() {
                return value;
            }
        }
    }
}