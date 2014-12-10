package constraints

import models.{GenericTypes, DrugGroups}
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.db.slick._

object MedicationProductTemplateConstraint {
  def apply(implicit s: Session): Constraint[String] = {
    val groupNames = DrugGroups.list.map(_.name.toLowerCase)
    val typeNames = GenericTypes.list.map(_.name.toLowerCase)

    Constraint[String]("constraints.medicationProductTemplate")({ template =>
      """\{\{(type|group)\(([^\)]+)\)\}\}""".r.findAllMatchIn(template).map(m => {
        groupNames.contains(m.group(2).toLowerCase) || typeNames.contains(m.group(2).toLowerCase)
      }).forall(_ == true) match {
        case true => Valid
        case false => Invalid("Contains a reference to a type or group that does not exist.")
      }
    })
  }
}
