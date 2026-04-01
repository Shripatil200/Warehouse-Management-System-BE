package com.infotact.warehouse.dto.v1.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor // <-- Hibernate NEEDS this to match the query
public class UserResponse {
    private String id;
    private String name;
    private String email;
    private String contactNumber;
    private String status;
    private Object role; // Use the exact type of your role field in User (e.g., Role enum)
}