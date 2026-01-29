package com.yourcompany.mailapp.controller;

import com.yourcompany.mailapp.entity.Folder;
import com.yourcompany.mailapp.entity.MailAccount;
import com.yourcompany.mailapp.repository.FolderRepository;
import com.yourcompany.mailapp.repository.MailAccountRepository;
import com.yourcompany.mailapp.repository.MailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST Controller for folder management operations
 * Handles folder CRUD operations and statistics
 */
@Slf4j
@RestController
@RequestMapping("/api/folders")
@RequiredArgsConstructor
public class FolderController {

    private final FolderRepository folderRepository;
    private final MailAccountRepository mailAccountRepository;
    private final MailRepository mailRepository;

    /**
     * Get all folders for a mail account
     * 
     * @param mailAccountId mail account ID
     * @param userDetails   authenticated user
     * @return list of folders with counts
     */
    @GetMapping("/account/{mailAccountId}")
    public ResponseEntity<?> getFoldersByAccount(
            @PathVariable long mailAccountId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            log.debug("Getting folders for mail account: {}", mailAccountId);

            // Verify account belongs to user
            MailAccount mailAccount = mailAccountRepository.findById(mailAccountId)
                    .orElseThrow(() -> new RuntimeException("Mail account not found"));

            if (!mailAccount.getUser().getUsername().equals(userDetails.getUsername())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Access denied"));
            }

            List<Folder> folders = folderRepository.findByMailAccountId(mailAccountId);

            // Map to response with counts
            List<Map<String, Object>> folderResponses = folders.stream()
                    .map(folder -> mapFolderToResponse(folder))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(folderResponses);

        } catch (RuntimeException e) {
            log.error("Error getting folders: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error getting folders", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to retrieve folders"));
        }
    }

