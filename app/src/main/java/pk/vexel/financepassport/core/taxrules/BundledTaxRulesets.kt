package pk.vexel.financepassport.core.taxrules

/**
 * Local ruleset repository for the rulesets shipped inside the app. Rulesets are packaged as
 * plain classpath resources under `src/main/resources/taxrules` (JSON files, not `assets`)
 * specifically so [TaxRulesetLoader] can be exercised end-to-end — file read included — from a
 * plain JVM test, without an Android Context or instrumentation.
 */
object BundledTaxRulesets {
    /**
     * Which version [loadDefault] resolves to for any *new* tax year/mapping/draft created from
     * now on. Bumping this never rewrites already-stored `rulesetVersion` columns on
     * `TaxYearEntity`/`TaxMappingEntity`/`TaxAnnualDraftEntity` rows — those are plain stored
     * strings, set once at creation — so existing records keep referencing whichever version was
     * current when they were created, and [loadVersion] can still resolve that exact version's
     * rules for them (see [pk.vexel.financepassport.core.database.FinanceRepository.prepareAnnualDraft],
     * which loads the tax year's own stored version rather than always using [loadDefault]).
     */
    const val CURRENT_VERSION = "pk-structural-2"

    private val VERSION_RESOURCES = mapOf(
        "pk-structural-1" to "taxrules/pk-structural-1.json",
        "pk-structural-2" to "taxrules/pk-structural-2.json",
    )

    fun loadDefault(): TaxRuleset = loadVersion(CURRENT_VERSION)

    fun loadVersion(version: String): TaxRuleset {
        val resourcePath = VERSION_RESOURCES[version]
            ?: throw RulesetError.MissingField("no bundled ruleset resource is registered for version '$version'")
        return load(resourcePath)
    }

    fun load(resourcePath: String): TaxRuleset {
        val stream = javaClass.classLoader?.getResourceAsStream(resourcePath)
            ?: throw RulesetError.MissingField("bundled ruleset resource '$resourcePath' was not found")
        val json = stream.bufferedReader().use { it.readText() }
        return TaxRulesetLoader.parse(json)
    }
}
