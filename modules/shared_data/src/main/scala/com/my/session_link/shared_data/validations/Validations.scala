package com.my.session_link.shared_data.validations

import cats.data.NonEmptySet
import cats.effect.Async
import cats.syntax.all._
import com.my.session_link.shared_data.errors.Errors.valid
import com.my.session_link.shared_data.serializers.Serializers
import com.my.session_link.shared_data.types.Types.{NotarizeSession, SessionCalculatedState, SessionStateOnChain, CreateSession}
import com.my.session_link.shared_data.validations.TypeValidators._
import org.tessellation.currency.dataApplication.DataState
import org.tessellation.currency.dataApplication.dataApplication.DataApplicationValidationErrorOr

import org.tessellation.schema.SnapshotOrdinal
import org.tessellation.schema.address.Address
import org.tessellation.security.SecurityProvider
import org.tessellation.security.hash.Hash
import org.tessellation.security.signature.signature.SignatureProof

object Validations {



  def CreateSessionValidations[F[_] : Async](update: CreateSession, maybeState: Option[DataState[SessionStateOnChain, SessionCalculatedState]], lastSnapshotOrdinal: Option[SnapshotOrdinal]): F[DataApplicationValidationErrorOr[Unit]] = Async[F].delay {
    val validatedCreatePollSnapshot = lastSnapshotOrdinal match {
      case Some(value) => validateSnapshotCreateSession(value, update)
      case None => valid
    }

    maybeState match {
      case Some(state) =>
        val sessionId = Hash.fromBytes(Serializers.serializeUpdate(update))
        val validatedPoll = validateIfSessionAlreadyExists(state, sessionId.toString)
        validatedCreatePollSnapshot.productR(validatedPoll)
      case None => validatedCreatePollSnapshot
    }
  }

  def NotarizeSessionValidations[F[_] : Async](update: NotarizeSession, maybeState: Option[DataState[SessionStateOnChain, SessionCalculatedState]], lastSnapshotOrdinal: Option[SnapshotOrdinal]): F[DataApplicationValidationErrorOr[Unit]] = Async[F].delay {
    val validatedCreatePollSnapshot = lastSnapshotOrdinal match {
      case Some(value) => validateSnapshotNotarizeSession(value, update)
      case None => valid
    }

    maybeState match {
      case Some(state) =>
        val sessionId = Hash.fromBytes(Serializers.serializeUpdate(update))
        val validatedPoll = validateIfSessionAlreadyExists(state, sessionId.toString)
        validatedCreatePollSnapshot.productR(validatedPoll)
      case None => validatedCreatePollSnapshot
    }
  }


  private def extractAddresses[F[_] : Async : SecurityProvider](proofs: NonEmptySet[SignatureProof]): F[List[Address]] = {
    proofs
      .map(_.id)
      .toList
      .traverse(_.toAddress[F])
  }

  def NotarizeSessionValidationsWithSignature[F[_] : Async](update: NotarizeSession, proofs: NonEmptySet[SignatureProof], state: DataState[SessionStateOnChain, SessionCalculatedState])(implicit sp: SecurityProvider[F]): F[DataApplicationValidationErrorOr[Unit]] = {
    for {
      addresses <- extractAddresses(proofs)
      validatedAddress = validateProvidedAddress(addresses, update.accessProvider)
      validatedPoll <- NotarizeSessionValidations(update, state.some, None)
    } yield validatedAddress.productR(validatedPoll)
  }

  def CreateSessionValidationsWithSignature[F[_] : Async](update: CreateSession, proofs: NonEmptySet[SignatureProof], state: DataState[SessionStateOnChain, SessionCalculatedState])(implicit sp: SecurityProvider[F]): F[DataApplicationValidationErrorOr[Unit]] = {
    for {
      addresses <- extractAddresses(proofs)
      validatedAddress = validateProvidedAddress(addresses, update.accessProvider)
      validatedPoll <- CreateSessionValidations(update, state.some, None)
      validatedSig = validateCreateSessionSignature(update)
    } yield validatedAddress.productR(validatedPoll)productR(validatedSig)
  }
}

