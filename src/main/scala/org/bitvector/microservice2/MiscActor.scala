package org.bitvector.microservice2

import java.util

import akka.actor.Status.Success
import akka.actor.{Actor, ActorLogging}

object MiscActor {

  case class Start()

  case class Stop()

  case class GetAllProducts()

  case class AllProducts(products: java.util.ArrayList[Integer])

  case class GetProduct(product: Integer)

  case class Product(id: Integer)

  case class AddProduct(product: Integer)

  case class UpdateProduct(product: Integer)

  case class DeleteProduct(product: Integer)

}

class MiscActor extends Actor with ActorLogging {

  import MiscActor._

  def receive = {
    case Start() => this.start()
    case Stop() => this.stop()
    case GetAllProducts() => log.info("received getallproducts"); val list = new util.ArrayList[Integer](); sender() ! AllProducts(list)
    case GetProduct(_) => log.info("received getproduct"); sender() ! Product(1)
    case AddProduct(_) => log.info("received addproduct"); sender() ! Success
    case UpdateProduct(_) => log.info("received updateproduct"); sender() ! Success
    case DeleteProduct(_) => log.info("received deleteproduct"); sender() ! Success
    case _ => log.info("received unknown message")
  }

  def start() = {
    log.info("received start")
  }

  def stop() = {
    log.info("received stop")
  }

}
