package com.my.session_link.l0

import cats.data.NonEmptyList
import cats.effect.{IO, Resource}
import cats.syntax.applicative._
import cats.syntax.option.catsSyntaxOptionId
import cats.syntax.validated._
import com.my.session_link.l0.custom_routes.CustomRoutes
import com.my.session_link.shared_data.LifecycleSharedFunctions
import com.my.session_link.shared_data.calculated_state.CalculatedStateService
import com.my.session_link.shared_data.deserializers.Deserializers
import com.my.session_link.shared_data.serializers.Serializers
import com.my.session_link.shared_data.types.Types._
import io.circe.{Decoder, Encoder}
import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.{EntityDecoder, HttpRoutes}
import org.tessellation.BuildInfo
import org.tessellation.currency.dataApplication._
import org.tessellation.currency.dataApplication.dataApplication.{DataApplicationBlock, DataApplicationValidationErrorOr}
import org.tessellation.currency.l0.CurrencyL0App
import org.tessellation.ext.cats.effect.ResourceIO
import org.tessellation.schema.SnapshotOrdinal
import org.tessellation.schema.cluster.ClusterId
import org.tessellation.schema.semver.{MetagraphVersion, TessellationVersion}
import org.tessellation.security.hash.Hash
import org.tessellation.security.signature.Signed

import java.util.UUID

object Main extends CurrencyL0App(
  "currency-l0",
  "currency L0 node",
  ClusterId(UUID.fromString("517c3a05-9219-471b-a54c-21b7d72f4ae5")),
  metagraphVersion = MetagraphVersion.unsafeFrom(BuildInfo.version),
  tessellationVersion = TessellationVersion.unsafeFrom(BuildInfo.version)
) {

  private def makeBaseDataApplicationL0Service(
    calculatedStateService: CalculatedStateService[IO]
  ): BaseDataApplicationL0Service[IO] =
    BaseDataApplicationL0Service(new DataApplicationL0Service[IO, SessionUpdate, SessionStateOnChain, SessionCalculatedState] {
      override def genesis: DataState[SessionStateOnChain, SessionCalculatedState] = DataState(SessionStateOnChain(List.empty), SessionCalculatedState(Map.empty))

      override def validateUpdate(update: SessionUpdate)(implicit context: L0NodeContext[IO]): IO[DataApplicationValidationErrorOr[Unit]] = ().validNec.pure[IO]

      override def validateData(state: DataState[SessionStateOnChain, SessionCalculatedState], updates: NonEmptyList[Signed[SessionUpdate]])(implicit context: L0NodeContext[IO]): IO[DataApplicationValidationErrorOr[Unit]] = LifecycleSharedFunctions.validateData[IO](state, updates)

      override def combine(state: DataState[SessionStateOnChain, SessionCalculatedState], updates: List[Signed[SessionUpdate]])(implicit context: L0NodeContext[IO]): IO[DataState[SessionStateOnChain, SessionCalculatedState]] = LifecycleSharedFunctions.combine[IO](state, updates)

      override def serializeState(state: SessionStateOnChain): IO[Array[Byte]] = IO(Serializers.serializeState(state))

      override def serializeUpdate(update: SessionUpdate): IO[Array[Byte]] = IO(Serializers.serializeUpdate(update))

      override def serializeBlock(block: Signed[DataApplicationBlock]): IO[Array[Byte]] = IO(Serializers.serializeBlock(block)(dataEncoder.asInstanceOf[Encoder[DataUpdate]]))

      override def deserializeState(bytes: Array[Byte]): IO[Either[Throwable, SessionStateOnChain]] = IO(Deserializers.deserializeState(bytes))

      override def deserializeUpdate(bytes: Array[Byte]): IO[Either[Throwable, SessionUpdate]] = IO(Deserializers.deserializeUpdate(bytes))

      override def deserializeBlock(bytes: Array[Byte]): IO[Either[Throwable, Signed[DataApplicationBlock]]] = IO(Deserializers.deserializeBlock(bytes)(dataDecoder.asInstanceOf[Decoder[DataUpdate]]))

      override def dataEncoder: Encoder[SessionUpdate] = implicitly[Encoder[SessionUpdate]]

      override def dataDecoder: Decoder[SessionUpdate] = implicitly[Decoder[SessionUpdate]]

      override def routes(implicit context: L0NodeContext[IO]): HttpRoutes[IO] = CustomRoutes[IO](calculatedStateService).public

      override def signedDataEntityDecoder: EntityDecoder[IO, Signed[SessionUpdate]] = circeEntityDecoder

      override def calculatedStateEncoder: Encoder[SessionCalculatedState] = implicitly[Encoder[SessionCalculatedState]]

      override def calculatedStateDecoder: Decoder[SessionCalculatedState] = implicitly[Decoder[SessionCalculatedState]]

      override def getCalculatedState(implicit context: L0NodeContext[IO]): IO[(SnapshotOrdinal, SessionCalculatedState)] = calculatedStateService.getCalculatedState.map(calculatedState => (calculatedState.ordinal, calculatedState.state))

      override def setCalculatedState(ordinal: SnapshotOrdinal, state: SessionCalculatedState)(implicit context: L0NodeContext[IO]): IO[Boolean] = calculatedStateService.setCalculatedState(ordinal, state)

      override def hashCalculatedState(state: SessionCalculatedState)(implicit context: L0NodeContext[IO]): IO[Hash] = calculatedStateService.hashCalculatedState(state)

      override def serializeCalculatedState(state: SessionCalculatedState): IO[Array[Byte]] = IO(Serializers.serializeCalculatedState(state))

      override def deserializeCalculatedState(bytes: Array[Byte]): IO[Either[Throwable, SessionCalculatedState]] = IO(Deserializers.deserializeCalculatedState(bytes))
    })

  private def makeL0Service: IO[BaseDataApplicationL0Service[IO]] = {
    for {
      calculatedStateService <- CalculatedStateService.make[IO]
      dataApplicationL0Service = makeBaseDataApplicationL0Service(calculatedStateService)
    } yield dataApplicationL0Service
  }

  override def dataApplication: Option[Resource[IO, BaseDataApplicationL0Service[IO]]] =
    makeL0Service.asResource.some
}
