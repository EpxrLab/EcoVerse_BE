package com.sep490.ecoverse_be.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResponseDto<T> {

    private int status;

    private String message;

    private T data;

    public static <T> ResponseDto<T> success(T data, String message) {
        return ResponseDto.<T>builder()
                .status(200)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ResponseDto<T> created(T data, String message) {
        return ResponseDto.<T>builder()
                .status(201)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ResponseDto<T> badRequest(T data, String message) {
        return ResponseDto.<T>builder()
                .status(400)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ResponseDto<T> unauthorized(String message) {
        return ResponseDto.<T>builder()
                .status(401)
                .message(message)
                .data(null)
                .build();
    }

    public static <T> ResponseDto<T> forbidden(String message) {
        return ResponseDto.<T>builder()
                .status(403)
                .message(message)
                .data(null)
                .build();
    }

    public static <T> ResponseDto<T> notFound(String message) {
        return ResponseDto.<T>builder()
                .status(404)
                .message(message)
                .data(null)
                .build();
    }

    public static <T> ResponseDto<T> error(String message) {
        return ResponseDto.<T>builder()
                .status(500)
                .message(message)
                .data(null)
                .build();
    }
}
