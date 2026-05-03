# Implementation Plan - KPI Data Saving + AI Analysis (Gemini + Risk + 8D)

This plan outlines the steps to implement a robust KPI management system that saves imported data, performs line-by-line AI analysis using Gemini (with RAG integration), and displays it in an advanced dashboard.

## User Review Required

> [!IMPORTANT]
> The AI analysis will be performed line-by-line for each KPI. This might increase the number of API calls to Gemini. I will optimize this by checking if an analysis already exists and using batching where possible (though the user requested line-by-line).

> [!NOTE]
> I will add an `import_session_id` to both `kpi_import_preview` and `kpi_analysis` tables to associate the data with a specific import session, ensuring data integrity.

## Proposed Changes

### Backend - Entities & Repositories

#### [NEW] [KpiImportPreview.java](file:///c:/Users/moote/QHSE-Analytics/QHSEAnalytics/src/main/java/com/QHSEAnalytics/entity/KpiImportPreview.java)
- Entity for `kpi_import_preview` table.
- Fields: `id`, `importSession`, `kpiName`, `category`, `unit`, `definition`, `valueN`, `valueN1`, `status`, `commentaire`, `variationPercent`, `ecart`, `createdAt`.

#### [NEW] [KpiAnalysis.java](file:///c:/Users/moote/QHSE-Analytics/QHSEAnalytics/src/main/java/com/QHSEAnalytics/entity/KpiAnalysis.java)
- Entity for `kpi_analysis` table.
- Fields: `id`, `importSession`, `kpiName`, `riskLevel`, `issueDetected`, `correctiveAction`, `preventiveAction`, `immediateAction`, `requires8d`, `eightDDetails` (TEXT for JSON), `aiNote`, `createdAt`.

#### [NEW] [KpiImportPreviewRepository.java](file:///c:/Users/moote/QHSE-Analytics/QHSEAnalytics/src/main/java/com/QHSEAnalytics/repository/KpiImportPreviewRepository.java)
#### [NEW] [KpiAnalysisRepository.java](file:///c:/Users/moote/QHSE-Analytics/QHSEAnalytics/src/main/java/com/QHSEAnalytics/repository/KpiAnalysisRepository.java)

---

### Backend - DTOs & Services

#### [NEW] [KpiAnalysisResult.java](file:///c:/Users/moote/QHSE-Analytics/QHSEAnalytics/src/main/java/com/QHSEAnalytics/dto/response/KpiAnalysisResult.java)
- DTO to capture the structured output from Gemini for a single KPI.

#### [NEW] [KpiEnrichmentService.java](file:///c:/Users/moote/QHSE-Analytics/QHSEAnalytics/src/main/java/com/QHSEAnalytics/service/KpiEnrichmentService.java)
- Service to handle:
    - Saving preview data.
    - Fetching RAG knowledge for a KPI.
    - Constructing the Gemini prompt.
    - Calling Gemini for each KPI.
    - Parsing and saving analysis results.
    - Updating RAG database if new definitions/thresholds are generated.

#### [MODIFY] [DashboardAnalysteService.java](file:///c:/Users/moote/QHSE-Analytics/QHSEAnalytics/src/main/java/com/QHSEAnalytics/service/DashboardAnalysteService.java)
- Update to fetch data from `KpiImportPreview` and `KpiAnalysis` for the advanced dashboard view.

---

### Frontend - Angular

#### [MODIFY] [dashboard-analyste.component.ts](file:///c:/Users/moote/QHSE-Analytics/frontend/src/app/features/analyste/pages/dashboard/dashboard-analyste.component.ts)
- Update state management to handle the new KPI data structure.
- Implement filtering logic (Category, Risk, Status).

#### [MODIFY] [dashboard-analyste.component.html](file:///c:/Users/moote/QHSE-Analytics/frontend/src/app/features/analyste/pages/dashboard/dashboard-analyste.component.html)
- Replace/Enhance the table with visual indicators (🔴, 🟠, 🟢).
- Add filters UI.
- Implement expandable rows for detailed AI analysis (Risks, Actions, 8D).

#### [MODIFY] [dashboard-analyste.component.css](file:///c:/Users/moote/QHSE-Analytics/frontend/src/app/features/analyste/pages/dashboard/dashboard-analyste.component.css)
- Add styles for the new dashboard elements, badges, and expandable sections.

---

## Verification Plan

### Automated Tests
- JUnit tests for `KpiEnrichmentService` to verify prompt generation and response parsing.
- Integration test for RAG update logic.

### Manual Verification
- Upload a file and verify that:
    1. Data is saved in `kpi_import_preview`.
    2. Gemini is called for each KPI.
    3. Results are saved in `kpi_analysis`.
    4. Dashboard displays KPIs with correct risk badges and expandable analysis.
    5. Filters work as expected.
    6. 8D section appears only when required.
