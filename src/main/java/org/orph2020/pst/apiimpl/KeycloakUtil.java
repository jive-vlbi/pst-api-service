package org.orph2020.pst.apiimpl;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.Gson ;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import java.util.List;
import java.util.ArrayList;

import jakarta.ws.rs.WebApplicationException;
import jakarta.enterprise.context.RequestScoped;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import org.jboss.logging.Logger;

import org.ivoa.dm.proposal.prop.InvestigatorKind;

@RequestScoped
public class KeycloakUtil {
    private static final Logger logger = Logger.getLogger(KeycloakUtil.class);
    
    @Inject @ConfigProperty(name = "polaris-realm-name")      String realm;
    @Inject @ConfigProperty(name = "keycloak.admin-username") String adminUsername;
    @Inject @ConfigProperty(name = "keycloak.admin-password") String adminPassword;
    @Inject @ConfigProperty(name = "auth-server-master")      String serverUrl;

    private HttpClient httpClient;
    private String accessToken;
    private Gson gson;
    
    @PostConstruct
    private void init() {
        logger.debug("Initializing KeycloakUtil for: " + serverUrl + " " + realm);
        httpClient = HttpClient.newHttpClient();
        gson = new Gson();
        authenticate();
    }
    
    public void createProposal(Long proposalId) {
        try {
            logger.debug("Creating proposal for ID: " + proposalId);
            
            // Create roles
            String investigatorRoleName = "proposal_" + proposalId + "_investigator";
            String piRoleName = "proposal_" + proposalId + "_pi";
            
            createRole(investigatorRoleName);
            createRole(piRoleName);
            
            // Get investigators group
            GroupRepresentation investigatorsGroup = findGroupByName("investigators");
            
            // Create child groups
            GroupRepresentation proposalInvestigatorsGroup = createChildGroup(
                investigatorsGroup.getId(), "investigators of " + proposalId);
            
            GroupRepresentation proposalPisGroup = createChildGroup(
                proposalInvestigatorsGroup.getId(), "PIs of " + proposalId);
            
            // Assign roles to groups
            assignRoleToGroup(proposalInvestigatorsGroup.getId(), investigatorRoleName);
            assignRoleToGroup(proposalPisGroup.getId(), piRoleName);
            
            logger.debug("Successfully created Keycloak groups for proposal: " + proposalId);
        } catch (Exception e) {
            logger.error("Error creating proposal: " + e.getMessage(), e);
            throw new WebApplicationException("Error creating proposal: " + e.getMessage());
        }
    }

    public void deleteProposal(Long proposalId) {
        try {
            logger.debug("Deleting proposal: " + proposalId);
            
            // will also delete the child group for PIs
            GroupRepresentation proposalInvestigators = findGroupByName("investigators of " + proposalId);
            deleteGroup(proposalInvestigators.getId());

            deleteRole("proposal_" + proposalId + "_investigator");
            deleteRole("proposal_" + proposalId + "_pi");
            
            logger.debug("Successfully deleted proposal: " + proposalId);
        } catch (Exception e) {
            logger.error("Error deleting proposal: " + e.getMessage(), e);
            throw new WebApplicationException("Error deleting proposal: " + e.getMessage());
        }
    }

    public void addToProposal(Long proposalId,
                              String keycloakUserId,
                              InvestigatorKind investigatorKind) {
        try {
            logger.debug("Adding user to proposal: " + proposalId + ", user: " + keycloakUserId);
            
            GroupRepresentation proposalInvestigators;
            if (investigatorKind == InvestigatorKind.PI) {
                proposalInvestigators = findGroupByName("PIs of " + proposalId);
            } else {
                proposalInvestigators = findGroupByName("investigators of " + proposalId);
            }
            
            addUserToGroup(keycloakUserId, proposalInvestigators.getId());
            
            logger.debug("Successfully added user to proposal: " + proposalId);
        } catch (Exception e) {
            logger.error("Error adding user to proposal: " + e.getMessage(), e);
            throw new WebApplicationException("Error adding user to proposal: " + e.getMessage());
        }
    }

