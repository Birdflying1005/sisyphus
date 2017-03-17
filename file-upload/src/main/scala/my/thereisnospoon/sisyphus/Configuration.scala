package my.thereisnospoon.sisyphus

import com.typesafe.config.{Config, ConfigFactory}

trait Configuration {
  lazy val config: Config = ConfigFactory.load()
}
