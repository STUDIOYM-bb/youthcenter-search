package com.themoa.youthcentersearch.region.service;

import com.themoa.youthcentersearch.policy.domain.RegionCode;
import com.themoa.youthcentersearch.policy.domain.RegionSyncRun;
import com.themoa.youthcentersearch.policy.region.RegionCatalog;
import com.themoa.youthcentersearch.policy.repository.RegionExternalCodeRepository;
import com.themoa.youthcentersearch.policy.repository.RegionSyncErrorRepository;
import com.themoa.youthcentersearch.policy.repository.RegionSyncRunRepository;
import com.themoa.youthcentersearch.region.config.RegionSyncProperties;
import com.themoa.youthcentersearch.region.sgis.SgisRegionClient;
import com.themoa.youthcentersearch.region.sgis.dto.SgisRegionItem;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RegionSynchronizationServiceTest {
    @Test
    void upsertsProvinceAndCityAndRefreshesCatalog() {
        SgisRegionClient client = mock(SgisRegionClient.class);
        when(client.fetchProvinces()).thenReturn(List.of(new SgisRegionItem("47", "경상북도", "경상북도")));
        when(client.fetchChildren("47")).thenReturn(List.of(new SgisRegionItem("47850", "칠곡군", "경상북도 칠곡군")));
        RegionCatalog catalog = mock(RegionCatalog.class);
        RegionProvincePersistenceService persistenceService = mock(RegionProvincePersistenceService.class);
        RegionCode province = new RegionCode(null, "P:경상북도", "경상북도", null, "PROVINCE");
        RegionCode county = new RegionCode(province, "M:경상북도:칠곡군", "경상북도", "칠곡군", "CITY");
        when(persistenceService.upsertProvince("47", "경상북도"))
                .thenReturn(new RegionProvincePersistenceService.PersistedRegion(province, 1, 0, 0));
        when(persistenceService.upsertMunicipality(eq(province), any(NormalizedMunicipality.class)))
                .thenReturn(new RegionProvincePersistenceService.PersistedRegion(county, 1, 0, 0));
        RegionSyncRunRepository runRepository = mock(RegionSyncRunRepository.class);
        when(runRepository.save(any(RegionSyncRun.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var service = new RegionSynchronizationService(properties(), client, catalog, new RegionSynchronizationState(),
                new MunicipalityHierarchyResolver(new RegionMunicipalityNormalizer(new RegionAdministrativeLevelResolver())),
                persistenceService, runRepository, mock(RegionSyncErrorRepository.class), mock(RegionExternalCodeRepository.class));

        var result = service.synchronize();

        assertThat(result.provinceReceivedCount()).isEqualTo(1);
        assertThat(result.childReceivedCount()).isEqualTo(1);
        assertThat(result.insertedCount()).isEqualTo(2);
        verify(persistenceService).upsertProvince("47", "경상북도");
        verify(persistenceService).upsertMunicipality(eq(province), argThat(region -> region.municipalityName().equals("칠곡군")));
        verify(catalog).refreshCache();
    }

    private RegionSyncProperties properties() {
        return new RegionSyncProperties(true, false, "0 0 4 1 * *",
                Duration.ZERO, Duration.ofSeconds(1), Duration.ofSeconds(1), 3,
                new RegionSyncProperties.Sgis("http://localhost", "key", "secret"));
    }
}
