package com.sep490.ecoverse_be.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GenerateAiQuizRequest {

    @NotNull(message = "Campaign ID không được rỗng")
    private UUID campaignId;

    @NotNull(message = "Round ID không được rỗng — AI quiz bắt buộc phải dựa vào waste items của round")
    private UUID roundId;

    @NotNull(message = "Số câu hỏi không được rỗng")
    @Min(value = 15, message = "Số câu hỏi phải từ 15 trở lên")
    @Max(value = 30, message = "Số câu hỏi tối đa là 30")
    private Integer questionCount;

    @NotNull(message = "Lớp học không được rỗng")
    @Min(value = 1, message = "Lớp phải từ 1 trở lên")
    @Max(value = 12, message = "Lớp tối đa là 12")
    private Integer targetGrade;

    @NotNull(message = "Số coin thưởng không được rỗng")
    @Min(value = 1, message = "Số coin thưởng phải >= 1")
    private Integer coinsOnPass;

    @NotNull(message = "Thời gian mỗi câu không được rỗng")
    @Min(value = 5, message = "Thời gian mỗi câu phải >= 5 giây")
    private Integer timePerQuestion;

    /**
     * Danh sách file IDs (đã upload trước đó) để làm ngữ cảnh bổ sung cho AI.
     * Optional — nếu không có thì AI chỉ dựa vào campaign + waste items.
     */
    private List<UUID> fileIds;
}
