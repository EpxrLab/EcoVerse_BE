package com.sep490.ecoverse_be.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
public class CreatePartnershipCampaignRequest {

    @NotBlank
    private String campaignName;

    private String description;

    @NotNull
    private OffsetDateTime startDate;

    @NotNull
    private OffsetDateTime endDate;

    private OffsetDateTime registrationDate;

    private OffsetDateTime registrationDeadline;

    private OffsetDateTime invitationDate;

    private OffsetDateTime invitationDeadline;

    private Integer minStudentsPerSchool;

    private Integer totalStudentQuota;

    private Integer topRankingCount;

    private String bannerImageUrl;

    /**
     * Danh sách trường tham gia kèm quota tối đa từng trường.
     * Nếu có {@link #totalStudentQuota}, tổng {@code maxStudentsInvited} phải bằng đúng giá trị đó.
     */
    @Valid
    private List<PartnershipCampaignSchoolRequest> invitedSchools;

    @Valid
    private List<CampaignRewardRequest> rewards;

    @NotEmpty
    @Valid
    private List<PartnershipRoundRequest> rounds;
}

