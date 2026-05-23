package com.QHSEAnalytics.service;

import com.QHSEAnalytics.shared.dto.request.CreateKpiRequest;
import com.QHSEAnalytics.shared.dto.request.UpdateKpiRequest;
import com.QHSEAnalytics.shared.dto.response.KpiDeleteResponse;
import com.QHSEAnalytics.shared.dto.response.KpiResponse;
import com.QHSEAnalytics.shared.entity.CategorieKpi;
import com.QHSEAnalytics.shared.entity.Kpi;
import com.QHSEAnalytics.shared.enums.UniteKpi;
import com.QHSEAnalytics.shared.enums.Direction;
import com.QHSEAnalytics.shared.exception.InvalidSeuilException;
import com.QHSEAnalytics.shared.exception.KpiAlreadyExistsException;
import com.QHSEAnalytics.shared.exception.KpiNotFoundException;
import com.QHSEAnalytics.shared.repository.CategorieKpiRepository;
import com.QHSEAnalytics.shared.repository.KpiRepository;
import com.QHSEAnalytics.kpi.service.KpiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("KpiService — tests unitaires")
class KpiServiceTest {

    @Mock KpiRepository kpiRepository;
    @Mock CategorieKpiRepository categorieKpiRepository;

    @InjectMocks KpiService kpiService;

    private CategorieKpi categorieQ;
    private Kpi kpiAccidents;

    @BeforeEach
    void setUp() {
        categorieQ = new CategorieKpi();
        categorieQ.setId(1L);
        categorieQ.setCode("Q");
        categorieQ.setLibelle("Qualité");

        kpiAccidents = Kpi.builder()
                .id(1L)
                .nom("Taux d'accidents")
                .definition("Nombre d'accidents / heures travaillées")
                .unite(UniteKpi.POURCENTAGE)
                .categorieKpi(categorieQ)
                .seuilFaible(5.0)
                .seuilModere(10.0)
                .seuilCritique(20.0)
                .ordre(1)
                .isActive(true)
                .direction(Direction.LOWER_IS_BETTER)
                .build();
    }



