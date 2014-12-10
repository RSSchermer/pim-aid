package models

object Step extends Enumeration {
  type Step = Value
  val GeneralInformation = Value("GeneralInformation")
  val MedicationList = Value("MedicationList")
  val ConditionalStatementSelection = Value("ConditionalStatementSelection")
  val UnconditionalStatementSelection = Value("UnconditionalStatementSelection")
  val SuggestionList = Value("SuggestionList")
}