import re

with open('app/src/main/java/pk/vexel/financepassport/core/database/Daos.kt', 'r') as f:
    content = f.read()

# Add CategoryDao
category_dao = """
@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY family, name")
    fun observeAll(): Flow<List<CategoryEntity>>
    @Query("SELECT * FROM categories WHERE status = 'ACTIVE' ORDER BY family, name")
    fun observeActive(): Flow<List<CategoryEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(category: CategoryEntity)
    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CategoryEntity?
    @Query("SELECT * FROM categories ORDER BY family, name")
    suspend fun getAll(): List<CategoryEntity>
}
"""
if "CategoryDao" not in content:
    content = content.replace("interface FinancialContextDao", category_dao + "\n@Dao\ninterface FinancialContextDao")

# Add SettlementEventDao
settlement_dao = """
@Dao
interface SettlementEventDao {
    @Query("SELECT * FROM settlement_events WHERE entityId = :entityId ORDER BY dateEpochDay DESC")
    fun observeForEntity(entityId: String): Flow<List<SettlementEventEntity>>
    @Query("SELECT * FROM settlement_events WHERE entityId = :entityId ORDER BY dateEpochDay DESC")
    suspend fun getForEntity(entityId: String): List<SettlementEventEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(event: SettlementEventEntity)
    @Query("SELECT * FROM settlement_events ORDER BY dateEpochDay DESC")
    suspend fun getAll(): List<SettlementEventEntity>
}
"""
if "SettlementEventDao" not in content:
    content = content.replace("interface ReceivableDao", settlement_dao + "\n@Dao\ninterface ReceivableDao")

# Replace InvestmentDao
investment_old = r"@Dao\s+interface InvestmentDao\s+\{[^\}]+\}"
investment_new = """@Dao
interface SimpleInvestmentDao {
    @Query("SELECT * FROM simple_investments ORDER BY status, title")
    fun observeAll(): Flow<List<SimpleInvestmentEntity>>
    @Query("SELECT * FROM simple_investments ORDER BY status, title")
    suspend fun getAll(): List<SimpleInvestmentEntity>
    @Query("SELECT * FROM simple_investments WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): SimpleInvestmentEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(investment: SimpleInvestmentEntity)
}"""
content = re.sub(investment_old, investment_new, content)

# Replace RecurringItemDao
recurring_old = r"@Dao\s+interface RecurringItemDao\s+\{[^\}]+\}"
recurring_new = """@Dao
interface RecurringTemplateDao {
    @Query("SELECT * FROM recurring_templates WHERE status != 'ARCHIVED' ORDER BY title")
    fun observeActive(): Flow<List<RecurringTemplateEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(template: RecurringTemplateEntity)
    @Query("SELECT * FROM recurring_templates ORDER BY title")
    suspend fun getAll(): List<RecurringTemplateEntity>
    @Query("SELECT * FROM recurring_templates WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): RecurringTemplateEntity?
    @Query("UPDATE recurring_templates SET status = :status, updatedAtEpochMillis = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, updatedAt: Long)
}

@Dao
interface ExpectedOccurrenceDao {
    @Query("SELECT * FROM expected_occurrences WHERE templateId = :templateId ORDER BY dueDateEpochDay ASC")
    fun observeForTemplate(templateId: String): Flow<List<ExpectedOccurrenceEntity>>
    @Query("SELECT * FROM expected_occurrences WHERE templateId = :templateId ORDER BY dueDateEpochDay ASC")
    suspend fun getForTemplate(templateId: String): List<ExpectedOccurrenceEntity>
    @Query("SELECT * FROM expected_occurrences WHERE status = :status ORDER BY dueDateEpochDay ASC")
    fun observeByStatus(status: String): Flow<List<ExpectedOccurrenceEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(occurrence: ExpectedOccurrenceEntity)
    @Query("SELECT * FROM expected_occurrences ORDER BY dueDateEpochDay DESC")
    suspend fun getAll(): List<ExpectedOccurrenceEntity>
    @Query("SELECT * FROM expected_occurrences WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ExpectedOccurrenceEntity?
    @Query("SELECT * FROM expected_occurrences WHERE templateId = :templateId AND status IN ('UPCOMING', 'DUE', 'OVERDUE') ORDER BY dueDateEpochDay ASC")
    suspend fun getUnconfirmedForTemplate(templateId: String): List<ExpectedOccurrenceEntity>
}"""
content = re.sub(recurring_old, recurring_new, content)

with open('app/src/main/java/pk/vexel/financepassport/core/database/Daos.kt', 'w') as f:
    f.write(content)
