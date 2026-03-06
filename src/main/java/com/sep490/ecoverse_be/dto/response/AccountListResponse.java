package com.sep490.ecoverse_be.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AccountListResponse {

    private String schoolName;
    private int totalStudents;
    private int totalParents;
    private List<ParentAccountInfo> accounts;
}
