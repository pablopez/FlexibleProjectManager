package com.flexibleprojectmanager.platform.projects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.flexibleprojectmanager.platform.projects.domain.Project;
import com.flexibleprojectmanager.platform.projects.infrastructure.JpaProjectRepositoryAdapter;
import com.flexibleprojectmanager.platform.projects.infrastructure.ProjectJpaEntity;
import com.flexibleprojectmanager.platform.projects.infrastructure.SpringDataProjectRepository;

class JpaProjectRepositoryAdapterTest {
    @Test
    void usesTheContractPageSizeAndStableOrder() {
        SpringDataProjectRepository repository = org.mockito.Mockito.mock(SpringDataProjectRepository.class);
        when(repository.findByOrganizationId(any(), any())).thenReturn(new PageImpl<>(List.of()));
        UUID organizationId = UUID.randomUUID();

        new JpaProjectRepositoryAdapter(repository).findAll(organizationId, 0, 25, null);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findByOrganizationId(eq(organizationId), pageable.capture());
        assertEquals(25, pageable.getValue().getPageSize());
        assertEquals("createdAt: DESC,id: ASC", pageable.getValue().getSort().toString());
    }

    @Test
    void delegatesStatusFiltering() {
        SpringDataProjectRepository repository = org.mockito.Mockito.mock(SpringDataProjectRepository.class);
        when(repository.findByOrganizationIdAndStatus(any(), eq(Project.Status.ARCHIVED), any()))
                .thenReturn(new PageImpl<>(List.<ProjectJpaEntity>of()));

        new JpaProjectRepositoryAdapter(repository).findAll(UUID.randomUUID(), 1, 200, Project.Status.ARCHIVED);

        verify(repository).findByOrganizationIdAndStatus(any(), eq(Project.Status.ARCHIVED), any());
    }
}
