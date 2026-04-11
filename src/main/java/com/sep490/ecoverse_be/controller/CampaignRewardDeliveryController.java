package com.sep490.ecoverse_be.controller;

import com.sep490.ecoverse_be.dto.request.DeliverRewardDeliveryRequest;
import com.sep490.ecoverse_be.dto.request.ShipRewardDeliveryRequest;
import com.sep490.ecoverse_be.dto.response.CampaignRewardDeliveryResponse;
import com.sep490.ecoverse_be.dto.response.ResponseDto;
import com.sep490.ecoverse_be.enums.PartnershipRewardStatus;
import com.sep490.ecoverse_be.service.ICampaignRewardDeliveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Campaign Reward Delivery", description = "APIs quan ly giao qua thi dua partnership: partnership ship qua, truong xac nhan nhan, giao cho hoc sinh, phu huynh xac nhan")
public class CampaignRewardDeliveryController {

    private final ICampaignRewardDeliveryService deliveryService;

    // ======================== PARTNERSHIP endpoints ========================

    @GetMapping("/api/partnership/campaigns/{campaignId}/reward-deliveries")
    @PreAuthorize("hasAuthority('THIRD_PARTY_PARTNERSHIP')")
    @Operation(
            summary = "Partnership xem danh sach giao qua cua campaign",
            description = """
                    Lay toan bo delivery records cua campaign do partnership tao.
                    Co the loc theo `status`: PREPARING | SHIPPING | ARRIVED | DELIVERED | CONFIRMED.

                    Response bao gom:
                    - `totalScore`: diem tong hop cua hoc sinh (combinedAccuracyPercentage)
                    - `rewardImageUrl` / `rewardImagePresignedUrl`: hinh anh phan qua
                    """
    )
    public ResponseEntity<ResponseDto<List<CampaignRewardDeliveryResponse>>> getDeliveriesByCampaign(
            @PathVariable UUID campaignId,
            @RequestParam(required = false) PartnershipRewardStatus status) {
        return ResponseEntity.ok(ResponseDto.success(
                deliveryService.getDeliveriesByCampaign(campaignId, status),
                "Lay danh sach delivery thanh cong"));
    }

    @PutMapping("/api/partnership/reward-deliveries/{id}/ship")
    @PreAuthorize("hasAuthority('THIRD_PARTY_PARTNERSHIP')")
    @Operation(
            summary = "Partnership xac nhan da gui qua ve truong (PREPARING -> SHIPPING)",
            description = """
                    Partnership danh dau da chuan bi va gui qua ve truong.
                    Co the them `trackingCode` de truong theo doi van chuyen.

                    **Loi co the xay ra:**
                    - `400` — Status khong phai PREPARING
                    - `403` — Delivery khong thuoc campaign cua partnership nay
                    - `404` — Khong tim thay delivery
                    """
    )
    public ResponseEntity<ResponseDto<CampaignRewardDeliveryResponse>> shipReward(
            @PathVariable UUID id,
            @RequestBody(required = false) ShipRewardDeliveryRequest request) {
        if (request == null) request = new ShipRewardDeliveryRequest();
        return ResponseEntity.ok(ResponseDto.success(
                deliveryService.shipReward(id, request),
                "Da xac nhan gui qua thanh cong"));
    }

    // ======================== SCHOOL endpoints ========================

    @GetMapping("/api/school/campaigns/{campaignId}/reward-deliveries")
    @PreAuthorize("hasAuthority('PARTNERSHIP_SCHOOL')")
    @Operation(
            summary = "Truong xem danh sach qua cua hoc sinh truong minh",
            description = """
                    Lay cac delivery records cua truong trong campaign partnership.
                    Co the loc theo `status`.
                    """
    )
    public ResponseEntity<ResponseDto<List<CampaignRewardDeliveryResponse>>> getDeliveriesBySchool(
            @PathVariable UUID campaignId,
            @RequestParam(required = false) PartnershipRewardStatus status) {
        return ResponseEntity.ok(ResponseDto.success(
                deliveryService.getDeliveriesBySchool(campaignId, status),
                "Lay danh sach delivery thanh cong"));
    }

