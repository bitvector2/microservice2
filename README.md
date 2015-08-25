# microservice2

Testing Akka, Undertow, Hibernate, Hazelcast on a cluster of Raspberry Pi 2B servers:

* git clone https://github.com/bitvector2/microservice2.git
* cd microservice2
* Edit src/main/resources/META-INF/persistence.xml
* Edit src/main/resources/application.conf
* ./gradlew clean shadowJar
* ./microservice2.sh

Use the following commands for benchmarking/fault recovery:

* ab -n 10000 -c 64 -k https://www.bitvector.org/products/1
* watch --interval 1 curl --silent --show-error --include --max-time 1 https://www.bitvector.org/products/1

To setup a development environment, download and expand Gradle into your home directory and configure as such in
IntelliJ IDEA

* Download: [Gradle 2.6](https://services.gradle.org/distributions/gradle-2.6-all.zip)
* Run: echo "org.gradle.daemon=true" >> ~/.gradle/gradle.properties
* Run: ./gradlew dependencyUpdates

Technology stack in use:

* Java 8
* Akka
* Undertow
* Hibernate
* HikariCP
* Hazelcast
* Gradle