    public void removeFromProposal(Long proposalId,
                                   String keycloakUserId,
                                   InvestigatorKind investigatorKind) {
        try {
            logger.debug("Removing user from proposal: " + proposalId + ", user: " + keycloakUserId);
            
            GroupRepresentation proposalInvestigators;
            if (investigatorKind == InvestigatorKind.PI) {
                proposalInvestigators = findGroupByName("PIs of " + proposalId);
            } else {
                proposalInvestigators = findGroupByName("investigators of " + proposalId);
            }
            
            removeUserFromGroup(keycloakUserId, proposalInvestigators.getId());
            
            logger.debug("Successfully removed user from proposal: " + proposalId);
        } catch (Exception e) {
            logger.error("Error removing user from proposal: " + e.getMessage(), e);
            throw new WebApplicationException("Error removing user from proposal: " + e.getMessage());
        }
    }

    public void setInvestigatorType(String keycloakUserId,
                                    InvestigatorKind targetRole,
                                    Long proposalId) {
        try {
            GroupRepresentation proposalInvestigators = findGroupByName("investigators of " + proposalId);
            GroupRepresentation proposalPis = findGroupByName("PIs of " + proposalId);
            
            GroupRepresentation addToGroup = (targetRole == InvestigatorKind.PI ? proposalPis : proposalInvestigators);
            GroupRepresentation removeFromGroup = (targetRole == InvestigatorKind.PI ? proposalInvestigators : proposalPis);
            
            addUserToGroup(keycloakUserId, addToGroup.getId());
            removeUserFromGroup(keycloakUserId, removeFromGroup.getId());
            
        } catch (Exception e) {
            throw new WebApplicationException("Error setting investigator type: " + e.getMessage());
        }
    }

