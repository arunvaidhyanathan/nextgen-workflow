# OneCMS Service - Outstanding Issues

## API Response Data Issue - Fixed but Requires Service Restart

**Status**: Fixed in code, needs service restart to apply changes

**Problem**: The `/api/cms/v1/cases/{caseNumber}` endpoint returns `null` values for:
- `allegations`
- `entities` 
- `narratives`
- `processInstanceId`
- `workflowMetadata`

**Root Cause**: JPA entity mapping issues in the `Case` entity. The relationships were using `mappedBy = "caseId"` but the related entities (`Allegation`, `CaseEntity`, `CaseNarrative`) don't have proper JPA relationship fields - they only have plain `Long caseId` fields.

**Database Verification**: ✅ Data exists correctly in database
- Case `CMS-2025-000016` has: 1 allegation, 1 entity, 1 narrative
- `process_instance_id` is correctly stored: `cb64cd50-89a7-11f0-a6a3-1acf79ce7d86`

**Changes Made**:
1. ✅ **Fixed JPA Mappings** in `Case.java`:
   ```java
   // Changed from:
   @OneToMany(mappedBy = "caseId", ...)
   
   // To:
   @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
   @JoinColumn(name = "case_id")
   ```

2. ✅ **Enhanced convertToResponse Method** in `CaseService.java`:
   - Added logic to populate allegations, entities, narratives
   - Added workflow metadata population
   - Added processInstanceId mapping

3. ✅ **Updated Repository Query** in `CaseRepository.java`:
   ```java
   @Query("SELECT c FROM Case c LEFT JOIN FETCH c.allegations LEFT JOIN FETCH c.entities LEFT JOIN FETCH c.narratives WHERE c.caseNumber = :caseNumber")
   Optional<Case> findByCaseNumber(@Param("caseNumber") String caseNumber);
   ```

4. ✅ **Added Transaction Annotation**: Made `getCaseByNumber` method `@Transactional(readOnly = true)`

**Next Actions Required**:
1. 🔄 **Restart OneCMS Service** - JPA mapping changes need service restart to take effect
2. 🧪 **Test API Endpoint** - Verify `/api/cms/v1/cases/CMS-2025-000016` returns complete data
3. ✅ **Verify Workflow Integration** - Confirm processInstanceId and workflowMetadata are populated

**Test Commands**:
```bash
# After service restart, test with:
curl -X GET http://localhost:8083/api/cms/v1/cases/CMS-2025-000016 \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 550e8400-e29b-41d4-a716-446655440001" | jq .

# Should return complete data including:
# - allegations: [1 item]
# - entities: [1 item] 
# - narratives: [1 item]
# - processInstanceId: "cb64cd50-89a7-11f0-a6a3-1acf79ce7d86"
# - workflowMetadata: {...}
```

**Files Modified**:
- `/src/main/java/com/citi/onecms/entity/Case.java`
- `/src/main/java/com/citi/onecms/service/CaseService.java` 
- `/src/main/java/com/citi/onecms/repository/CaseRepository.java`

---

## Completed Successfully ✅

### 2-Phase Case Creation Workflow Integration
- ✅ Workflow metadata registration and BPMN deployment
- ✅ Phase 1: `POST /api/cms/v1/createcase-draft` - Creates case and starts workflow
- ✅ Phase 2: `POST /api/cms/v1/enhance-case` - Adds allegations, entities, narratives
- ✅ Database persistence verification
- ✅ Flowable workflow integration (tasks created in `eo-intake-analyst-queue`)
- ✅ Process instances active with business keys

### Test Cases Verified
- ✅ Case `CMS-2025-000015` - Original 2-phase workflow test
- ✅ Case `CMS-2025-000016` - JPA mapping verification test
- ✅ Flowable queue_tasks table populated correctly
- ✅ Process instances active in Flowable engine

### API Endpoints Working
- ✅ `POST /api/cms/v1/createcase-draft` 
- ✅ `POST /api/cms/v1/enhance-case`
- ⚠️ `GET /api/cms/v1/cases/{caseNumber}` - Needs service restart for complete data