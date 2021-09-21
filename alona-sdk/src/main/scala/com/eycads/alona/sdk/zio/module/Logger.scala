package com.eycads.alona.sdk.zio.module

import com.eycads.alona.sdk.zio.types.ErrorTypes.ZioError
import org.slf4j.Marker
import zio.{Has, ZIO}

object Logger {

  type Logger = Has[Logger.Service]

  trait Service {
    def error(message: String): ZIO[Logger, ZioError, Unit]
    def error(message:  String, cause: Throwable): ZIO[Logger, ZioError, Unit]
    def error(message: String, args: Any*): ZIO[Logger, ZioError, Unit]
    def error(marker: Marker, message: String): ZIO[Logger, ZioError, Unit]
    def error(marker: Marker, message: String, cause: Throwable): ZIO[Logger, ZioError, Unit]
    def error(marker: Marker, message: String, args: Any*): ZIO[Logger, ZioError, Unit]
    def warn(message: String): ZIO[Logger, ZioError, Unit]
    def warn(message:  String, cause: Throwable): ZIO[Logger, ZioError, Unit]
    def warn(message: String, args: Any*): ZIO[Logger, ZioError, Unit]
    def warn(marker: Marker, message: String): ZIO[Logger, ZioError, Unit]
    def warn(marker: Marker, message: String, cause: Throwable): ZIO[Logger, ZioError, Unit]
    def warn(marker: Marker, message: String, args: Any*): ZIO[Logger, ZioError, Unit]
    def info(message: String): ZIO[Logger, ZioError, Unit]
    def info(message:  String, cause: Throwable): ZIO[Logger, ZioError, Unit]
    def info(message: String, args: Any*): ZIO[Logger, ZioError, Unit]
    def info(marker: Marker, message: String): ZIO[Logger, ZioError, Unit]
    def info(marker: Marker, message: String, cause: Throwable): ZIO[Logger, ZioError, Unit]
    def info(marker: Marker, message: String, args: Any*): ZIO[Logger, ZioError, Unit]
    def debug(message: String): ZIO[Logger, ZioError, Unit]
    def debug(message:  String, cause: Throwable): ZIO[Logger, ZioError, Unit]
    def debug(message: String, args: Any*): ZIO[Logger, ZioError, Unit]
    def debug(marker: Marker, message: String): ZIO[Logger, ZioError, Unit]
    def debug(marker: Marker, message: String, cause: Throwable): ZIO[Logger, ZioError, Unit]
    def debug(marker: Marker, message: String, args: Any*): ZIO[Logger, ZioError, Unit]
    def trace(message: String): ZIO[Logger, ZioError, Unit]
    def trace(message:  String, cause: Throwable): ZIO[Logger, ZioError, Unit]
    def trace(message: String, args: Any*): ZIO[Logger, ZioError, Unit]
    def trace(marker: Marker, message: String): ZIO[Logger, ZioError, Unit]
    def trace(marker: Marker, message: String, cause: Throwable): ZIO[Logger, ZioError, Unit]
    def trace(marker: Marker, message: String, args: Any*): ZIO[Logger, ZioError, Unit]
  }

  def error(message: String): ZIO[Logger, ZioError, Unit] =
    ZIO.accessM(_.get.error(message))
  def error(message:  String, cause: Throwable): ZIO[Logger, ZioError, Unit] =
    ZIO.accessM(_.get.error(message, cause))
  def error(message: String, args: Any*): ZIO[Logger, ZioError, Unit] =
    ZIO.accessM(_.get.error(message, args))
  def error(marker: Marker, message: String): ZIO[Logger, ZioError, Unit] =
    ZIO.accessM(_.get.error(marker, message))
  def error(marker: Marker, message: String, cause: Throwable): ZIO[Logger, ZioError, Unit] =
    ZIO.accessM(_.get.error(marker, message, cause))
  def error(marker: Marker, message: String, args: Any*): ZIO[Logger, ZioError, Unit] =
    ZIO.accessM(_.get.error(marker, message, args))

  def warn(message: String): ZIO[Logger, ZioError, Unit] =
    ZIO.accessM(_.get.warn(message))
  def warn(message:  String, cause: Throwable): ZIO[Logger, ZioError, Unit] =
    ZIO.accessM(_.get.warn(message, cause))
  def warn(message: String, args: Any*): ZIO[Logger, ZioError, Unit] =
    ZIO.accessM(_.get.warn(message, args))
  def warn(marker: Marker, message: String): ZIO[Logger, ZioError, Unit] =
    ZIO.accessM(_.get.warn(marker, message))
  def warn(marker: Marker, message: String, cause: Throwable): ZIO[Logger, ZioError, Unit] =
    ZIO.accessM(_.get.warn(marker, message, cause))
  def warn(marker: Marker, message: String, args: Any*): ZIO[Logger, ZioError, Unit] =
    ZIO.accessM(_.get.warn(marker, message, args))

  def info(message: String): ZIO[Logger, ZioError, Unit] =
    ZIO.accessM(_.get.info(message))
  def info(message:  String, cause: Throwable): ZIO[Logger, ZioError, Unit] =
    ZIO.accessM(_.get.info(message, cause))
  def info(message: String, args: Any*): ZIO[Logger, ZioError, Unit] =
    ZIO.accessM(_.get.info(message, args))
  def info(marker: Marker, message: String): ZIO[Logger, ZioError, Unit] =
    ZIO.accessM(_.get.info(marker, message))
  def info(marker: Marker, message: String, cause: Throwable): ZIO[Logger, ZioError, Unit] =
    ZIO.accessM(_.get.info(marker, message, cause))
  def info(marker: Marker, message: String, args: Any*): ZIO[Logger, ZioError, Unit] =
    ZIO.accessM(_.get.info(marker, message, args))

  def debug(message: String): ZIO[Logger, ZioError, Unit] =
    ZIO.accessM(_.get.debug(message))
  def debug(message:  String, cause: Throwable): ZIO[Logger, ZioError, Unit] =
    ZIO.accessM(_.get.debug(message, cause))
  def debug(message: String, args: Any*): ZIO[Logger, ZioError, Unit] =
    ZIO.accessM(_.get.debug(message, args))
  def debug(marker: Marker, message: String): ZIO[Logger, ZioError, Unit] =
    ZIO.accessM(_.get.debug(marker, message))
  def debug(marker: Marker, message: String, cause: Throwable): ZIO[Logger, ZioError, Unit] =
    ZIO.accessM(_.get.debug(marker, message, cause))
  def debug(marker: Marker, message: String, args: Any*): ZIO[Logger, ZioError, Unit] =
    ZIO.accessM(_.get.debug(marker, message, args))

  def trace(message: String): ZIO[Logger, ZioError, Unit] =
    ZIO.accessM(_.get.trace(message))
  def trace(message:  String, cause: Throwable): ZIO[Logger, ZioError, Unit] =
    ZIO.accessM(_.get.trace(message, cause))
  def trace(message: String, args: Any*): ZIO[Logger, ZioError, Unit] =
    ZIO.accessM(_.get.trace(message, args))
  def trace(marker: Marker, message: String): ZIO[Logger, ZioError, Unit] =
    ZIO.accessM(_.get.trace(marker, message))
  def trace(marker: Marker, message: String, cause: Throwable): ZIO[Logger, ZioError, Unit] =
    ZIO.accessM(_.get.trace(marker, message, cause))
  def trace(marker: Marker, message: String, args: Any*): ZIO[Logger, ZioError, Unit] =
    ZIO.accessM(_.get.trace(marker, message, args))
}
