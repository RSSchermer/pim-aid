package modelTests

import org.scalatest.{Suite, BeforeAndAfterAll}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}
import scala.language.postfixOps
import scala.util.{Failure, Success}

import entitytled.Entitytled

import slick.driver.H2Driver

import model._

trait ModelSpec extends BeforeAndAfterAll
  with Entitytled
  with ConditionExpressionComponent
  with DrugComponent
  with DrugGroupComponent
  with ExpressionTermComponent
  with GenericTypeComponent
  with MedicationProductComponent
  with RuleComponent
  with SuggestionTemplateComponent
  with UserSessionComponent
{
  self: Suite =>

  val driver: H2Driver = H2Driver

  import driver.api._
  
  def setupDatabase(): Database = {
    val dbUrl = s"jdbc:h2:mem:${this.getClass.getSimpleName};DB_CLOSE_DELAY=-1"
    val db = Database.forURL(dbUrl, driver = "org.h2.Driver")
    db.createSession().force()

    val result = db.run((
        TableQuery[Drugs].schema ++
        TableQuery[DrugGroups].schema ++
        TableQuery[DrugGroupsGenericTypes].schema ++
        TableQuery[ExpressionTerms].schema ++
        TableQuery[ExpressionTermsRules].schema ++
        TableQuery[ExpressionTermsStatementTerms].schema ++
        TableQuery[GenericTypes].schema ++
        TableQuery[GenericTypesMedicationProducts].schema ++
        TableQuery[MedicationProducts].schema ++
        TableQuery[Rules].schema ++
        TableQuery[RulesSuggestionTemplates].schema ++
        TableQuery[StatementTermsUserSessions].schema ++
        TableQuery[SuggestionTemplates].schema ++
        TableQuery[UserSessions].schema
      ).create)

    Await.result(result, 15 seconds)

    db
  }

  implicit lazy val db: Database = setupDatabase()

  override protected def afterAll(): Unit =
    db.close()

  def query[T](dbAction: DBIO[T])(implicit ex: ExecutionContext, timeout: Duration = 15 seconds): Any =
    runAction(dbAction)

  def commit[T](dbAction: DBIO[T])(implicit ex: ExecutionContext, timeout: Duration = 15 seconds): Any =
    runAction(dbAction.transactionally)

  def rollback[T](dbAction: DBIO[T])(implicit ex: ExecutionContext, timeout: Duration = 15 seconds): Any = {

    case class RollbackException(expected: T) extends RuntimeException("rollback exception")

    val rollbackAction = dbAction.flatMap { result =>
      // NOTE:
      // DBIO.failed returns DBIOAction[Nothing, NoStream, Effect], but we need to preserve T
      // otherwise, we'll end up with a 'R' returned by 'transactionally' method
      // this seems to be need when compiling for 2.10.x (probably a bug fixed on 2.11.x series)
      DBIO.failed(RollbackException(result)).map(_ => result) // map back to T
    }.transactionally.asTry

    val finalAction =
      rollbackAction.map {
        case Success(result)                    => result
        case Failure(RollbackException(result)) => result
        case Failure(other)                     => throw other
      }

    runAction(finalAction)
  }

  private def runAction[T](dbAction: DBIO[T])(implicit ex: ExecutionContext, timeout: Duration): Any =
    Await.result(db.run(dbAction), timeout)
}
