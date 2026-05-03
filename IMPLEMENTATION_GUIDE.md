# 🛠️ QHSE-Analytics Implementation Guide

## Phase 1: AI Analysis Enhancement ✨

### 1.1 RAG Knowledge Base Implementation

Create new service for RAG:

````java
// filepath: c:\Users\moote\QHSE-Analytics\QHSEAnalytics\src\main\java\com\QHSEAnalytics\service\RagKnowledgeService.java
@Service
@Slf4j
public class RagKnowledgeService {
    
    @Autowired
    private RagKnowledgeRepository ragRepository;
    
    public void saveKnowledge(String kpiName, String definition, 
                             Map<String, Object> thresholds,
                             String category) {
        RagKnowledge knowledge = RagKnowledge.builder()
            .kpiName(kpiName)
            .definition(definition)
            .thresholds(new JSONObject(thresholds).toString())
            .category(category)
            .createdAt(LocalDateTime.now())
            .build();
        
        ragRepository.save(knowledge);
        log.info("RAG knowledge saved for KPI: {}", kpiName);
    }
    
    public Optional<String> getContext(String kpiName) {
        return ragRepository.findByKpiName(kpiName)
            .map(k -> buildContext(k));
    }
    
    private String buildContext(RagKnowledge knowledge) {
        return String.format(
            "KPI: %s\nDefinition: %s\nCategory: %s\nThresholds: %s",
            knowledge.getKpiName(),
            knowledge.getDefinition(),
            knowledge.getCategory(),
            knowledge.getThresholds()
        );
    }
}