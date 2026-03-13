package com.sep490.ecoverse_be.dto.response;

import com.sep490.ecoverse_be.enums.AccountStatus;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ParentAdminDetail implements AdminUserDetail {

    private UUID parentId;
    private String fullName;
    private String phoneNumber;
    private String email;
    private AccountStatus accountStatus;
    private Boolean isActive;
    private List<String> schoolNames;
}
