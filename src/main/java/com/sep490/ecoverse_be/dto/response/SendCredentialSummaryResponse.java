package com.sep490.ecoverse_be.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SendCredentialSummaryResponse {

    private int sentCount;

    private int skippedCount;
}
