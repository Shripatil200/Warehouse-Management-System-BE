package com.infotact.warehouse.dto.v1.response;

import com.infotact.warehouse.entity.User;
import com.infotact.warehouse.entity.enums.Role;
import com.infotact.warehouse.entity.enums.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * Data Transfer Object representing a user profile within the warehouse ecosystem.
 * <p>
 * This response provides a high-level view of staff metadata, including their
 * current operational status and system permissions (Role).
 * Sensitive data such as passwords or internal security tokens are explicitly excluded.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "UserResponse",
        description = "Public profile information for warehouse staff and administrators"
)
public class UserResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "The unique UUID of the user",
            example = "550e8400-e29b-41d4-a716-446655440000")
    private String id;

    @Schema(description = "Full legal name of the user",
            example = "Amit Sharma")
    private String name;

    @Schema(description = "Registered work email address",
            example = "amit.s@infotact.com")
    private String email;

    @Schema(description = "Primary 10-digit mobile contact number",
            example = "9876543210")
    private String contactNumber;

    @Schema(description = "Current account state",
            example = "ACTIVE", allowableValues = {"ACTIVE", "INACTIVE", "DELETED"})
    private UserStatus status;

    @Schema(description = "System-level permissions role",
            example = "MANAGER", allowableValues = {"ADMIN", "MANAGER", "EMPLOYEE"})
    private Role role;

    /**
     * Convenience constructor for mapping the User JPA Entity to this DTO.
     * * @param user The source persistence entity.
     */
    public UserResponse(User user) {
        this.id = user.getId();
        this.name = user.getName();
        this.email = user.getEmail();
        this.contactNumber = user.getContactNumber();
        this.status = user.getStatus();
        this.role = user.getRole();
    }
}