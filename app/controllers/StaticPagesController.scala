package controllers

import play.api.mvc._

import views._

object StaticPagesController extends Controller {
  def introduction = Action {
    Ok(html.staticPages.introduction())
  }
}

