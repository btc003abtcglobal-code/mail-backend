package com.yourcompany.mailapp.service.mail;

import com.yourcompany.mailapp.entity.Attachment;
import com.yourcompany.mailapp.repository.AttachmentRepository;
import com.yourcompany.mailapp.util.FileUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final FileUtil fileUtil;

    @Transactional
    public Attachment uploadAttachment(MultipartFile file) throws IOException {
        String filePath = fileUtil.saveFile(file);

        Attachment attachment = new Attachment();
        attachment.setFilename(fileUtil.sanitizeFilename(file.getOriginalFilename()));
        attachment.setContentType(file.getContentType());
        attachment.setSizeBytes(file.getSize());
        attachment.setFilePath(filePath);
        attachment.setIsInline(false);

        Attachment saved = attachmentRepository.save(attachment);
        log.info("Attachment uploaded: {} ({})", saved.getFilename(), saved.getId());
        return saved;
    }

    public Attachment getAttachment(Long attachmentId) {
        return attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new RuntimeException("Attachment not found"));
    }

    public Resource getAttachmentFile(Long attachmentId) {
        Attachment attachment = getAttachment(attachmentId);
        File file = fileUtil.getFile(attachment.getFilePath());

        if (!file.exists()) {
            throw new RuntimeException("File not found: " + attachment.getFilename());
        }

        return new FileSystemResource(file);
    }

    public List<Attachment> getAttachmentsByMailId(Long mailId) {
        return attachmentRepository.findByMailId(mailId);
    }

    @Transactional
    public void deleteAttachment(Long attachmentId) {
        Attachment attachment = getAttachment(attachmentId);
        fileUtil.deleteFile(attachment.getFilePath());
        attachmentRepository.delete(attachment);
        log.info("Attachment deleted: {}", attachmentId);
    }
}
