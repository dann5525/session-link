package com.my.session_link.l0.custom_routes

import cats.effect.Async
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import cats.syntax.functor._
import com.my.session_link.shared_data.calculated_state.CalculatedStateService
import com.my.session_link.shared_data.types.Types._
import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive
import eu.timepit.refined.auto._
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.middleware.CORS
import org.http4s.{HttpRoutes, Response}
import org.tessellation.routes.internal.{InternalUrlPrefix, PublicRoutes}
import org.tessellation.schema.address.Address
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import java.time.Instant

case class CustomRoutes[F[_] : Async](calculatedStateService: CalculatedStateService[F]) extends Http4sDsl[F] with PublicRoutes[F] {
  implicit val logger: SelfAwareStructuredLogger[F] = Slf4jLogger.getLogger[F]

  @derive(decoder, encoder)
  case class SessionResponse(
    id: String,
    accessId: String, 
    accessProvider: Address, 
    accessObj: String,
    endSnapshotOrdinal: Long,
    status: String,
    metadata: Option[SessionMetadataResponse]
  )

  @derive(decoder, encoder)
  case class SessionMetadataResponse(
    startTime: Instant,
    data: Map[String, MetadataValue]
  )

  private def formatSession(session: Session, lastOrdinal: Long): SessionResponse = {
    val status = if (session.endSnapshotOrdinal < lastOrdinal) "Expired" else "Active"
    SessionResponse(
      id = session.id,
      accessProvider = session.accessProvider,
      accessId = session.accessId,
      accessObj = session.accessObj,
      endSnapshotOrdinal = session.endSnapshotOrdinal,
      status = status,
      metadata = session.metadata.map(m => SessionMetadataResponse(
        startTime = m.startTime,
        data = m.data
      ))
    )
  }

  private def getAllSessions: F[Response[F]] = {
    calculatedStateService.getCalculatedState
      .map(v => (v.ordinal, v.state))
      .map { case (ord, state) => 
        state.sessions.view.mapValues(formatSession(_, ord.value.value)).toList 
      }
      .flatMap(Ok(_))
      .handleErrorWith { e =>
        val message = s"An error occurred when getAllSessions: ${e.getMessage}"
        logger.error(message) >> new Exception(message).raiseError[F, Response[F]]
      }
  }

  private def getFilteredSessionById(sessionId: String): F[Response[F]] = {
    calculatedStateService.getCalculatedState
      .map(v => (v.ordinal, v.state))
      .map { case (ord, state) => 
        state.sessions.get(sessionId).map(formatSession(_, ord.value.value)) 
      }
      .flatMap(_.fold(NotFound())(Ok(_)))
      .handleErrorWith { e =>
        val message = s"An error occurred when getFilteredSessionById: ${e.getMessage}"
        logger.error(message) >> new Exception(message).raiseError[F, Response[F]]
      }
  }

  private val routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "sessions" => getAllSessions
    case GET -> Root / "sessions" / sessionId => getFilteredSessionById(sessionId)
  }

  val public: HttpRoutes[F] =
    CORS
      .policy
      .withAllowCredentials(false)
      .httpRoutes(routes)

  override protected def prefixPath: InternalUrlPrefix = "/"
}