    @Test
    @DisplayName("getAllKpis retourne la liste des KPIs actifs")
    void getAllKpis_returnsActiveList() {
        when(kpiRepository.findByIsActiveTrueOrderByOrdreAsc()).thenReturn(List.of(kpiAccidents));

        List<KpiResponse> result = kpiService.getAllKpis();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getNom()).isEqualTo("Taux d'accidents");
        assertThat(result.get(0).getCategorieCode()).isEqualTo("Q");
    }

    @Test
    @DisplayName("getAllKpis retourne liste vide si aucun KPI actif")
    void getAllKpis_returnsEmptyList_whenNoActiveKpis() {
        when(kpiRepository.findByIsActiveTrueOrderByOrdreAsc()).thenReturn(List.of());

        List<KpiResponse> result = kpiService.getAllKpis();

        assertThat(result).isEmpty();
    }



    @Test
    @DisplayName("getKpiById retourne le KPI correspondant")
    void getKpiById_returnsKpi() {
        when(kpiRepository.findById(1L)).thenReturn(Optional.of(kpiAccidents));

        KpiResponse result = kpiService.getKpiById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getNom()).isEqualTo("Taux d'accidents");
    }

    @Test
    @DisplayName("getKpiById lève KpiNotFoundException si KPI inexistant")
    void getKpiById_throwsNotFound_whenAbsent() {
        when(kpiRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> kpiService.getKpiById(99L))
                .isInstanceOf(KpiNotFoundException.class)
                .hasMessageContaining("99");
    }



    @Test
    @DisplayName("createKpi crée et retourne le nouveau KPI")
    void createKpi_success() {
        CreateKpiRequest request = buildCreateRequest("Nouveau KPI", 5.0, 10.0, 20.0);

        when(categorieKpiRepository.findByCode("Q")).thenReturn(Optional.of(categorieQ));
        when(kpiRepository.existsByNomAndCategorieKpi(any(), any())).thenReturn(false);
        when(kpiRepository.save(any(Kpi.class))).thenAnswer(inv -> {
            Kpi k = inv.getArgument(0);
            k.setId(2L);
            return k;
        });

        KpiResponse result = kpiService.createKpi(request);

        assertThat(result.getNom()).isEqualTo("Nouveau KPI");
        assertThat(result.getSeuilFaible()).isEqualTo(5.0);
        verify(kpiRepository).save(any(Kpi.class));
    }

    @Test
    @DisplayName("createKpi rejette des seuils invalides (faible >= modere)")
    void createKpi_throwsInvalidSeuil_whenThresholdsWrong() {
        CreateKpiRequest request = buildCreateRequest("KPI Invalide", 15.0, 10.0, 20.0);

        when(categorieKpiRepository.findByCode("Q")).thenReturn(Optional.of(categorieQ));
        when(kpiRepository.existsByNomAndCategorieKpi(any(), any())).thenReturn(false);

        assertThatThrownBy(() -> kpiService.createKpi(request))
                .isInstanceOf(InvalidSeuilException.class);
    }

    @Test
    @DisplayName("createKpi rejette un doublon dans la même catégorie")
    void createKpi_throwsConflict_whenDuplicate() {
        CreateKpiRequest request = buildCreateRequest("Taux d'accidents", 5.0, 10.0, 20.0);

        when(categorieKpiRepository.findByCode("Q")).thenReturn(Optional.of(categorieQ));
        when(kpiRepository.existsByNomAndCategorieKpi(any(), any())).thenReturn(true);

        assertThatThrownBy(() -> kpiService.createKpi(request))
                .isInstanceOf(KpiAlreadyExistsException.class);
    }



    @Test
    @DisplayName("updateKpi met à jour uniquement les champs fournis")
    void updateKpi_partialUpdate() {
        UpdateKpiRequest request = new UpdateKpiRequest();
        request.setSeuilCritique(25.0);

        when(kpiRepository.findById(1L)).thenReturn(Optional.of(kpiAccidents));
        when(kpiRepository.existsByNomAndCategorieKpiAndIdNot(any(), any(), any())).thenReturn(false);
        when(kpiRepository.save(any(Kpi.class))).thenReturn(kpiAccidents);

        kpiService.updateKpi(1L, request);

        verify(kpiRepository).save(argThat(k -> k.getSeuilCritique() == 25.0));
    }

    @Test
    @DisplayName("updateKpi lève KpiNotFoundException si KPI inexistant")
    void updateKpi_throwsNotFound_whenAbsent() {
        when(kpiRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> kpiService.updateKpi(99L, new UpdateKpiRequest()))
                .isInstanceOf(KpiNotFoundException.class);
    }



    @Test
    @DisplayName("deleteKpi supprime définitivement si aucune donnée liée")
    void deleteKpi_hardDelete_whenNoLinkedData() {
        when(kpiRepository.findById(1L)).thenReturn(Optional.of(kpiAccidents));
        when(kpiRepository.hasLinkedData(1L)).thenReturn(false);
        KpiDeleteResponse response = kpiService.deleteKpi(1L);

        assertThat(response.isDeleted()).isTrue();
        verify(kpiRepository).deleteById(1L);
    }

    @Test
    @DisplayName("deleteKpi désactive si données historiques présentes")
    void deleteKpi_softDelete_whenLinkedData() {
        when(kpiRepository.findById(1L)).thenReturn(Optional.of(kpiAccidents));
        when(kpiRepository.hasLinkedData(1L)).thenReturn(true);
        when(kpiRepository.save(any())).thenReturn(kpiAccidents);
        KpiDeleteResponse response = kpiService.deleteKpi(1L);

        assertThat(response.isDeleted()).isFalse();
        assertThat(response.getMessage()).contains("désactivé");
        verify(kpiRepository, never()).deleteById(any());
    }



    private CreateKpiRequest buildCreateRequest(String nom, double faible, double modere, double critique) {
        CreateKpiRequest req = new CreateKpiRequest();
        req.setNom(nom);
        req.setDefinition("Définition test");
        req.setUnite(UniteKpi.POURCENTAGE);
        req.setCategorieCode("Q");
        req.setSeuilFaible(faible);
        req.setSeuilModere(modere);
        req.setSeuilCritique(critique);
        req.setOrdre(1);
        req.setDirection(Direction.LOWER_IS_BETTER);
        return req;
    }
}
