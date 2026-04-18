package com.sep490.ecoverse_be.scheduler;

import com.sep490.ecoverse_be.dto.response.StorageResponse;
import com.sep490.ecoverse_be.entity.WasteItem;
import com.sep490.ecoverse_be.repository.WasteItemRepository;
import com.sep490.ecoverse_be.service.IStorageService;
import com.sep490.ecoverse_be.service.impl.TripoApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class TripoPollingScheduler {

    private final WasteItemRepository wasteItemRepository;
    private final TripoApiService tripoApiService;
    private final IStorageService storageService;

    @Scheduled(fixedDelayString = "${tripo.polling.delay:60000}")
    public void pollTripoStatus() {
        List<WasteItem> pendingItems = wasteItemRepository.findByTripoStatus("PENDING");

        if (pendingItems.isEmpty()) {
            return;
        }

        log.info("Found {} pending Tripo 3D model generation tasks.", pendingItems.size());

        for (WasteItem item : pendingItems) {
            try {
                processItem(item);
            } catch (Exception e) {
                log.error("Failed to process Tripo task for WasteItem ID: {}", item.getId(), e);
            }
        }
    }

    private void processItem(WasteItem item) {
        if (item.getTripoTaskId() == null) {
            item.setTripoStatus("FAILED");
            wasteItemRepository.save(item);
            return;
        }

        TripoApiService.TripoTaskStatusResponse statusResponse = tripoApiService.checkTaskStatus(item.getTripoTaskId());

        if (statusResponse == null) {
            return; // Wait for next tick
        }

        String status = statusResponse.getStatus();

        if ("FINISHED".equalsIgnoreCase(status)) {
            if (statusResponse.getResults() != null && !statusResponse.getResults().isEmpty()) {
                String assetUrl = statusResponse.getResults().get(0).getAsset();
                log.info("Tripo task {} FINISHED. Downloading asset from {}", item.getTripoTaskId(), assetUrl);

                String fileName = "tripo_" + item.getTripoTaskId() + ".glb";
                StorageResponse storageResponse = storageService.uploadModelFromUrl(assetUrl, fileName);

                item.setModel3dUrl(storageResponse.getUrl());
                item.setTripoStatus("FINISHED");

                wasteItemRepository.save(item);
                log.info("Successfully saved 3D model to S3 for WasteItem ID: {}", item.getId());
            } else {
                item.setTripoStatus("FAILED");
                wasteItemRepository.save(item);
                log.warn("Tripo task {} finished but returned no results.", item.getTripoTaskId());
            }
        } else if ("FAILED".equalsIgnoreCase(status) || "CANCELLED".equalsIgnoreCase(status)) {
            item.setTripoStatus("FAILED");
            wasteItemRepository.save(item);
            log.warn("Tripo task {} FAILED. Reason: {}", item.getTripoTaskId(), statusResponse.getFailureReason());
        }
        // If PENDING or RUNNING, do nothing and wait for next scheduled run.
    }
}
