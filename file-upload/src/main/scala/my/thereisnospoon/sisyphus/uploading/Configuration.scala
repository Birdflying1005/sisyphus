package my.thereisnospoon.sisyphus.uploading

import com.typesafe.config.{Config, ConfigFactory}

trait Configuration {
  lazy val config: Config = ConfigFactory.load()
}
