package com.sep490.ecoverse_be.dto.response;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StorageResponse {
    private String publicId;
    private String url;
    private String presignedUrl;
}
