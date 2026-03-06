package com.sep490.ecoverse_be.dto.response;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CloudinaryResponse {
    private String publicId;
    private String url;
}
