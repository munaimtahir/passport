package pk.vexel.financepassport.core.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinVerifierTest {
    @Test fun pinDigestRoundTripAndWrongPin() {
        val record = PinVerifier.create("1234".toCharArray())
        assertTrue(PinVerifier.verify("1234".toCharArray(), record))
        assertFalse(PinVerifier.verify("4321".toCharArray(), record))
    }
}
