package com.sep490.ecoverse_be.service;

import com.sep490.ecoverse_be.dto.response.RewardStatusLogResponse;
import com.sep490.ecoverse_be.entity.RewardStatusLog;
import com.sep490.ecoverse_be.entity.User;
import com.sep490.ecoverse_be.enums.RewardLogTopic;
import com.sep490.ecoverse_be.repository.CampaignRewardDeliveryRepository;
import com.sep490.ecoverse_be.repository.RewardRequestRepository;
import com.sep490.ecoverse_be.repository.RewardStatusLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RewardStatusLogService {

    private final RewardStatusLogRepository logRepository;
    private final RewardRequestRepository rewardRequestRepository;
    private final CampaignRewardDeliveryRepository deliveryRepository;

    /**
     * Ghi log chuyển trạng thái reward.
     *
     * @param topic       SCHOOL_REWARD hoặc PARTNERSHIP_REWARD
     * @param referenceId ID của RewardRequest hoặc CampaignRewardDelivery
     * @param fromStatus  Trạng thái trước (nullable nếu lần tạo đầu tiên)
     * @param toStatus    Trạng thái sau
     * @param actorUser   User thực hiện (nullable nếu hệ thống tự động)
     * @param actorName   Tên hiển thị actor
     * @param actorRole   Role của actor (STUDENT, PARENT, PARTNERSHIP_SCHOOL, SYSTEM,...)
     * @param reason      Lý do chuyển trạng thái
     * @param notes       Ghi chú bổ sung
     */
    public void logTransition(RewardLogTopic topic,
                              UUID referenceId,
                              String fromStatus,
                              String toStatus,
                              User actorUser,
                              String actorName,
                              String actorRole,
                              String reason,
                              String notes) {
        RewardStatusLog log = new RewardStatusLog();
        log.setTopic(topic);
        log.setReferenceId(referenceId);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setActorUser(actorUser);
        log.setActorName(actorName);
        log.setActorRole(actorRole);
        log.setReason(reason);
        log.setNotes(notes);
        log.setTransitionAt(OffsetDateTime.now());
        logRepository.save(log);
    }

    /**
     * Lấy toàn bộ log theo referenceId (sắp xếp theo thời gian tăng dần).
     */
    public List<RewardStatusLogResponse> getLogsByReference(UUID referenceId) {
        return logRepository.findByReferenceIdOrderByTransitionAtAsc(referenceId)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    /**
     * Lấy log theo referenceId và topic.
     */
    public List<RewardStatusLogResponse> getLogsByReferenceAndTopic(UUID referenceId, RewardLogTopic topic) {
        return logRepository.findByReferenceIdAndTopicOrderByTransitionAtAsc(referenceId, topic)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    /**
     * Trường xem tất cả log SCHOOL_REWARD thuộc về trường mình (có phân trang).
     * Logic: lấy tất cả RewardRequest IDs của school → query logs theo referenceId IN danh sách đó.
     */
    public Page<RewardStatusLogResponse> getAllLogsBySchool(UUID schoolId, Pageable pageable) {
        List<UUID> requestIds = rewardRequestRepository.findBySchoolIdOrderByCreatedAtDesc(schoolId)
                .stream().map(r -> r.getId()).collect(Collectors.toList());

        if (requestIds.isEmpty()) {
            return Page.empty(pageable);
        }

        return logRepository.findByTopicAndReferenceIdInOrderByTransitionAtDesc(
                RewardLogTopic.SCHOOL_REWARD, requestIds, pageable
        ).map(this::mapToResponse);
    }

    /**
     * Partnership xem tất cả log PARTNERSHIP_REWARD thuộc về campaigns của mình (có phân trang).
     * Logic: lấy tất cả CampaignRewardDelivery IDs thuộc campaigns của partnership → query logs.
     */
    public Page<RewardStatusLogResponse> getAllLogsByPartnership(UUID partnershipId, Pageable pageable) {
        List<UUID> deliveryIds = deliveryRepository.findByPartnershipId(partnershipId)
                .stream().map(d -> d.getId()).collect(Collectors.toList());

        if (deliveryIds.isEmpty()) {
            return Page.empty(pageable);
        }

        return logRepository.findByTopicAndReferenceIdInOrderByTransitionAtDesc(
                RewardLogTopic.PARTNERSHIP_REWARD, deliveryIds, pageable
        ).map(this::mapToResponse);
    }

    private RewardStatusLogResponse mapToResponse(RewardStatusLog log) {
        return RewardStatusLogResponse.builder()
                .id(log.getId())
                .topic(log.getTopic().name())
                .referenceId(log.getReferenceId())
                .fromStatus(log.getFromStatus())
                .toStatus(log.getToStatus())
                .actorUserId(log.getActorUser() != null ? log.getActorUser().getId() : null)
                .actorName(log.getActorName())
                .actorRole(log.getActorRole())
                .reason(log.getReason())
                .notes(log.getNotes())
                .transitionAt(log.getTransitionAt())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
