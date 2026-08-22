package pk.vexel.financepassport.core.taxrules

/**
 * Local ruleset repository for the rulesets shipped inside the app. Rulesets are packaged as
 * plain classpath resources under `src/main/resources/taxrules` (JSON files, not `assets`)
 * specifically so [TaxRulesetLoader] can be exercised end-to-end — file read included — from a
 * plain JVM test, without an Android Context or instrumentation.
 */
object BundledTaxRulesets {
    private const val DEFAULT_RESOURCE_PATH = "taxrules/pk-structural-1.json"

    fun loadDefault(): TaxRuleset = load(DEFAULT_RESOURCE_PATH)

    fun load(resourcePath: String): TaxRuleset {
        val stream = javaClass.classLoader?.getResourceAsStream(resourcePath)
            ?: throw RulesetError.MissingField("bundled ruleset resource '$resourcePath' was not found")
        val json = stream.bufferedReader().use { it.readText() }
        return TaxRulesetLoader.parse(json)
    }
}
