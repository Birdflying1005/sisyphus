package my.thereisnospoon.sisyphus.uploading

import akka.http.scaladsl.server.{HttpApp, Route}

object UploadServer extends HttpApp with App {

  def route: Route = new UploadRoute("/tmp").route

  startServer("localhost", 9191)
}
