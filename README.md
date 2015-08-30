# microservice2

Testing Akka, Undertow, Hibernate, HikariCP, and Hazelcast on a cluster of
[Raspberry Pi 2 Model B](https://www.raspberrypi.org/products/raspberry-pi-2-model-b/) servers:

* git clone https://github.com/bitvector2/microservice2.git
* cd microservice2
* Edit src/main/resources/META-INF/persistence.xml
* Edit src/main/resources/application.conf
* ./gradlew clean shadowJar
* ./microservice2.sh

Use the following commands for benchmarking/fault recovery testing:

* ab -n 10000 -c 64 -k https://www.bitvector.org/products/99
* watch --interval 1 curl --silent --show-error --include --max-time 1 https://www.bitvector.org/products

To setup a development environment, download and expand Gradle into your home directory and configure such in
IntelliJ IDEA.  Then enable the gradle daemon and check dependencies:

* Run: echo "org.gradle.daemon=true" >> ~/.gradle/gradle.properties
* Run: ./gradlew dependencyUpdates

Technology stack in use:

* [Java 8](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
* [Akka](http://akka.io/)
* [Undertow](http://undertow.io/)
* [Hibernate](http://hibernate.org/)
* [HikariCP](http://brettwooldridge.github.io/HikariCP/)
* [Hazelcast](http://hazelcast.org/)
* [Gradle](http://gradle.org/)
