package utils

import models._

class MedicationProductTemplateRenderer(groupsProducts: List[(DrugGroup, MedicationProduct)],
                                        typesProducts: List[(GenericType, MedicationProduct)]) {
  def render(template: String): List[String] = {
    replacePlaceholder(template)
  }

  private def replacePlaceholder(template: String): List[String] = {
    """\{\{(type|group)\(([^\)]+)\)\}\}""".r.findFirstMatchIn(template) match {
      case Some(m) => m.group(1).toLowerCase match {
        case "type" =>
          typesProducts
            .filter(x => x._1.name.toLowerCase == m.group(2).toLowerCase)
            .map { x => template.replaceAll(s"""\\{\\{type\\(${m.group(2)}\\)\\}\\}""", x._2.name) }
            .flatMap(replacePlaceholder)
        case "group" =>
          groupsProducts
            .filter(x => x._1.name.toLowerCase == m.group(2).toLowerCase)
            .map { x => template.replaceAll(s"""\\{\\{group\\(${m.group(2)}\\)\\}\\}""", x._2.name) }
            .flatMap(replacePlaceholder)
      }
      case _ => List(template)
    }
  }
}
