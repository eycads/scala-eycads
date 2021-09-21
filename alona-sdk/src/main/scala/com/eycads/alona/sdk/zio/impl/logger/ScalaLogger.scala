package com.eycads.alona.sdk.zio.impl.logger

import com.eycads.alona.sdk.zio.module.Logger
import com.eycads.alona.sdk.zio.module.Logger.Logger
import com.eycads.alona.sdk.zio.types.ErrorTypes.ZioError
import org.slf4j.Marker
import zio.{Layer, UIO, ZIO, ZLayer}

class ScalaLogger(logger: com.typesafe.scalalogging.Logger) extends Logger.Service {

  override def error(message: String): ZIO[Logger, ZioError, Unit] =
    UIO(logger.error(message))
  override def error(message:  String, cause: Throwable): ZIO[Logger, ZioError, Unit] =
    UIO(logger.error(message, cause))
  override def error(message: String, args: Any*): ZIO[Logger, ZioError, Unit] =
    UIO(logger.error(message, args))
  override def error(marker: Marker, message: String): ZIO[Logger, ZioError, Unit] =
    UIO(logger.error(marker, message))
  override def error(marker: Marker, message: String, cause: Throwable): ZIO[Logger, ZioError, Unit] =
    UIO(logger.error(marker, message, cause))
  override def error(marker: Marker, message: String, args: Any*): ZIO[Logger, ZioError, Unit] =
    UIO(logger.error(marker, message, args))

  override def warn(message: String): ZIO[Logger, ZioError, Unit] =
    UIO(logger.warn(message))
  override def warn(message:  String, cause: Throwable): ZIO[Logger, ZioError, Unit] =
    UIO(logger.warn(message, cause))
  override def warn(message: String, args: Any*): ZIO[Logger, ZioError, Unit] =
    UIO(logger.warn(message, args))
  override def warn(marker: Marker, message: String): ZIO[Logger, ZioError, Unit] =
    UIO(logger.warn(marker, message))
  override def warn(marker: Marker, message: String, cause: Throwable): ZIO[Logger, ZioError, Unit] =
    UIO(logger.warn(marker, message, cause))
  override def warn(marker: Marker, message: String, args: Any*): ZIO[Logger, ZioError, Unit] =
    UIO(logger.warn(marker, message, args))

  override def info(message: String): ZIO[Logger, ZioError, Unit] =
    UIO(logger.info(message))
  override def info(message:  String, cause: Throwable): ZIO[Logger, ZioError, Unit] =
    UIO(logger.info(message, cause))
  override def info(message: String, args: Any*): ZIO[Logger, ZioError, Unit] =
    UIO(logger.info(message, args))
  override def info(marker: Marker, message: String): ZIO[Logger, ZioError, Unit] =
    UIO(logger.info(marker, message))
  override def info(marker: Marker, message: String, cause: Throwable): ZIO[Logger, ZioError, Unit] =
    UIO(logger.info(marker, message, cause))
  override def info(marker: Marker, message: String, args: Any*): ZIO[Logger, ZioError, Unit] =
    UIO(logger.info(marker, message, args))

  override def debug(message: String): ZIO[Logger, ZioError, Unit] =
    UIO(logger.debug(message))
  override def debug(message:  String, cause: Throwable): ZIO[Logger, ZioError, Unit] =
    UIO(logger.debug(message, cause))
  override def debug(message: String, args: Any*): ZIO[Logger, ZioError, Unit] =
    UIO(logger.debug(message, args))
  override def debug(marker: Marker, message: String): ZIO[Logger, ZioError, Unit] =
    UIO(logger.debug(marker, message))
  override def debug(marker: Marker, message: String, cause: Throwable): ZIO[Logger, ZioError, Unit] =
    UIO(logger.debug(marker, message, cause))
  override def debug(marker: Marker, message: String, args: Any*): ZIO[Logger, ZioError, Unit] =
    UIO(logger.debug(marker, message, args))

  override def trace(message: String): ZIO[Logger, ZioError, Unit] =
    UIO(logger.trace(message))
  override def trace(message:  String, cause: Throwable): ZIO[Logger, ZioError, Unit] =
    UIO(logger.trace(message, cause))
  override def trace(message: String, args: Any*): ZIO[Logger, ZioError, Unit] =
    UIO(logger.trace(message, args))
  override def trace(marker: Marker, message: String): ZIO[Logger, ZioError, Unit] =
    UIO(logger.trace(marker, message))
  override def trace(marker: Marker, message: String, cause: Throwable): ZIO[Logger, ZioError, Unit] =
    UIO(logger.trace(marker, message, cause))
  override def trace(marker: Marker, message: String, args: Any*): ZIO[Logger, ZioError, Unit] =
    UIO(logger.trace(marker, message, args))

}

object ScalaLogger {

  private def live(logger: com.typesafe.scalalogging.Logger): Layer[Throwable, Logger] =
    ZLayer.succeed[Logger.Service](new ScalaLogger(logger))

  def createLive(logger: com.typesafe.scalalogging.Logger): Layer[Throwable, Logger] =
    live(logger)

}
