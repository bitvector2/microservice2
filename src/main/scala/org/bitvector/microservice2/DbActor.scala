package org.bitvector.microservice2

import akka.actor.Status.Success
import akka.actor.{Actor, ActorLogging}
import slick.driver.PostgresDriver.api._

import scala.collection.mutable.ArrayBuffer

// import scala.concurrent.ExecutionContext.Implicits.global

object DbActor {
  case class Start()
  case class Stop()
  case class GetAllProducts()

  case class AllProducts(products: ArrayBuffer[Product])

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
  val productsQuery = TableQuery[Products]

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
    val products = new ArrayBuffer[Product]()
    products += new Product(1L, "foo")
    products += new Product(2L, "bar")
    products += new Product(3L, "baz")
    sender() ! AllProducts(products)
  }

  def doGetProduct(id: Long) = {
    val product = new Product(1L, "asdf")
    sender() ! AProduct(product)
  }

  def doAddProduct(product: Product) = {
    log.info("received addproduct")
    sender() ! Success
  }

  def doUpdateProduct(product: Product) = {
    log.info("received updateproduct")
    sender() ! Success
  }

  def doDeleteProduct(product: Product) = {
    log.info("received deleteproduct")
    sender() ! Success
  }
}
