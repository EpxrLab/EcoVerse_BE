package com.sep490.ecoverse_be.dto.request;

import com.sep490.ecoverse_be.enums.ApprovalStatus;
import lombok.Data;

@Data
public class UpdateApprovalRequest {
    private ApprovalStatus status;

    private String reason;
}
