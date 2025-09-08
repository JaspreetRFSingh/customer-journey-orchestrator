package com.journey.orchestrator.api;

import com.journey.orchestrator.model.CustomerProfile;
import com.journey.orchestrator.service.ProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * REST API for profile management.
 */
@RestController
@RequestMapping("/api/v1/profiles")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @PostMapping
    public ResponseEntity<CustomerProfile> createProfile(
            @RequestBody Map<String, Object> request) {
        String customerId = (String) request.get("customerId");
        CustomerProfile.Identity identity = buildIdentity(request);
        Map<String, Object> attributes = (Map<String, Object>) request.get("attributes");
        
        CustomerProfile profile = profileService.createProfile(customerId, identity, attributes);
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/{profileId}")
    public ResponseEntity<CustomerProfile> getProfile(@PathVariable String profileId) {
        Optional<CustomerProfile> profile = profileService.getProfile(profileId);
        return profile.map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<CustomerProfile> getByCustomerId(@PathVariable String customerId) {
        Optional<CustomerProfile> profile = profileService.getProfileByCustomerId(customerId);
        return profile.map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{profileId}/attributes")
    public ResponseEntity<CustomerProfile> updateAttribute(
            @PathVariable String profileId,
            @RequestBody Map<String, Object> request) {
        String key = (String) request.get("key");
        Object value = request.get("value");
        
        CustomerProfile profile = profileService.updateAttribute(profileId, key, value);
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/{profileId}/stats")
    public ResponseEntity<ProfileService.CacheStats> getCacheStats() {
        return ResponseEntity.ok(profileService.getCacheStats());
    }

    private CustomerProfile.Identity buildIdentity(Map<String, Object> request) {
        Map<String, Object> identityData = (Map<String, Object>) request.get("identity");
        if (identityData == null) {
            return CustomerProfile.Identity.builder().build();
        }
        
        return CustomerProfile.Identity.builder()
            .email((String) identityData.get("email"))
            .phone((String) identityData.get("phone"))
            .crmId((String) identityData.get("crmId"))
            .deviceId((String) identityData.get("deviceId"))
            .build();
    }
}
