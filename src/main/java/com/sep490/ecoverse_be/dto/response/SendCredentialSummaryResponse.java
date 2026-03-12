package com.sep490.ecoverse_be.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SendCredentialSummaryResponse {

    private int sentCount;

    // So phu huynh bi bo qua vi da nhan email truoc do
    private int skippedCount;
}
