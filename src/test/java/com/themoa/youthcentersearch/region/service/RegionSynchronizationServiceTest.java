package com.themoa.youthcentersearch.region.service;

import com.themoa.youthcentersearch.policy.domain.RegionCode;
import com.themoa.youthcentersearch.policy.region.RegionAliasCatalog;
import com.themoa.youthcentersearch.policy.region.RegionCatalog;
import com.themoa.youthcentersearch.policy.region.RegionNormalizer;
import com.themoa.youthcentersearch.policy.repository.RegionCodeRepository;
import com.themoa.youthcentersearch.region.config.RegionSyncProperties;
import com.themoa.youthcentersearch.region.sgis.SgisRegionClient;
import com.themoa.youthcentersearch.region.sgis.dto.SgisRegionItem;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RegionSynchronizationServiceTest {
    @Test
    void upsertsProvinceAndCityAndRefreshesCatalog() {
        SgisRegionClient client = mock(SgisRegionClient.class);
        when(client.fetchProvinces()).thenReturn(List.of(new SgisRegionItem("47", "경상북도", "경상북도")));
        when(client.fetchChildren("47")).thenReturn(List.of(new SgisRegionItem("47850", "칠곡군", "경상북도 칠곡군")));
        InMemoryRegionRepository repository = new InMemoryRegionRepository();
        RegionAliasCatalog aliases = new RegionAliasCatalog();
        RegionCatalog catalog = new RegionCatalog(repository, aliases, new RegionNormalizer(aliases));
        catalog.allSpecificRegionsByLongestName();
        var service = new RegionSynchronizationService(properties(), client, repository, catalog,
                new RegionSynchronizationState(), new TransactionTemplate(transactionManager()),
                new MunicipalityHierarchyResolver(new RegionMunicipalityNormalizer(new RegionAdministrativeLevelResolver())));

        var result = service.synchronize();

        assertThat(result.provinceReceivedCount()).isEqualTo(1);
        assertThat(result.childReceivedCount()).isEqualTo(1);
        assertThat(result.insertedCount()).isEqualTo(2);
        RegionCode province = repository.findByRegionCode("47").orElseThrow();
        RegionCode county = repository.findByRegionCode("47850").orElseThrow();
        assertThat(county.getParent()).isSameAs(province);
        assertThat(county.getProvince()).isEqualTo("경상북도");
        assertThat(county.getCity()).isEqualTo("칠곡군");
        assertThat(county.getRegionLevel()).isEqualTo("CITY");
        assertThat(catalog.allSpecificRegionsByLongestName()).extracting(RegionCode::getRegionCode).contains("47850");
    }

    private RegionSyncProperties properties() {
        return new RegionSyncProperties(true, false, "0 0 4 1 * *",
                Duration.ZERO, Duration.ofSeconds(1), Duration.ofSeconds(1), 3,
                new RegionSyncProperties.Sgis("http://localhost", "key", "secret"));
    }

    private PlatformTransactionManager transactionManager() {
        return new PlatformTransactionManager() {
            @Override
            public TransactionStatus getTransaction(TransactionDefinition definition) {
                return new SimpleTransactionStatus();
            }

            @Override
            public void commit(TransactionStatus status) {
            }

            @Override
            public void rollback(TransactionStatus status) {
            }
        };
    }

    private static class InMemoryRegionRepository implements RegionCodeRepository {
        private final List<RegionCode> regions = new ArrayList<>();

        @Override
        public Optional<RegionCode> findByRegionCode(String regionCode) {
            return regions.stream().filter(region -> region.getRegionCode().equals(regionCode)).findFirst();
        }

        @Override
        public List<RegionCode> findByProvince(String province) {
            return regions.stream().filter(region -> region.getProvince().equals(province)).toList();
        }

        @Override
        public List<RegionCode> findByProvinceAndCity(String province, String city) {
            return regions.stream()
                    .filter(region -> region.getProvince().equals(province) && city.equals(region.getCity()))
                    .toList();
        }

        @Override
        public long countByRegionLevel(String regionLevel) {
            return regions.stream().filter(region -> region.getRegionLevel().equals(regionLevel)).count();
        }

        @Override
        public List<RegionCode> findAll() {
            return List.copyOf(regions);
        }

        @Override
        public <S extends RegionCode> S save(S entity) {
            regions.add(entity);
            return entity;
        }

        @Override
        public long count() {
            return regions.size();
        }

        @Override public void flush() { }
        @Override public <S extends RegionCode> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends RegionCode> List<S> saveAllAndFlush(Iterable<S> entities) { return saveAll(entities); }
        @Override public void deleteAllInBatch(Iterable<RegionCode> entities) { }
        @Override public void deleteAllByIdInBatch(Iterable<Integer> integers) { }
        @Override public void deleteAllInBatch() { }
        @Override public RegionCode getOne(Integer integer) { return null; }
        @Override public RegionCode getById(Integer integer) { return null; }
        @Override public RegionCode getReferenceById(Integer integer) { return null; }
        @Override public <S extends RegionCode> Optional<S> findOne(org.springframework.data.domain.Example<S> example) { return Optional.empty(); }
        @Override public <S extends RegionCode> List<S> findAll(org.springframework.data.domain.Example<S> example) { return List.of(); }
        @Override public <S extends RegionCode> List<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Sort sort) { return List.of(); }
        @Override public <S extends RegionCode> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Pageable pageable) { return org.springframework.data.domain.Page.empty(); }
        @Override public <S extends RegionCode> long count(org.springframework.data.domain.Example<S> example) { return 0; }
        @Override public <S extends RegionCode> boolean exists(org.springframework.data.domain.Example<S> example) { return false; }
        @Override public <S extends RegionCode, R> R findBy(org.springframework.data.domain.Example<S> example, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
        @Override public <S extends RegionCode> List<S> saveAll(Iterable<S> entities) { List<S> saved = new ArrayList<>(); entities.forEach(entity -> saved.add(save(entity))); return saved; }
        @Override public Optional<RegionCode> findById(Integer integer) { return Optional.empty(); }
        @Override public boolean existsById(Integer integer) { return false; }
        @Override public List<RegionCode> findAllById(Iterable<Integer> integers) { return List.of(); }
        @Override public void deleteById(Integer integer) { }
        @Override public void delete(RegionCode entity) { }
        @Override public void deleteAllById(Iterable<? extends Integer> integers) { }
        @Override public void deleteAll(Iterable<? extends RegionCode> entities) { }
        @Override public void deleteAll() { }
        @Override public List<RegionCode> findAll(org.springframework.data.domain.Sort sort) { return findAll(); }
        @Override public org.springframework.data.domain.Page<RegionCode> findAll(org.springframework.data.domain.Pageable pageable) { return org.springframework.data.domain.Page.empty(); }
    }
}