    @PutMapping("/api/school/reward-deliveries/{id}/arrived")
    @PreAuthorize("hasAuthority('PARTNERSHIP_SCHOOL')")
    @Operation(
            summary = "Truong xac nhan da nhan qua tu partnership (SHIPPING -> ARRIVED)",
            description = """
                    Nha truong xac nhan da nhan duoc qua tu ben partnership gui ve.

                    **Loi co the xay ra:**
                    - `400` — Status khong phai SHIPPING
                    - `403` — Delivery khong thuoc truong nay
                    - `404` — Khong tim thay delivery
                    """
    )
    public ResponseEntity<ResponseDto<CampaignRewardDeliveryResponse>> confirmArrived(@PathVariable UUID id) {
        return ResponseEntity.ok(ResponseDto.success(
                deliveryService.confirmArrived(id),
                "Xac nhan nhan qua thanh cong"));
    }

    @PutMapping("/api/school/reward-deliveries/{id}/deliver")
    @PreAuthorize("hasAuthority('PARTNERSHIP_SCHOOL')")
    @Operation(
            summary = "Truong xac nhan da giao qua cho hoc sinh kem anh bang chung (ARRIVED -> DELIVERED)",
            description = """
                    Nha truong giao qua truc tiep cho hoc sinh va upload anh bang chung.

                    `deliveryImageUrl` (bat buoc): S3 key cua anh bang chung giao qua,
                    lay tu `POST /api/files/upload/image`.

                    **Loi co the xay ra:**
                    - `400` — Status khong phai ARRIVED, hoac thieu anh bang chung
                    - `403` — Delivery khong thuoc truong nay
                    - `404` — Khong tim thay delivery
                    """
    )
    public ResponseEntity<ResponseDto<CampaignRewardDeliveryResponse>> markDelivered(
            @PathVariable UUID id,
            @Valid @RequestBody DeliverRewardDeliveryRequest request) {
        return ResponseEntity.ok(ResponseDto.success(
                deliveryService.markDelivered(id, request),
                "Xac nhan giao qua thanh cong"));
    }

    // ======================== STUDENT / PARENT endpoints ========================

    @GetMapping("/api/rewards/deliveries/my")
    @PreAuthorize("hasAnyAuthority('STUDENT', 'PARENT')")
    @Operation(
            summary = "Xem lich su nhan qua thi dua partnership cua minh / con",
            description = """
                    - **Student**: tra ve danh sach qua cua hoc sinh do.
                    - **Parent**: tra ve danh sach qua cua tat ca con.
                    """
    )
    public ResponseEntity<ResponseDto<List<CampaignRewardDeliveryResponse>>> getMyDeliveries() {
        return ResponseEntity.ok(ResponseDto.success(
                deliveryService.getMyDeliveries(),
                "Lay danh sach qua thanh cong"));
    }

    @PutMapping("/api/rewards/deliveries/{id}/confirm")
    @PreAuthorize("hasAuthority('PARENT')")
    @Operation(
            summary = "Phu huynh xac nhan da nhan qua cho con (DELIVERED -> CONFIRMED)",
            description = """
                    Chi phu huynh co lien ket voi hoc sinh trong delivery moi co the xac nhan.

                    **Loi co the xay ra:**
                    - `400` — Status khong phai DELIVERED
                    - `403` — Phu huynh khong co lien ket voi hoc sinh
                    - `404` — Khong tim thay delivery
                    """
    )
    public ResponseEntity<ResponseDto<CampaignRewardDeliveryResponse>> confirmReceived(@PathVariable UUID id) {
        return ResponseEntity.ok(ResponseDto.success(
                deliveryService.confirmReceived(id),
                "Xac nhan nhan qua thanh cong"));
    }
}
