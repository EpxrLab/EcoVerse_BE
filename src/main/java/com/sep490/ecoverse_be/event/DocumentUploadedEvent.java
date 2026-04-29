package com.sep490.ecoverse_be.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

/**
 * Spring Application Event được publish khi một tài liệu (PDF/DOCX/TXT)
 * được upload thành công. Listener sẽ tự động embed thành vector và lưu vào Qdrant.
 */
@Getter
public class DocumentUploadedEvent extends ApplicationEvent {

    private final UUID fileId;

    public DocumentUploadedEvent(Object source, UUID fileId) {
        super(source);
        this.fileId = fileId;
    }
}
