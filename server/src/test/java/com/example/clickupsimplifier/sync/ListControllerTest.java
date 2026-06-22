package com.example.clickupsimplifier.sync;

import com.example.clickupsimplifier.persistence.WorkspaceList;
import com.example.clickupsimplifier.persistence.WorkspaceListRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ListController.class)
class ListControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean WorkspaceListRepository listRepo;

    @Test
    void getLists_returns200WithAllLists() throws Exception {
        when(listRepo.findAllOrderedForDisplay()).thenReturn(List.of(
                new WorkspaceList("l1", "Alpha", "s1", "f1", false),
                new WorkspaceList("l2", "Beta", "s1", null, true)
        ));

        mockMvc.perform(get("/api/lists"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("l1"))
                .andExpect(jsonPath("$[0].syncEnabled").value(false))
                .andExpect(jsonPath("$[1].id").value("l2"))
                .andExpect(jsonPath("$[1].syncEnabled").value(true));
    }

    @Test
    void putSyncEnabled_existing_returns204() throws Exception {
        when(listRepo.updateSyncEnabled("l1", true)).thenReturn(1);

        mockMvc.perform(put("/api/lists/l1/sync-enabled")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\": true}"))
                .andExpect(status().isNoContent());

        verify(listRepo).updateSyncEnabled("l1", true);
    }

    @Test
    void putSyncEnabled_nonExisting_returns404() throws Exception {
        when(listRepo.updateSyncEnabled("missing", false)).thenReturn(0);

        mockMvc.perform(put("/api/lists/missing/sync-enabled")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\": false}"))
                .andExpect(status().isNotFound());
    }
}
