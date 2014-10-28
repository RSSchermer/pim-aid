package controllers

import play.api.mvc._

import views._

object StaticPagesController extends Controller {
  def introduction = Action {
    Ok(html.staticPages.introduction())
  }

  def privacy = Action {
    Ok(html.staticPages.privacy())
  }

  def about = Action {
    Ok(html.staticPages.about())
  }

  def contact = Action {
    Ok(html.staticPages.contact())
  }
}

