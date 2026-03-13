package com.sep490.ecoverse_be.dto.response;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ParentAccountInfo {

    private UUID parentId;
    private String parentFullName;
    private String phoneNumber;
    private String password;
    private String parentEmail;
    // true = da gui email thong tin dang nhap, false = chua gui
    private Boolean credentialEmailSent;
    private List<StudentAccountInfo> children;
}
