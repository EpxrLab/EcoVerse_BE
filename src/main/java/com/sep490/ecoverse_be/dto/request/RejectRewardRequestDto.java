package com.sep490.ecoverse_be.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RejectRewardRequestDto {

    @NotBlank(message = "Lý do từ chối không được rỗng")
    private String reason;

    private boolean approved;
}
