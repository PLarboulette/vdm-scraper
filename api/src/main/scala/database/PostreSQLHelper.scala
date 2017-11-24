package database

import com.github.mauricio.async.db.{Connection, QueryResult, RowData}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

object PostreSQLHelper {

  def test () (implicit connection : Connection , ec : ExecutionContext ): Unit = {

    val future: Future[QueryResult] = connection.sendQuery("SELECT 0")
    val mapResult: Future[Any] =
      future.map(
        queryResult => queryResult.rows match {
          case Some(resultSet) => {
            val row : RowData = resultSet.head
            row(0)
          }
          case None => -1
        }
      )

    val result = Await.result( mapResult, 5 seconds)
    println(result)
  }

}
