package org.bitvector.microservice2

import java.util

import akka.actor.Status.Success
import akka.actor.{Actor, ActorLogging}
import slick.driver.PostgresDriver.api._

// import scala.concurrent.ExecutionContext.Implicits.global

object DbActor {

  case class Start()

  case class Stop()

  case class GetAllProducts()

  case class AllProducts(products: java.util.ArrayList[ProductEntity])

  case class GetProduct(id: Int)

  case class Product(product: ProductEntity)

  case class AddProduct(product: ProductEntity)

  case class UpdateProduct(product: ProductEntity)

  case class DeleteProduct(product: ProductEntity)

}

class DbActor extends Actor with ActorLogging {

  import DbActor._

  val settings = Settings(context.system)
  val database = Database.forConfig("microservice2.database")
  val productEntity = TableQuery[ProductEntity]

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
    log.info("received getallproducts")
    val products = new util.ArrayList[ProductEntity]()
    sender() ! AllProducts(products)
  }

  def doGetProduct(id: Int) = {
    log.info("received getproduct")
    val product = new ProductEntity(tag = null)
    sender() ! Product(product)
  }

  def doAddProduct(product: ProductEntity) = {
    log.info("received addproduct")
    sender() ! Success
  }

  def doUpdateProduct(product: ProductEntity) = {
    log.info("received updateproduct")
    sender() ! Success
  }

  def doDeleteProduct(product: ProductEntity) = {
    log.info("received deleteproduct")
    sender() ! Success
  }

}
