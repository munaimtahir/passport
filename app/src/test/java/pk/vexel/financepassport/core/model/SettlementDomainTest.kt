package pk.vexel.financepassport.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettlementDomainTest {
    @Test fun loanInstallmentReconcilesCashPrincipalAndFinancingCost() {
        val split = LiabilityInstallmentSplit(100_000, 80_000, 20_000)
        assertEquals(100_000, split.principalMinor + split.financingCostMinor)
    }

    @Test fun moneyLentReceiptIsNotIncome() {
        val result = settleReceivable(50_000, 50_000, "MONEY_LENT")
        assertEquals(0, result.remainingMinor)
        assertFalse(result.recognizesIncome)
    }

    @Test fun incomeDueReceiptRecognizesOnlyTheReceivedAmount() {
        val result = settleReceivable(10_000, 6_000, "INCOME_DUE")
        assertEquals(4_000, result.remainingMinor)
        assertTrue(result.recognizesIncome)
    }

    @Test fun redemptionComponentsReconcileToNetCash() {
        val result = InvestmentRedemptionReconciliation(500_000, 60_000, 9_000, 551_000)
        assertEquals(551_000, result.netReceivedMinor)
    }
}
