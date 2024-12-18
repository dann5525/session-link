package com.my.session_link.data_l1

import cats.data.NonEmptyList
import cats.effect.{IO, Resource}
import cats.implicits.catsSyntaxOptionId
import cats.syntax.applicative._
import cats.syntax.validated._
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
import org.tessellation.currency.l1.CurrencyL1App
import org.tessellation.ext.cats.effect.ResourceIO
import org.tessellation.schema.SnapshotOrdinal
import org.tessellation.schema.cluster.ClusterId
import org.tessellation.schema.semver.{MetagraphVersion, TessellationVersion}
import org.tessellation.security.hash.Hash
import org.tessellation.security.signature.Signed

import java.util.UUID

object Main
  extends CurrencyL1App(
    "currency-data_l1",
    "currency data L1 node",
    ClusterId(UUID.fromString("517c3a05-9219-471b-a54c-21b7d72f4ae5")),
    metagraphVersion = MetagraphVersion.unsafeFrom(BuildInfo.version),
    tessellationVersion = TessellationVersion.unsafeFrom(BuildInfo.version)
  ) {
  private def makeBaseDataApplicationL1Service(
    calculatedStateService: CalculatedStateService[IO]
  ): BaseDataApplicationL1Service[IO] =
    BaseDataApplicationL1Service(new DataApplicationL1Service[IO, SessionUpdate, SessionStateOnChain, SessionCalculatedState] {
      override def validateUpdate(update: SessionUpdate)(implicit context: L1NodeContext[IO]): IO[DataApplicationValidationErrorOr[Unit]] = LifecycleSharedFunctions.validateUpdate[IO](update)

      override def validateData(state: DataState[SessionStateOnChain, SessionCalculatedState], updates: NonEmptyList[Signed[SessionUpdate]])(implicit context: L1NodeContext[IO]): IO[DataApplicationValidationErrorOr[Unit]] = ().validNec.pure[IO]

      override def combine(state: DataState[SessionStateOnChain, SessionCalculatedState], updates: List[Signed[SessionUpdate]])(implicit context: L1NodeContext[IO]): IO[DataState[SessionStateOnChain, SessionCalculatedState]] = state.pure[IO]

      override def serializeUpdate(update: SessionUpdate): IO[Array[Byte]] = IO(Serializers.serializeUpdate(update))

      override def serializeState(state: SessionStateOnChain): IO[Array[Byte]] = IO(Serializers.serializeState(state))

      override def serializeBlock(block: Signed[DataApplicationBlock]): IO[Array[Byte]] = IO(Serializers.serializeBlock(block)(dataEncoder.asInstanceOf[Encoder[DataUpdate]]))

      override def deserializeUpdate(bytes: Array[Byte]): IO[Either[Throwable, SessionUpdate]] = IO(Deserializers.deserializeUpdate(bytes))

      override def deserializeState(bytes: Array[Byte]): IO[Either[Throwable, SessionStateOnChain]] = IO(Deserializers.deserializeState(bytes))

      override def deserializeBlock(bytes: Array[Byte]): IO[Either[Throwable, Signed[DataApplicationBlock]]] = IO(Deserializers.deserializeBlock(bytes)(dataDecoder.asInstanceOf[Decoder[DataUpdate]]))

      override def routes(implicit context: L1NodeContext[IO]): HttpRoutes[IO] = HttpRoutes.empty

      override def signedDataEntityDecoder: EntityDecoder[IO, Signed[SessionUpdate]] = circeEntityDecoder

      override def dataEncoder: Encoder[SessionUpdate] = implicitly[Encoder[SessionUpdate]]

      override def dataDecoder: Decoder[SessionUpdate] = implicitly[Decoder[SessionUpdate]]

      override def calculatedStateEncoder: Encoder[SessionCalculatedState] = implicitly[Encoder[SessionCalculatedState]]

      override def calculatedStateDecoder: Decoder[SessionCalculatedState] = implicitly[Decoder[SessionCalculatedState]]

      override def getCalculatedState(implicit context: L1NodeContext[IO]): IO[(SnapshotOrdinal, SessionCalculatedState)] = calculatedStateService.getCalculatedState.map(calculatedState => (calculatedState.ordinal, calculatedState.state))

      override def setCalculatedState(ordinal: SnapshotOrdinal, state: SessionCalculatedState)(implicit context: L1NodeContext[IO]): IO[Boolean] = calculatedStateService.setCalculatedState(ordinal, state)

      override def hashCalculatedState(state: SessionCalculatedState)(implicit context: L1NodeContext[IO]): IO[Hash] = calculatedStateService.hashCalculatedState(state)

      override def serializeCalculatedState(state: SessionCalculatedState): IO[Array[Byte]] = IO(Serializers.serializeCalculatedState(state))

      override def deserializeCalculatedState(bytes: Array[Byte]): IO[Either[Throwable, SessionCalculatedState]] = IO(Deserializers.deserializeCalculatedState(bytes))
    })

  private def makeL1Service: IO[BaseDataApplicationL1Service[IO]] = {
    for {
      calculatedStateService <- CalculatedStateService.make[IO]
      dataApplicationL1Service = makeBaseDataApplicationL1Service(calculatedStateService)
    } yield dataApplicationL1Service
  }

  override def dataApplication: Option[Resource[IO, BaseDataApplicationL1Service[IO]]] =
    makeL1Service.asResource.some
}
