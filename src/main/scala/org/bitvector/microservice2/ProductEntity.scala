package org.bitvector.microservice2

import slick.driver.PostgresDriver.api._
import slick.lifted.ProvenShape

class ProductEntity(tag: Tag) extends Table[(Int, String)](tag, "products") {
  // Every table needs a * projection with the same type as the table's type parameter
  def * : ProvenShape[(Int, String)] = (id, name)

  def id = column[Int]("id", O.PrimaryKey, O.AutoInc) // This is the primary key column

  def name = column[String]("name")

}
