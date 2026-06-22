package com.project.ciaklog.service;

import com.project.ciaklog.dto.request.WatchEntryRequest;
import com.project.ciaklog.dto.response.WatchEntryResponse;
import com.project.ciaklog.entity.WatchStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface WatchEntryService {
    WatchEntryResponse addToLibrary(String username, WatchEntryRequest dto);
    WatchEntryResponse updateStatus(String username, UUID entryId, WatchStatus newStatus);
    void removeFromLibrary(String username, UUID entryId);
    Page<WatchEntryResponse> getUserLibrary(String username, WatchStatus status, Pageable pageable);
}