    /**
     * Get single folder by ID
     * 
     * @param folderId    folder ID
     * @param userDetails authenticated user
     * @return folder details with counts
     */
    @GetMapping("/{folderId}")
    public ResponseEntity<?> getFolder(
            @PathVariable long folderId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            log.debug("Getting folder: {}", folderId);

            Folder folder = folderRepository.findById(folderId)
                    .orElseThrow(() -> new RuntimeException("Folder not found"));

            // Verify folder belongs to user's account
            if (!folder.getMailAccount().getUser().getUsername().equals(userDetails.getUsername())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Access denied"));
            }

            Map<String, Object> response = mapFolderToResponse(folder);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Error getting folder: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error getting folder", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to retrieve folder"));
        }
    }

    /**
     * Create a new custom folder
     * 
     * @param mailAccountId mail account ID
     * @param request       folder creation request
     * @param userDetails   authenticated user
     * @return created folder
     */
    @PostMapping("/account/{mailAccountId}")
    public ResponseEntity<?> createFolder(
            @PathVariable long mailAccountId,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            log.info("Creating folder for account: {}", mailAccountId);

            // Validate input
            String folderName = request.get("name");
            if (folderName == null || folderName.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Folder name is required"));
            }

            // Verify account belongs to user
            MailAccount mailAccount = mailAccountRepository.findById(mailAccountId)
                    .orElseThrow(() -> new RuntimeException("Mail account not found"));

            if (!mailAccount.getUser().getUsername().equals(userDetails.getUsername())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Access denied"));
            }

            // Check if folder already exists
            if (folderRepository.findByMailAccountAndFullName(mailAccount, folderName).isPresent()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Folder already exists"));
            }

            // Create folder
            Folder folder = new Folder();
            folder.setName(folderName);
            folder.setFullName(folderName);
            folder.setType(Folder.FolderType.CUSTOM);
            folder.setMailAccount(mailAccount);
            folder.setUnreadCount(0);
            folder.setTotalCount(0);

            Folder savedFolder = folderRepository.save(folder);
            log.info("Folder created: {} for account: {}", folderName, mailAccountId);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(mapFolderToResponse(savedFolder));

        } catch (RuntimeException e) {
            log.error("Error creating folder: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating folder", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to create folder"));
        }
    }

    /**
     * Update folder name
     * 
     * @param folderId    folder ID
     * @param request     update request
     * @param userDetails authenticated user
     * @return updated folder
     */
    @PutMapping("/{folderId}")
    public ResponseEntity<?> updateFolder(
            @PathVariable long folderId,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            log.info("Updating folder: {}", folderId);

            Folder folder = folderRepository.findById(folderId)
                    .orElseThrow(() -> new RuntimeException("Folder not found"));

            // Verify folder belongs to user's account
            if (!folder.getMailAccount().getUser().getUsername().equals(userDetails.getUsername())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Access denied"));
            }

            // Only allow updating custom folders
            if (folder.getType() != Folder.FolderType.CUSTOM) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Cannot modify system folders"));
            }

            String newName = request.get("name");
            if (newName != null && !newName.trim().isEmpty()) {
                folder.setName(newName);
                folder.setFullName(newName);
            }

            Folder updatedFolder = folderRepository.save(folder);
            log.info("Folder updated: {}", folderId);

            return ResponseEntity.ok(mapFolderToResponse(updatedFolder));

        } catch (RuntimeException e) {
            log.error("Error updating folder: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating folder", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to update folder"));
        }
    }

    /**
     * Delete a custom folder
     * 
     * @param folderId    folder ID
     * @param userDetails authenticated user
     * @return deletion confirmation
     */
    @DeleteMapping("/{folderId}")
    public ResponseEntity<?> deleteFolder(
            @PathVariable long folderId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            log.info("Deleting folder: {}", folderId);

            Folder folder = folderRepository.findById(folderId)
                    .orElseThrow(() -> new RuntimeException("Folder not found"));

            // Verify folder belongs to user's account
            if (!folder.getMailAccount().getUser().getUsername().equals(userDetails.getUsername())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Access denied"));
            }

            // Only allow deleting custom folders
            if (folder.getType() != Folder.FolderType.CUSTOM) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Cannot delete system folders"));
            }

            // Check if folder has mails
            Long mailCount = mailRepository.countByFolderAndIsDeletedFalse(folder);
            if (mailCount > 0) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse(
                                "Cannot delete folder with mails. Please move or delete all mails first."));
            }

            folderRepository.delete(folder);
            log.info("Folder deleted: {}", folderId);

            return ResponseEntity.ok(createSuccessResponse("Folder deleted successfully"));

        } catch (RuntimeException e) {
            log.error("Error deleting folder: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error deleting folder", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to delete folder"));
        }
    }

    /**
     * Get folder statistics
     * 
     * @param folderId    folder ID
     * @param userDetails authenticated user
     * @return folder statistics
     */
    @GetMapping("/{folderId}/stats")
    public ResponseEntity<?> getFolderStats(
            @PathVariable long folderId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            log.debug("Getting stats for folder: {}", folderId);

            Folder folder = folderRepository.findById(folderId)
                    .orElseThrow(() -> new RuntimeException("Folder not found"));

            // Verify folder belongs to user's account
            if (!folder.getMailAccount().getUser().getUsername().equals(userDetails.getUsername())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Access denied"));
            }

            // Get statistics from database
            Object[] stats = mailRepository.getFolderStatistics(folderId);

            Map<String, Object> response = new HashMap<>();
            response.put("folderId", folderId);
            response.put("folderName", folder.getName());
            response.put("folderType", folder.getType().name());
            response.put("totalMails", stats[0]);
            response.put("unreadMails", stats[1]);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Error getting folder stats: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error getting folder stats", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to retrieve folder statistics"));
        }
    }

    /**
     * Get folder by type
     * 
     * @param mailAccountId mail account ID
     * @param type          folder type (INBOX, SENT, DRAFTS, TRASH, SPAM)
     * @param userDetails   authenticated user
     * @return folder
     */
    @GetMapping("/account/{mailAccountId}/type/{type}")
    public ResponseEntity<?> getFolderByType(
            @PathVariable long mailAccountId,
            @PathVariable String type,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            log.debug("Getting folder by type: {} for account: {}", type, mailAccountId);

            // Verify account belongs to user
            MailAccount mailAccount = mailAccountRepository.findById(mailAccountId)
                    .orElseThrow(() -> new RuntimeException("Mail account not found"));

            if (!mailAccount.getUser().getUsername().equals(userDetails.getUsername())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Access denied"));
            }

            // Parse folder type
            Folder.FolderType folderType;
            try {
                folderType = Folder.FolderType.valueOf(type.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Invalid folder type: " + type));
            }

            Folder folder = folderRepository.findByMailAccountAndType(mailAccount, folderType)
                    .orElseThrow(() -> new RuntimeException("Folder not found"));

            return ResponseEntity.ok(mapFolderToResponse(folder));

        } catch (RuntimeException e) {
            log.error("Error getting folder by type: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error getting folder by type", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to retrieve folder"));
        }
    }

    /**
     * Refresh folder counts
     * 
     * @param folderId    folder ID
     * @param userDetails authenticated user
     * @return updated folder
     */
    @PostMapping("/{folderId}/refresh")
    public ResponseEntity<?> refreshFolderCounts(
            @PathVariable long folderId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            log.info("Refreshing counts for folder: {}", folderId);

            Folder folder = folderRepository.findById(folderId)
                    .orElseThrow(() -> new RuntimeException("Folder not found"));

            // Verify folder belongs to user's account
            if (!folder.getMailAccount().getUser().getUsername().equals(userDetails.getUsername())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Access denied"));
            }

            // Recalculate counts
            Long totalCount = mailRepository.countByFolderAndIsDeletedFalse(folder);
            Long unreadCount = mailRepository.countByFolderAndIsReadFalseAndIsDeletedFalse(folder);

            folder.setTotalCount(totalCount.intValue());
            folder.setUnreadCount(unreadCount.intValue());

            Folder updatedFolder = folderRepository.save(folder);
            log.info("Folder counts refreshed: {}", folderId);

            return ResponseEntity.ok(mapFolderToResponse(updatedFolder));

        } catch (RuntimeException e) {
            log.error("Error refreshing folder counts: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error refreshing folder counts", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to refresh folder counts"));
        }
    }

    /**
     * Map folder entity to response DTO
     * 
     * @param folder folder entity
     * @return response map
     */
    private Map<String, Object> mapFolderToResponse(Folder folder) {
        // Recalculate counts from database for accuracy
        Long totalCount = mailRepository.countByFolderAndIsDeletedFalse(folder);
        Long unreadCount = mailRepository.countByFolderAndIsReadFalseAndIsDeletedFalse(folder);

        Map<String, Object> response = new HashMap<>();
        response.put("id", folder.getId());
        response.put("name", folder.getName());
        response.put("fullName", folder.getFullName());
        response.put("type", folder.getType().name());
        response.put("unreadCount", unreadCount);
        response.put("totalCount", totalCount);
        response.put("mailAccountId", folder.getMailAccount().getId());
        response.put("createdAt", folder.getCreatedAt());

        return response;
    }

    /**
     * Helper method to create error response
     * 
     * @param message error message
     * @return error response map
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", message);
        error.put("timestamp", System.currentTimeMillis());
        return error;
    }

    /**
     * Helper method to create success response
     * 
     * @param message success message
     * @return success response map
     */
    private Map<String, Object> createSuccessResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", message);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    /**
     * Exception handler for runtime errors
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntimeException(RuntimeException e) {
        log.error("Runtime error in FolderController: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(createErrorResponse(e.getMessage()));
    }

    /**
     * Exception handler for generic errors
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGenericException(Exception e) {
        log.error("Unexpected error in FolderController", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("An unexpected error occurred"));
    }
}
