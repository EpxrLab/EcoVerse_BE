package com.sep490.ecoverse_be.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ImportErrorDetail {

    private int rowNumber;
    private String field;
    private String message;
}
