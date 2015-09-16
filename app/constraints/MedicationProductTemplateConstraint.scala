package constraints

import scala.concurrent.ExecutionContext
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

import play.api.data.validation.{Constraint, Invalid, Valid}

import model.PIMAidDBContext._
import model.PIMAidDBContext.driver.api._

object MedicationProductTemplateConstraint {
  def apply(implicit ec: ExecutionContext): Constraint[String] = {
    val groupNames = Await.result(db.run(DrugGroup.all.map(_.name.toLowerCase).result), 10 seconds)
    val typeNames = Await.result(db.run(GenericType.all.map(_.name.toLowerCase).result), 10 seconds)

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
