package com.example.clickupsimplifier.sync;

import com.example.clickupsimplifier.persistence.SyncSet;
import com.example.clickupsimplifier.persistence.SyncSetRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SyncController.class)
class SyncControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean WorkspaceSyncService syncService;
    @MockitoBean SyncSetRepository syncSetRepo;

    @Test
    void triggerFullPull_whenIdle_returns202() throws Exception {
        when(syncService.getStatus()).thenReturn(SyncJobStatus.idle());

        mockMvc.perform(post("/api/sync/full-pull"))
                .andExpect(status().isAccepted());

        verify(syncService).triggerPull(null);
    }

    @Test
    void triggerFullPull_whenRunning_returns409() throws Exception {
        when(syncService.getStatus()).thenReturn(SyncJobStatus.running(Instant.now()));

        mockMvc.perform(post("/api/sync/full-pull"))
                .andExpect(status().isConflict());

        verify(syncService, never()).triggerPull(any());
    }

    @Test
    void getStatus_returns200WithStateAndSyncSets() throws Exception {
        Instant now = Instant.now();
        when(syncService.getStatus()).thenReturn(SyncJobStatus.completed(now, now));
        when(syncSetRepo.findAll()).thenReturn(List.of(
                new SyncSet("dictionaries", now),
                new SyncSet("tasks", now)
        ));

        mockMvc.perform(get("/api/sync/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("COMPLETED"))
                .andExpect(jsonPath("$.syncSets.dictionaries").exists())
                .andExpect(jsonPath("$.syncSets.tasks").exists());
    }
}
