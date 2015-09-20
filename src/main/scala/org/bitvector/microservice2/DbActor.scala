package org.bitvector.microservice2

import akka.actor.{Actor, ActorLogging}
import slick.driver.PostgresDriver.api._
import slick.lifted.TableQuery

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

  implicit val executionContext = context.system.dispatcher
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
    case _ => log.error("DbActor received unknown message")
  }

  def doStart() = {
    log.info("DbActor received start")
  }

  def doStop() = {
    log.info("DbActor received stop")

    database.close()
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
      case result => if (result.isEmpty) caller ! AProduct(null) else caller ! AProduct(result.head)
    }
  }

  def doAddProduct(product: Product) = {
    val caller = sender()
    val future = database.run(productsQuery += product)
    future.onSuccess {
      case result => if (result != 1) caller ! false else caller ! true
    }
  }

  def doUpdateProduct(product: Product) = {
    val caller = sender()
    val future = database.run((for {p <- productsQuery if p.id === product.id} yield p.name).update(product.name))
    future.onSuccess {
      case result => if (result != 1) caller ! false else caller ! true
    }
  }

  def doDeleteProduct(product: Product) = {
    val caller = sender()
    val future = database.run(productsQuery.filter(p => p.id === product.id).delete)
    future.onSuccess {
      case result => if (result != 1) caller ! false else caller ! true
    }
  }
}
