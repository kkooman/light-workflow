package com.kkooman.lightworkflow.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

@Schema(name = "ApiResponse", description = "공통 API 응답 포맷")
public record ApiResponse<T>(
        @Schema(description = "성공 여부", example = "true") boolean success,
        @Schema(description = "응답 코드", example = "SUCCESS") String code,
        @Schema(description = "응답 메시지", example = "요청이 정상 처리되었습니다.") String message,
        @Schema(description = "응답 데이터") T data,
        @Schema(description = "응답 시각 (UTC)", example = "2026-08-31T12:00:00Z") OffsetDateTime timestamp) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "SUCCESS", "요청이 정상 처리되었습니다.", data, OffsetDateTime.now());
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, "SUCCESS", message, data, OffsetDateTime.now());
    }

    public static <T> ApiResponse<T> fail(String code, String message) {
        return new ApiResponse<>(false, code, message, null, OffsetDateTime.now());
    }
}
