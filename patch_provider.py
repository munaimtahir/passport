import re

with open('app/src/main/java/pk/vexel/financepassport/core/database/DatabaseProvider.kt', 'r') as f:
    content = f.read()

migrations_old = r"MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15,"
migrations_new = "MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16,"
content = content.replace(migrations_old, migrations_new)

with open('app/src/main/java/pk/vexel/financepassport/core/database/DatabaseProvider.kt', 'w') as f:
    f.write(content)
