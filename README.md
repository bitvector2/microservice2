# microservice2

Testing Akka, Undertow, Slick on a cluster of
[Raspberry Pi 2 Model B](https://www.raspberrypi.org/products/raspberry-pi-2-model-b/) servers:

* git clone https://github.com/bitvector2/microservice2.git
* cd microservice2
* Edit src/main/resources/application.conf and change service.database.url
* Edit src/main/resources/hazelcast.xml and change <interface></interface> attribute
* ./gradlew clean shadowJar
* ./microservice2.sh

Use the following commands for benchmarking/fault recovery testing:

* ab -n 10000 -c 64 -k https://www.bitvector.org/products/1
* watch --interval 1 curl --silent --show-error --include --max-time 1 https://www.bitvector.org/products

To setup a development environment, download and expand Gradle into your home directory and configure such in
IntelliJ IDEA.  Download and expand Scala into your home directory and configure such in IntelliJ IDEA.

Technology stack in use:

* [Java 8](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
* [Scala 2.11](http://scala-lang.org/)
* [Akka](http://akka.io/)
* [Undertow](http://undertow.io/)
* [Shiro](http://shiro.apache.org/)
* [JJWT](https://github.com/jwtk/jjwt)
* [Slick](http://slick.typesafe.com/)
* [HikariCP](http://brettwooldridge.github.io/HikariCP/)
* [Gradle](http://gradle.org/)
