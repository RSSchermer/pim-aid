package binders

import play.api.mvc.PathBindable

import models._

object `package` {
  implicit def drugIDPathBindable(implicit longBinder: PathBindable[Long]) = 
    new PathBindable[DrugID] {
      def bind(key: String, value: String): Either[String, DrugID] =
        longBinder.bind(key, value).right.map(DrugID)
  
      def unbind(key: String, id: DrugID): String =
        longBinder.unbind(key, id.value)
    }
  
  implicit def drugGroupIDPathBindable(implicit longBinder: PathBindable[Long]) = 
    new PathBindable[DrugGroupID] {
      def bind(key: String, value: String): Either[String, DrugGroupID] =
        longBinder.bind(key, value).right.map(DrugGroupID)
  
      def unbind(key: String, id: DrugGroupID): String =
        longBinder.unbind(key, id.value)
    }
  
  implicit def expressionTermIDPathBindable(implicit longBinder: PathBindable[Long]) = 
    new PathBindable[ExpressionTermID] {
      def bind(key: String, value: String): Either[String, ExpressionTermID] =
        longBinder.bind(key, value).right.map(ExpressionTermID)
  
      def unbind(key: String, id: ExpressionTermID): String =
        longBinder.unbind(key, id.value)
    }
  
  implicit def genericTypeIDPathBindable(implicit longBinder: PathBindable[Long]) = 
    new PathBindable[GenericTypeID] {
      def bind(key: String, value: String): Either[String, GenericTypeID] =
        longBinder.bind(key, value).right.map(GenericTypeID)
  
      def unbind(key: String, id: GenericTypeID): String =
        longBinder.unbind(key, id.value)
    }

  implicit def medicationProductIDPathBindable(implicit longBinder: PathBindable[Long]) = 
    new PathBindable[MedicationProductID] {
      def bind(key: String, value: String): Either[String, MedicationProductID] =
        longBinder.bind(key, value).right.map(MedicationProductID)
  
      def unbind(key: String, id: MedicationProductID): String =
        longBinder.unbind(key, id.value)
    }

  implicit def ruleIDPathBindable(implicit longBinder: PathBindable[Long]) = 
    new PathBindable[RuleID] {
      def bind(key: String, value: String): Either[String, RuleID] =
        longBinder.bind(key, value).right.map(RuleID)
  
      def unbind(key: String, id: RuleID): String =
        longBinder.unbind(key, id.value)
    }

  implicit def suggestionTemplateIDPathBindable(implicit longBinder: PathBindable[Long]) = 
    new PathBindable[SuggestionTemplateID] {
      def bind(key: String, value: String): Either[String, SuggestionTemplateID] =
        longBinder.bind(key, value).right.map(SuggestionTemplateID)
  
      def unbind(key: String, id: SuggestionTemplateID): String =
        longBinder.unbind(key, id.value)
    }

  implicit def userTokenPathBindable(implicit stringBinder: PathBindable[String]) =
    new PathBindable[UserToken] {
      def bind(key: String, value: String): Either[String, UserToken] =
        stringBinder.bind(key, value).right.map(UserToken)
  
      def unbind(key: String, id: UserToken): String =
        stringBinder.unbind(key, id.value)
    }
}
