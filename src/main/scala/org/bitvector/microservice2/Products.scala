package org.bitvector.microservice2

import slick.driver.PostgresDriver.api._

case class Product(id: Long, name: String)

class Products(tag: Tag) extends Table[Product](tag, "products") {
  def * = (id, name) <>(Product.tupled, Product.unapply)

  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

  def name = column[String]("name")
}
