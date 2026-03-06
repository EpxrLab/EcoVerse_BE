package com.sep490.ecoverse_be.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ImportResultResponse {

    private int totalRows;
    private int successCount;
    private int failCount;
    private List<ImportErrorDetail> errors;
}
