package com.sep490.ecoverse_be.service.impl;

import com.sep490.ecoverse_be.dto.request.CancelRewardRequestDto;
import com.sep490.ecoverse_be.dto.request.CreateRewardRequestDto;
import com.sep490.ecoverse_be.dto.request.RejectRewardRequestDto;
import com.sep490.ecoverse_be.dto.response.RewardRequestResponse;
import com.sep490.ecoverse_be.entity.*;
import com.sep490.ecoverse_be.enums.NotificationType;
import com.sep490.ecoverse_be.enums.RewardRequestStatus;
import com.sep490.ecoverse_be.enums.Role;
import com.sep490.ecoverse_be.enums.TransactionType;
import com.sep490.ecoverse_be.event.NotificationEvent;
import com.sep490.ecoverse_be.exception.BadRequestException;
import com.sep490.ecoverse_be.exception.NotFoundException;
import com.sep490.ecoverse_be.model.UserPrincipal;
import com.sep490.ecoverse_be.repository.*;
import com.sep490.ecoverse_be.service.IRewardRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RewardRequestServiceImpl implements IRewardRequestService {

    @Autowired
    private RewardRequestRepository rewardRequestRepository;

    @Autowired
    private RewardRepository rewardRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ParentRepository parentRepository;

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private StudentParentLinkRepository studentParentLinkRepository;

    @Autowired
    private CoinTransactionRepository coinTransactionRepository;

    @Autowired
    private S3PresignedUrlService s3PresignedUrlService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return ((UserPrincipal) auth.getPrincipal()).getUser();
    }

    private String generateRequestCode() {
        return "RR-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private RewardRequestResponse mapToResponse(RewardRequest r) {
        return RewardRequestResponse.builder()
                .id(r.getId())
                .requestCode(r.getRequestCode())
                .studentId(r.getStudent().getId())
                .studentName(r.getStudent().getFullName())
                .studentCode(r.getStudent().getStudentCode())
                .rewardId(r.getReward().getId())
                .rewardName(r.getReward().getRewardName())
                .rewardType(r.getReward().getRewardType())
                .rewardImageUrl(s3PresignedUrlService.generatePresignedUrl(r.getReward().getImageUrl()))
                .quantity(r.getQuantity())
                .totalCoins(r.getTotalCoins())
                .status(r.getStatus())
                .rejectedReason(r.getRejectedReason())
                .cancelledReason(r.getCancelledReason())
                .notes(r.getNotes())
                .approvedBy(r.getApprovedBy() != null ? r.getApprovedBy().getId() : null)
                .approvedAt(r.getApprovedAt())
                .rejectedAt(r.getRejectedAt())
                .deliveredAt(r.getDeliveredAt())
                .confirmedAt(r.getConfirmedAt())
                .cancelledAt(r.getCancelledAt())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }

    private void deductCoins(Student student, BigDecimal amount, UUID referenceId, String description, User creator) {
        if (student.getTotalCoins().compareTo(amount) < 0) {
            throw new BadRequestException("Số dư coin không đủ. Cần " + amount + ", hiện có " + student.getTotalCoins());
        }

        BigDecimal before = student.getTotalCoins();
        BigDecimal after = before.subtract(amount);
        student.setTotalCoins(after);
        studentRepository.save(student);

        CoinTransaction tx = new CoinTransaction();
        tx.setTransactionCode(generateRequestCode().replace("RR-", "TX-"));
        tx.setStudent(student);
        tx.setTransactionType(TransactionType.SPEND_REWARD);
        tx.setAmount(amount.negate());
        tx.setBalanceBefore(before);
        tx.setBalanceAfter(after);
        tx.setReferenceType("REWARD_REQUEST");
        tx.setReferenceId(referenceId);
        tx.setDescription(description);
        tx.setCreatedBy(creator);
        coinTransactionRepository.save(tx);
    }

    private void refundCoins(Student student, BigDecimal amount, UUID referenceId, String description, User creator) {
        BigDecimal before = student.getTotalCoins();
        BigDecimal after = before.add(amount);
        student.setTotalCoins(after);
        studentRepository.save(student);

        CoinTransaction tx = new CoinTransaction();
        tx.setTransactionCode(generateRequestCode().replace("RR-", "TX-"));
        tx.setStudent(student);
        tx.setTransactionType(TransactionType.REFUND);
        tx.setAmount(amount);
        tx.setBalanceBefore(before);
        tx.setBalanceAfter(after);
        tx.setReferenceType("REWARD_REQUEST");
        tx.setReferenceId(referenceId);
        tx.setDescription(description);
        tx.setCreatedBy(creator);
        coinTransactionRepository.save(tx);
    }

    private void reserveStock(Reward reward, int quantity) {
        if (Boolean.TRUE.equals(reward.getIsUnlimited())) return;

        int current = reward.getStockQuantity() != null ? reward.getStockQuantity() : 0;
        if (current < quantity) {
            throw new BadRequestException("Quà '" + reward.getRewardName() + "' đã hết hàng (còn lại: " + current + ")");
        }
        reward.setStockQuantity(current - quantity);
        rewardRepository.save(reward);
    }

    private void releaseStock(Reward reward, int quantity) {
        if (Boolean.TRUE.equals(reward.getIsUnlimited())) return;

        int current = reward.getStockQuantity() != null ? reward.getStockQuantity() : 0;
        reward.setStockQuantity(current + quantity);
        rewardRepository.save(reward);
    }


    @Override
    @Transactional
    public RewardRequestResponse createRequest(CreateRewardRequestDto dto) {
        User currentUser = getCurrentUser();
        Student student;
        Parent requestingParent = null;

        if (currentUser.getRole() == Role.STUDENT) {
            student = studentRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy học sinh"));
        } else {
            requestingParent = parentRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy phụ huynh"));

            if (dto.getStudentId() == null) {
                throw new BadRequestException("Phụ huynh phải chỉ định studentId của con");
            }

            Student targetStudent = studentRepository.findById(dto.getStudentId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy học sinh"));

            boolean isLinked = studentParentLinkRepository
                    .existsByStudentIdAndParentId(targetStudent.getId(), requestingParent.getId());
            if (!isLinked) {
                throw new BadRequestException("Học sinh này không phải con bạn");
            }
            student = targetStudent;
        }

        Reward reward = rewardRepository.findByIdAndSchoolIdAndIsActiveTrue(dto.getRewardId(), student.getSchool().getId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy quà"));

        reserveStock(reward, dto.getQuantity());

        BigDecimal totalCost = reward.getCoinCost().multiply(BigDecimal.valueOf(dto.getQuantity()));

        RewardRequest saved = new RewardRequest();
        saved.setRequestCode(generateRequestCode());
        saved.setStudent(student);
        saved.setReward(reward);
        saved.setSchool(student.getSchool());
        saved.setRequestedByParent(requestingParent);
        saved.setQuantity(dto.getQuantity());
        saved.setTotalCoins(totalCost);
        saved.setStatus(RewardRequestStatus.PENDING);
        saved.setNotes(dto.getNotes());
        saved.setCreatedAt(LocalDateTime.now());
        saved.setUpdatedAt(LocalDateTime.now());
        saved = rewardRequestRepository.save(saved);

        deductCoins(student, totalCost, saved.getId(),
                "Đổi quà: " + reward.getRewardName() + " (x" + dto.getQuantity() + ") - " + saved.getRequestCode(), currentUser);

        List<StudentParentLink> parentLinks = studentParentLinkRepository.findByStudentId(saved.getStudent().getId());

        // Thông báo cho học sinh: yêu cầu đổi quà đã tạo thành công
        eventPublisher.publishEvent(NotificationEvent.builder()
                .recipientUserId(student.getUser().getId())
                .type(NotificationType.REWARD_AVAILABLE)
                .title("Yêu cầu đổi quà đã được tạo")
                .message("Yêu cầu đổi \"" + reward.getRewardName() + "\" (x" + dto.getQuantity()
                        + ") đã được gửi. Vui lòng chờ nhà trường xét duyệt. Mã: " + saved.getRequestCode())
                .referenceType("reward_request")
                .referenceId(saved.getId())
                .sendEmail(false)
                .build());
    // Thông báo cho học sinh: yêu cầu đổi quà đã tạo thành công
        for (StudentParentLink link : parentLinks) {
            eventPublisher.publishEvent(NotificationEvent.builder()
                    .recipientUserId(link.getParent().getUser().getId())
                    .type(NotificationType.REWARD_AVAILABLE)
                    .title("Yêu cầu đổi quà cho con đã được tạo")
                    .message("Yêu cầu đổi \"" + reward.getRewardName() + "\" (x" + dto.getQuantity()
                            + ") đã được gửi. Vui lòng chờ nhà trường xét duyệt. Mã: " + saved.getRequestCode())
                    .referenceType("reward_request")
                    .referenceId(saved.getId())
                    .sendEmail(false)
                    .build());
        }
        return mapToResponse(saved);
    }

    @Override
    public List<RewardRequestResponse> getMyRequests() {
        User currentUser = getCurrentUser();

        if (currentUser.getRole() == Role.STUDENT) {
            Student student = studentRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy học sinh"));
            return rewardRequestRepository.findByStudentIdOrderByCreatedAtDesc(student.getId())
                    .stream().map(this::mapToResponse).collect(Collectors.toList());
        } else {
            Parent parent = parentRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy phụ huynh"));

            return studentParentLinkRepository.findByParentId(parent.getId()).stream()
                    .flatMap(link -> rewardRequestRepository
                            .findByStudentIdOrderByCreatedAtDesc(link.getStudent().getId()).stream())
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        }
    }

    @Override
    @Transactional
    public RewardRequestResponse cancelRequest(UUID requestId, CancelRewardRequestDto dto) {
        User currentUser = getCurrentUser();
        RewardRequest request;

        if (currentUser.getRole() == Role.STUDENT) {
            Student student = studentRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy học sinh"));
            request = rewardRequestRepository.findByIdAndStudentId(requestId, student.getId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy yêu cầu đổi quà"));
        } else {
            Parent parent = parentRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy phụ huynh"));
            request = studentParentLinkRepository.findByParentId(parent.getId()).stream()
                    .flatMap(link -> rewardRequestRepository
                            .findByIdAndStudentId(requestId, link.getStudent().getId()).stream())
                    .findFirst()
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy yêu cầu đổi quà"));
        }

        if (request.getStatus() != RewardRequestStatus.PENDING) {
            throw new BadRequestException(
                    "Chỉ có thể hủy khi status là PENDING. Status hiện tại: " + request.getStatus());
        }

        request.setStatus(RewardRequestStatus.CANCELLED);
        request.setCancelledAt(LocalDateTime.now());
        request.setCancelledReason(dto.getReason());
        request.setUpdatedAt(LocalDateTime.now());
        rewardRequestRepository.save(request);

        refundCoins(request.getStudent(), request.getTotalCoins(), request.getId(),
                "Hoàn coin hủy đổi quà: " + request.getReward().getRewardName() + " - " + request.getRequestCode(), currentUser);

        releaseStock(request.getReward(), request.getQuantity());

        return mapToResponse(request);
    }


    @Override
    public List<RewardRequestResponse> getSchoolRequests(RewardRequestStatus status) {
        User currentUser = getCurrentUser();
        School school = schoolRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy trường học"));

        List<RewardRequest> requests = status != null
                ? rewardRequestRepository.findBySchoolIdAndStatusOrderByCreatedAtDesc(school.getId(), status)
                : rewardRequestRepository.findBySchoolIdOrderByCreatedAtDesc(school.getId());

        return requests.stream().map(this::mapToResponse).collect(Collectors.toList());
    }


    @Override
    @Transactional
    public RewardRequestResponse approveOrRejectRequest(UUID requestId, RejectRewardRequestDto dto) {
        User currentUser = getCurrentUser();
        School school = schoolRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy trường học"));

        RewardRequest request = rewardRequestRepository.findByIdAndSchoolId(requestId, school.getId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy yêu cầu đổi quà"));

        if (request.getStatus() != RewardRequestStatus.PENDING) {
            throw new BadRequestException(
                    "Chỉ có thể duyệt khi status là PENDING. Status hiện tại: " + request.getStatus());
        }

        if (dto.isApproved()) {
            request.setStatus(RewardRequestStatus.APPROVED);
            request.setApprovedAt(LocalDateTime.now());
            request.setUpdatedAt(LocalDateTime.now());
        } else {
            request.setStatus(RewardRequestStatus.REJECTED);
            request.setRejectedReason(dto.getReason());
            request.setRejectedAt(LocalDateTime.now());
            request.setUpdatedAt(LocalDateTime.now());

            refundCoins(request.getStudent(), request.getTotalCoins(), request.getId(),
                    "Hoàn coin do bị từ chối đổi quà: " + request.getReward().getRewardName() + " - " + request.getRequestCode(), currentUser);

            releaseStock(request.getReward(), request.getQuantity());
        }

        request.setApprovedBy(currentUser);
        rewardRequestRepository.save(request);

        User studentUser = request.getStudent().getUser();
        // Lay parent cua hoc sinh (neu co) de gui them thong bao
        List<StudentParentLink> parentLinks = studentParentLinkRepository.findByStudentId(request.getStudent().getId());

        if (dto.isApproved()) {
            // Thong bao hoc sinh: duoc duyet
            eventPublisher.publishEvent(NotificationEvent.builder()
                    .recipientUserId(studentUser.getId())
                    .type(NotificationType.REWARD_AVAILABLE)
                    .title("Yêu cầu đổi quà được duyệt!")
                    .message("Yêu cầu đổi \"" + request.getReward().getRewardName()
                            + "\" đã được nhà trường duyệt. Nhà trường sẽ liên hệ để giao quà sớm.")
                    .referenceType("reward_request")
                    .referenceId(request.getId())
                    .sendEmail(false)
                    .build());
            // Thong bao phu huynh: duoc duyet
            for (StudentParentLink link : parentLinks) {
                eventPublisher.publishEvent(NotificationEvent.builder()
                        .recipientUserId(link.getParent().getUser().getId())
                        .type(NotificationType.REWARD_AVAILABLE)
                        .title("Yêu cầu đổi quà của con được duyệt")
                        .message("Yêu cầu đổi \"" + request.getReward().getRewardName()
                                + "\" của " + request.getStudent().getFullName()
                                + " đã được nhà trường duyệt.")
                        .referenceType("reward_request")
                        .referenceId(request.getId())
                        .sendEmail(false)
                        .build());
            }
        } else {
            // Thong bao hoc sinh: bi tu choi + hoan xu
            eventPublisher.publishEvent(NotificationEvent.builder()
                    .recipientUserId(studentUser.getId())
                    .type(NotificationType.REWARD_AVAILABLE)
                    .title("Yêu cầu đổi quà bị từ chối")
                    .message("Yêu cầu đổi \"" + request.getReward().getRewardName()
                            + "\" bị từ chối. Lý do: " + (dto.getReason() != null ? dto.getReason() : "Không có lý do")
                            + ". Xu đã được hoàn trả.")
                    .referenceType("reward_request")
                    .referenceId(request.getId())
                    .sendEmail(false)
                    .build());
            // Thong bao phu huynh: bi tu choi
            for (StudentParentLink link : parentLinks) {
                eventPublisher.publishEvent(NotificationEvent.builder()
                        .recipientUserId(link.getParent().getUser().getId())
                        .type(NotificationType.REWARD_AVAILABLE)
                        .title("Yêu cầu đổi quà của con bị từ chối")
                        .message("Yêu cầu đổi \"" + request.getReward().getRewardName()
                                + "\" của " + request.getStudent().getFullName()
                                + " bị từ chối. Xu đã được hoàn trả.")
                        .referenceType("reward_request")
                        .referenceId(request.getId())
                        .sendEmail(false)
                        .build());
            }
        }

        return mapToResponse(request);
    }

    @Override
    @Transactional
    public RewardRequestResponse markDelivered(UUID requestId) {
        User currentUser = getCurrentUser();
        School school = schoolRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy trường học"));

        RewardRequest request = rewardRequestRepository.findByIdAndSchoolId(requestId, school.getId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy yêu cầu đổi quà"));

        if (request.getStatus() != RewardRequestStatus.APPROVED) {
            throw new BadRequestException(
                    "Chỉ có thể xác nhận giao khi status là APPROVED. Status hiện tại: " + request.getStatus());
        }

        request.setStatus(RewardRequestStatus.DELIVERED);
        request.setDeliveredAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());
        rewardRequestRepository.save(request);

        // Thong bao phu huynh: qua san sang giao, phu huynh can xac nhan
        List<StudentParentLink> parentLinks = studentParentLinkRepository.findByStudentId(request.getStudent().getId());
        for (StudentParentLink link : parentLinks) {
            eventPublisher.publishEvent(NotificationEvent.builder()
                    .recipientUserId(link.getParent().getUser().getId())
                    .type(NotificationType.REWARD_DELIVERED)
                    .title("Quà đã giao!")
                    .message("Quà \"" + request.getReward().getRewardName() + "\" của "
                            + request.getStudent().getFullName()
                            + " đã giao. Vui lòng xác nhận đã nhận quà.")
                    .referenceType("reward_request")
                    .referenceId(request.getId())
                    .sendEmail(false)
                    .build());
        }

        return mapToResponse(request);
    }

    @Override
    @Transactional
    public RewardRequestResponse confirmReceived(UUID requestId) {
        User currentUser = getCurrentUser();
        Parent parent = parentRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phụ huynh"));

        RewardRequest request = studentParentLinkRepository.findByParentId(parent.getId()).stream()
                .flatMap(link -> rewardRequestRepository
                        .findByIdAndStudentId(requestId, link.getStudent().getId()).stream())
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Không tìm thấy yêu cầu đổi quà"));

        if (request.getStatus() != RewardRequestStatus.DELIVERED) {
            throw new BadRequestException(
                    "Chỉ có thể xác nhận quà khi status là DELIVERED. Status hiện tại: " + request.getStatus());
        }

        request.setStatus(RewardRequestStatus.CONFIRMED);
        request.setConfirmedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());
        request.setConfirmedByParent(parent);
        rewardRequestRepository.save(request);
        return mapToResponse(request);
    }
}
