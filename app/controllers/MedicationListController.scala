package controllers

import play.api.mvc._

import views._

object MedicationListController extends Controller {
  def index = Action {
    Ok(html.medicationList.index())
  }
}
