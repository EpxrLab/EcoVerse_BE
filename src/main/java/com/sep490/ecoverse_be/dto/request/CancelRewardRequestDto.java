package com.sep490.ecoverse_be.dto.request;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CancelRewardRequestDto {

    private String reason;
}
