package com.sep490.ecoverse_be.dto.response;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ListParentResponse {
    private UUID parentId;
    private String fullName;
    private String phoneNumber;
    private String parentEmail;
}
