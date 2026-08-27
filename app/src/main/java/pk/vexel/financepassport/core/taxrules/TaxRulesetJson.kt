package pk.vexel.financepassport.core.taxrules

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * A ruleset failed to load. Thrown instead of silently falling back to a wrong/default mapping,
 * per the product rule that tax rules are versioned configuration, never invented at runtime.
 */
sealed class RulesetError(message: String) : Exception(message) {
    class MalformedJson(detail: String) : RulesetError("Ruleset JSON is malformed: $detail")
    class MissingField(field: String) : RulesetError("Ruleset JSON is missing required field '$field'")
    class InvalidRule(detail: String) : RulesetError("Ruleset rule is invalid: $detail")
    class UnknownTaxEventType(value: String) : RulesetError("Ruleset references unknown tax event type '$value'")
}

/**
 * Parses a [TaxRuleset] from its JSON representation. Pure/host-testable: takes JSON text, not a
 * file path or Android [android.content.Context], so the same parser runs identically in JVM
 * tests and on-device.
 */
object TaxRulesetLoader {
    fun parse(json: String): TaxRuleset {
        val root = try {
            Json.parseToJsonElement(json).jsonObject
        } catch (failure: Exception) {
            throw RulesetError.MalformedJson(failure.message ?: failure.toString())
        }

        val jurisdiction = root["jurisdiction"]?.jsonPrimitive?.contentOrNull
            ?: throw RulesetError.MissingField("jurisdiction")
        val version = root["version"]?.jsonPrimitive?.contentOrNull
            ?: throw RulesetError.MissingField("version")
        val taxYear = root["taxYear"]?.jsonPrimitive?.contentOrNull
            ?: throw RulesetError.MissingField("taxYear")
        val rulesArray = root["rules"]?.jsonArray
            ?: throw RulesetError.MissingField("rules")

        val rules = rulesArray.mapIndexed { index, element ->
            val obj = try {
                element.jsonObject
            } catch (failure: Exception) {
                throw RulesetError.InvalidRule("rules[$index] is not an object")
            }
            val eventTypeRaw = obj["eventType"]?.jsonPrimitive?.contentOrNull
                ?: throw RulesetError.InvalidRule("rules[$index] is missing 'eventType'")
            val eventType = try {
                TaxEventType.valueOf(eventTypeRaw)
            } catch (failure: IllegalArgumentException) {
                throw RulesetError.UnknownTaxEventType(eventTypeRaw)
            }
            val sectionCode = obj["sectionCode"]?.jsonPrimitive?.contentOrNull
                ?: throw RulesetError.InvalidRule("rules[$index] is missing 'sectionCode'")
            val categoryCode = obj["categoryCode"]?.jsonPrimitive?.contentOrNull
                ?: throw RulesetError.InvalidRule("rules[$index] is missing 'categoryCode'")
            val evidence = obj["evidence"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
            val ambiguous = obj["ambiguous"]?.jsonPrimitive?.booleanOrNull ?: false
            TaxRule(eventType, sectionCode, categoryCode, evidence, ambiguous)
        }
        if (rules.isEmpty()) throw RulesetError.InvalidRule("a ruleset must contain at least one rule")

        return TaxRuleset(jurisdiction, taxYear, version, rules)
    }
}
