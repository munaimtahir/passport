import re

with open('app/src/androidTest/java/pk/vexel/financepassport/core/calendar/ReminderDeviceTest.kt', 'r') as f:
    content = f.read()

content = content.replace("recurringItemDao()", "recurringTemplateDao()")
old = r"RecurringTemplateEntity\([^)]+\)"
new = """RecurringTemplateEntity(
                id = "rent", title = "Rent", eventType = FinancialEventType.EXPENSE.name, amountMode = "FIXED",
                expectedAmountMinor = 50_000, currency = "PKR", frequency = "MONTHLY", intervalCount = 1,
                startDateEpochDay = today.toEpochDay(), endDateEpochDay = null, defaultAccountId = account.id,
                defaultContextId = null, defaultCategoryId = "Housing", counterparty = null, notes = null,
                status = "ACTIVE", createdAtEpochMillis = 1, updatedAtEpochMillis = 1
            )"""
content = re.sub(old, new, content)

with open('app/src/androidTest/java/pk/vexel/financepassport/core/calendar/ReminderDeviceTest.kt', 'w') as f:
    f.write(content)