    private void authenticate() {
        try {
            String tokenUrl = serverUrl + "/realms/" + realm + "/protocol/openid-connect/token";
            String formData = "grant_type=password&client_id=admin-cli"+ 
                "&username=" + adminUsername +
                "&password=" + adminPassword;
            
            logger.debug("Authenticating to: " + tokenUrl);
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(tokenUrl))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(BodyPublishers.ofString(formData))
                .build();
                
            HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
            
            logger.debug("Authentication response status: " + response.statusCode());
            logger.debug("Authentication response body: " + response.body());
            
            if (response.statusCode() != 200) {
                throw new WebApplicationException("Authentication failed: " + response.statusCode());
            }
            
            JsonObject tokenResponse = JsonParser.parseString(response.body()).getAsJsonObject();
            accessToken = tokenResponse.get("access_token").getAsString();
            logger.debug("Successfully obtained access token");
            
        } catch (Exception e) {
            logger.error("Authentication error: " + e.getMessage(), e);
            throw new WebApplicationException("Authentication error: " + e.getMessage());
        }
    }

    private HttpRequest.Builder authenticatedRequest(String path) {
        return HttpRequest.newBuilder()
            .uri(URI.create(serverUrl + "/admin/realms/" + realm + path))
            .header("Authorization", "Bearer " + accessToken)
            .header("Content-Type", "application/json");
    }
    
    private void createRole(String roleName) {
        try {
            JsonObject roleData = new JsonObject();
            roleData.addProperty("name", roleName);
            
            HttpRequest request = authenticatedRequest("/roles")
                .POST(BodyPublishers.ofString(roleData.toString()))
                .build();
                
            HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
            
            if (response.statusCode() != 201) {
                throw new WebApplicationException("Failed to create role: " + response.statusCode());
            }
            
        } catch (Exception e) {
            throw new WebApplicationException("Error creating role: " + e.getMessage());
        }
    }

    private void deleteRole(String roleName) {
        try {
            HttpRequest request = authenticatedRequest("/roles/" + roleName)
                .DELETE()
                .build();
                
            HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
            
            if (response.statusCode() != 204) {
                throw new WebApplicationException("Failed to delete role: " + response.statusCode());
            }
            
        } catch (Exception e) {
            throw new WebApplicationException("Error deleting role: " + e.getMessage());
        }
    }
    
    private GroupRepresentation findGroupByName(String groupName) {
        try {
            HttpRequest request = authenticatedRequest("/groups?populateHierarchy=false&exact=true&search=" + URLEncoder.encode(groupName, StandardCharsets.UTF_8))
                .GET()
                .build();
                
            HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                throw new WebApplicationException("Failed to find group: " + response.statusCode());
            }
            
            JsonArray groupsArray = JsonParser.parseString(response.body()).getAsJsonArray();
            
            if (groupsArray.size() < 1) {
                throw new WebApplicationException("Group not found: " + groupName);
            }
            else if (groupsArray.size() > 1) {
                throw new WebApplicationException("Multiple groups with the same name found for: " + groupName);
            }
            else {
                return gson.fromJson(groupsArray.get(0), GroupRepresentation.class);
            }
            
        } catch (Exception e) {
            throw new WebApplicationException("Error finding group: " + e.getMessage());
        }
    }
    
    private GroupRepresentation createChildGroup(String parentGroupId, String childGroupName) {
        try {
            JsonObject groupData = new JsonObject();
            groupData.addProperty("name", childGroupName);
            
            HttpRequest request = authenticatedRequest("/groups/" + parentGroupId + "/children")
                .POST(BodyPublishers.ofString(groupData.toString()))
                .build();
                
            HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
            
            if (response.statusCode() != 201) {
                throw new WebApplicationException("Failed to create child group: " + response.statusCode());
            }
            
            String location = response.headers().firstValue("Location").orElse("");
            String groupId = location.substring(location.lastIndexOf("/") + 1);
            
            GroupRepresentation newGroup = new GroupRepresentation();
            newGroup.setId(groupId);
            newGroup.setName(childGroupName);
            
            return newGroup;
            
        } catch (Exception e) {
            throw new WebApplicationException("Error creating child group: " + e.getMessage());
        }
    }

    private void deleteGroup(String groupId) {
        try {
            HttpRequest request = authenticatedRequest("/groups/" + groupId)
                .DELETE()
                .build();
                
            HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
            
            if (response.statusCode() != 204) {
                throw new WebApplicationException("Failed to delete group: " + response.statusCode());
            }
            
        } catch (Exception e) {
            throw new WebApplicationException("Error deleting group: " + e.getMessage());
        }
    }
    
    private void assignRoleToGroup(String groupId, String roleName) {
        try {
            // Get role details
            HttpRequest roleRequest = authenticatedRequest("/roles/" + roleName)
                .GET()
                .build();
                
            HttpResponse<String> roleResponse = httpClient.send(roleRequest, BodyHandlers.ofString());
            
            if (roleResponse.statusCode() != 200) {
                throw new WebApplicationException("Failed to get role: " + roleResponse.statusCode());
            }
            
            JsonObject roleJson = JsonParser.parseString(roleResponse.body()).getAsJsonObject();
            
            JsonArray rolesArray = new JsonArray();
            rolesArray.add(roleJson);
            
            HttpRequest request = authenticatedRequest("/groups/" + groupId + "/role-mappings/realm")
                .POST(BodyPublishers.ofString(rolesArray.toString()))
                .build();
                
            HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
            
            if (response.statusCode() != 204) {
                throw new WebApplicationException("Failed to assign role to group: " + response.statusCode());
            }
            
        } catch (Exception e) {
            throw new WebApplicationException("Error assigning role to group: " + e.getMessage());
        }
    }
    
    private void addUserToGroup(String userId, String groupId) {
        try {
            HttpRequest request = authenticatedRequest("/users/" + userId + "/groups/" + groupId)
                .PUT(BodyPublishers.noBody())
                .build();
                
            HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
            
            if (response.statusCode() != 204) {
                throw new WebApplicationException("Failed to add user to group: " + response.statusCode());
            }
            
        } catch (Exception e) {
            throw new WebApplicationException("Error adding user to group: " + e.getMessage());
        }
    }
    
    private void removeUserFromGroup(String userId, String groupId) {
        try {
            HttpRequest request = authenticatedRequest("/users/" + userId + "/groups/" + groupId)
                .DELETE()
                .build();
                
            HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
            
            if (response.statusCode() != 204) {
                throw new WebApplicationException("Failed to remove user from group: " + response.statusCode());
            }
            
        } catch (Exception e) {
            throw new WebApplicationException("Error removing user from group: " + e.getMessage());
        }
    }
    
    // POJO classes
    public static class RoleRepresentation {
        private String id;
        private String name;
        private String description;
        private boolean composite = false;
        private boolean clientRole = false;
        private String containerId;
        
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public boolean isComposite() { return composite; }
        public void setComposite(boolean composite) { this.composite = composite; }
        
        public boolean isClientRole() { return clientRole; }
        public void setClientRole(boolean clientRole) { this.clientRole = clientRole; }
        
        public String getContainerId() { return containerId; }
        public void setContainerId(String containerId) { this.containerId = containerId; }
    }
    
    public static class GroupRepresentation {
        private String id;
        private String name;
        private String path;
        private List<GroupRepresentation> subGroups;
        
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        
        public List<GroupRepresentation> getSubGroups() { return subGroups; }
        public void setSubGroups(List<GroupRepresentation> subGroups) { this.subGroups = subGroups; }
    }

}
    
