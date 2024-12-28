package com.my.session_link.shared_data

import cats.data.NonEmptyList
import cats.effect.Async
import cats.syntax.applicative._
import cats.syntax.either._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.option._
import com.my.session_link.shared_data.Utils.{getLastCurrencySnapshotOrdinal, getLastMetagraphIncrementalSnapshotInfo}
import com.my.session_link.shared_data.combiners.Combiners.{combineNotarizeSession, combineCreateSession, combineCreateSolSession, combineExtendSession, combineCloseSession}
import com.my.session_link.shared_data.errors.Errors.{CouldNotGetLatestCurrencySnapshot, DataApplicationValidationTypeOps}
import com.my.session_link.shared_data.types.Types._
import com.my.session_link.shared_data.validations.Validations.{NotarizeSessionValidations, NotarizeSessionValidationsWithSignature, CreateSessionValidations, CreateSessionValidationsWithSignature, CreateSolSessionValidations, CreateSolSessionValidationsWithSignature, ExtendSessionValidations, ExtendSessionValidationsWithSignature, CloseSessionValidations, CloseSessionValidationsWithSignature}
import org.slf4j.LoggerFactory
import org.tessellation.currency.dataApplication.dataApplication.DataApplicationValidationErrorOr
import org.tessellation.currency.dataApplication.{DataState, L0NodeContext, L1NodeContext}
import org.tessellation.security.SecurityProvider
import org.tessellation.security.signature.Signed

object LifecycleSharedFunctions {

  private val logger = LoggerFactory.getLogger("Data")

  def validateUpdate[F[_] : Async](update: SessionUpdate)(implicit context: L1NodeContext[F]): F[DataApplicationValidationErrorOr[Unit]] =
    for {
      maybeLastSnapshotOrdinal <- getLastCurrencySnapshotOrdinal(context.asRight[L0NodeContext[F]])
      response <- maybeLastSnapshotOrdinal.fold(CouldNotGetLatestCurrencySnapshot.invalid.pure[F]) { lastSnapshotOrdinal =>
        update match {
          
          case session: NotarizeSession => NotarizeSessionValidations(session, none, lastSnapshotOrdinal.some)

          case session: CreateSession => CreateSessionValidations(session, none, lastSnapshotOrdinal.some)

          case session: CreateSolSession => CreateSolSessionValidations(session, none, lastSnapshotOrdinal.some)

          case session: ExtendSession => ExtendSessionValidations(session, none, lastSnapshotOrdinal.some)

          case session: CloseSession => CloseSessionValidations(session, none, lastSnapshotOrdinal.some)
        }
      }
    } yield response

  def validateData[F[_] : Async](state: DataState[SessionStateOnChain, SessionCalculatedState], updates: NonEmptyList[Signed[SessionUpdate]])(implicit context: L0NodeContext[F]): F[DataApplicationValidationErrorOr[Unit]] = {
    implicit val sp: SecurityProvider[F] = context.securityProvider
    for {
      maybeLastSnapshotOrdinal <- getLastCurrencySnapshotOrdinal(context.asLeft[L1NodeContext[F]])
      maybeLastSnapshotInfo <- getLastMetagraphIncrementalSnapshotInfo(context.asLeft[L1NodeContext[F]])
      response <- (maybeLastSnapshotOrdinal, maybeLastSnapshotInfo) match {
        case (Some(lastSnapshotOrdinal@_), Some(lastSnapshotInfo@_)) =>
          updates.traverse { signedUpdate =>
            signedUpdate.value match {
              
              case session: NotarizeSession =>
                NotarizeSessionValidationsWithSignature(session, signedUpdate.proofs, state)

              case session: CreateSession =>
               CreateSessionValidationsWithSignature(session, signedUpdate.proofs, state)

               case session: CreateSolSession =>
               CreateSolSessionValidationsWithSignature(session, signedUpdate.proofs, state)

              case session: ExtendSession =>
                ExtendSessionValidationsWithSignature(session, signedUpdate.proofs, state)

              case session: CloseSession =>
                CloseSessionValidationsWithSignature(session, signedUpdate.proofs, state)
            }
          }.map(_.reduce)
        case _ => CouldNotGetLatestCurrencySnapshot.invalid.pure[F]
      }
    } yield response
  }

  def combine[F[_] : Async](state: DataState[SessionStateOnChain, SessionCalculatedState], updates: List[Signed[SessionUpdate]])(implicit context: L0NodeContext[F]): F[DataState[SessionStateOnChain, SessionCalculatedState]] = {
    val newStateF = DataState(SessionStateOnChain(List.empty), state.calculated).pure

    if (updates.isEmpty) {
      logger.info("Snapshot without any update, updating the state to empty updates")
      newStateF
    } else {
      getLastCurrencySnapshotOrdinal(Left(context)).flatMap {
        case None =>
          logger.info("Could not get lastMetagraphIncrementalSnapshotInfo, keeping current state")
          state.pure
        case Some(lastSnapshotOrdinal) =>
          newStateF.flatMap(newState => {
            val updatedState = updates.foldLeft(newState) { (acc, signedUpdate) => {
              val update = signedUpdate.value
              update match {
                case session: NotarizeSession =>
                  combineNotarizeSession(session, acc)

                case session: CreateSession =>
                  combineCreateSession(session, acc)

                case session: CreateSolSession =>
                  combineCreateSolSession(session, acc)

                case session: ExtendSession =>
                  combineExtendSession(session, acc)

                case session: CloseSession =>
                  combineCloseSession(session, acc, lastSnapshotOrdinal.value.value)
                }
              }
            }
            updatedState.pure
          })
      }
    }
  }
}