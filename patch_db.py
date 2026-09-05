import re

with open('app/src/main/java/pk/vexel/financepassport/core/database/AppDatabase.kt', 'r') as f:
    content = f.read()

content = content.replace("const val DATABASE_VERSION = 15", "const val DATABASE_VERSION = 16")

entities_old = r"InvestmentEventEntity::class,\n\s+ReceivableEntity::class,\n\s+GoalEntity::class,\n\s+RecurringItemEntity::class,"
entities_new = """ReceivableEntity::class,
        SettlementEventEntity::class,
        SimpleInvestmentEntity::class,
        GoalEntity::class,
        CategoryEntity::class,
        RecurringTemplateEntity::class,
        ExpectedOccurrenceEntity::class,"""
content = re.sub(entities_old, entities_new, content)

daos_old = r"abstract fun investmentDao\(\): InvestmentDao\n\s+abstract fun receivableDao\(\): ReceivableDao\n\s+abstract fun goalDao\(\): GoalDao\n\s+abstract fun recurringItemDao\(\): RecurringItemDao"
daos_new = """abstract fun simpleInvestmentDao(): SimpleInvestmentDao
    abstract fun receivableDao(): ReceivableDao
    abstract fun settlementEventDao(): SettlementEventDao
    abstract fun goalDao(): GoalDao
    abstract fun categoryDao(): CategoryDao
    abstract fun recurringTemplateDao(): RecurringTemplateDao
    abstract fun expectedOccurrenceDao(): ExpectedOccurrenceDao"""
content = re.sub(daos_old, daos_new, content)

with open('app/src/main/java/pk/vexel/financepassport/core/database/AppDatabase.kt', 'w') as f:
    f.write(content)
