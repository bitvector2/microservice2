package org.bitvector.microservice2

import akka.actor.Status.Success
import akka.actor.{Actor, ActorLogging}
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global

object DbActor {
  case class Start()
  case class Stop()
  case class GetAllProducts()

  case class AllProducts(products: Seq[Product])

  case class GetProduct(id: Long)

  case class AProduct(product: Product)

  case class AddProduct(product: Product)

  case class UpdateProduct(product: Product)

  case class DeleteProduct(product: Product)
}

class DbActor extends Actor with ActorLogging {
  import DbActor._

  val settings = Settings(context.system)
  val database = Database.forConfig("service.database")
  val productsQuery = TableQuery[ProductDAO]

  def receive = {
    case Start() => this.doStart()
    case Stop() => this.doStop()
    case GetAllProducts() => this.doGetAllProducts()
    case GetProduct(id) => this.doGetProduct(id)
    case AddProduct(product) => this.doAddProduct(product)
    case UpdateProduct(product) => this.doUpdateProduct(product)
    case DeleteProduct(product) => this.doDeleteProduct(product)
    case _ => log.info("received unknown message")
  }

  def doStart() = {
    log.info("received start")
  }

  def doStop() = {
    database.close()
    log.info("received stop")
  }

  def doGetAllProducts() = {
    val caller = sender()
    val future = database.run(productsQuery.result)
    future.onSuccess {
      case result => caller ! AllProducts(result)
    }
  }

  def doGetProduct(id: Long) = {
    val caller = sender()
    val future = database.run(productsQuery.filter(p => p.id === id).result)
    future.onSuccess {
      case result => caller ! AProduct(result.head)
    }
  }

  def doAddProduct(product: Product) = {
    val caller = sender()
    log.info("received addproduct")
    caller ! Success
  }

  def doUpdateProduct(product: Product) = {
    val caller = sender()
    log.info("received updateproduct")
    caller ! Success
  }

  def doDeleteProduct(product: Product) = {
    val caller = sender()
    log.info("received deleteproduct")
    caller ! Success
  }
}
