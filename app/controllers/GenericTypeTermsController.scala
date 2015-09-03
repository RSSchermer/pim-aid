package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import views._
import model.PIMAidDbContext._
import model.PIMAidDbContext.driver.api._

object GenericTypeTermsController extends Controller {
  val genericTypeTermForm = Form(
    mapping(
      "id" -> optional(longNumber.transform(
        (id: Long) => ExpressionTermID(id),
        (id: ExpressionTermID) => id.value
      )),
      "label" -> nonEmptyText.verifying("Must alphanumeric characters, dashes and underscores only.",
        _.matches("""[A-Za-z0-9\-_]+""")),
      "genericTypeId" -> longNumber.transform(
        (id: Long) => GenericTypeID(id),
        (genericTypeId: GenericTypeID) => genericTypeId.value
      )
    )(GenericTypeTerm.apply)(GenericTypeTerm.unapply)
  )

  def list = Action.async { implicit rs =>
    db.run(GenericTypeTerm.all.include(GenericTypeTerm.genericType).result).map { terms =>
      Ok(html.genericTypeTerms.list(terms))
    }
  }

  def create = Action.async { implicit rs =>
    db.run(GenericType.all.result).map { genericTypes =>
      Ok(html.genericTypeTerms.create(genericTypes, genericTypeTermForm))
    }
  }

  def save = Action.async { implicit rs =>
    genericTypeTermForm.bindFromRequest.fold(
      formWithErrors =>
        db.run(GenericType.all.result).map { genericTypes =>
          BadRequest(html.genericTypeTerms.create(genericTypes, formWithErrors))
        },
      drugTypeTerm =>
        db.run(GenericTypeTerm.insert(drugTypeTerm)).map { _ =>
          Redirect(routes.GenericTypeTermsController.list())
            .flashing("success" -> "The expression term was created successfully.")
        }
    )
  }

  def edit(id: ExpressionTermID) = Action.async { implicit rs =>
    db.run(for {
      termOption <- GenericTypeTerm.one(id).result
      genericTypes <- GenericType.all.result
    } yield termOption match {
      case Some(term) =>
        Ok(html.genericTypeTerms.edit(id, genericTypes, genericTypeTermForm.fill(term)))
      case _ =>
        NotFound
    })
  }

  def update(id: ExpressionTermID) = Action.async { implicit rs =>
    genericTypeTermForm.bindFromRequest.fold(
      formWithErrors =>
        db.run(GenericType.all.result).map { genericTypes =>
          BadRequest(html.genericTypeTerms.edit(id, genericTypes, formWithErrors))
        },
      term =>
        db.run(GenericTypeTerm.update(term)).map { _ =>
          Redirect(routes.GenericTypeTermsController.list())
            .flashing("success" -> "The expression term was updated successfully.")
        }
    )
  }

  def remove(id: ExpressionTermID) = Action.async { implicit rs =>
    db.run(GenericTypeTerm.one(id).result).map {
      case Some(term) =>
        Ok(html.genericTypeTerms.remove(term))
      case _ =>
        NotFound
    }
  }

  def delete(id: ExpressionTermID) = Action.async { implicit rs =>
    db.run(GenericTypeTerm.delete(id)).map { _ =>
      Redirect(routes.GenericTypeTermsController.list())
        .flashing("success" -> "The expression term was deleted successfully.")
    }
  }
}
