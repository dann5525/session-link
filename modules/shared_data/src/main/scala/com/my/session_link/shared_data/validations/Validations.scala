package com.my.session_link.shared_data.validations

import cats.data.NonEmptySet
import cats.effect.Async
import cats.syntax.all._
import com.my.session_link.shared_data.errors.Errors.valid
import com.my.session_link.shared_data.serializers.Serializers
import com.my.session_link.shared_data.types.Types.{NotarizeSession, SessionCalculatedState, SessionStateOnChain, CreateSession, CreateSolSession, ExtendSession, CloseSession}
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

  def CreateSolSessionValidations[F[_] : Async](update: CreateSolSession, maybeState: Option[DataState[SessionStateOnChain, SessionCalculatedState]], lastSnapshotOrdinal: Option[SnapshotOrdinal]): F[DataApplicationValidationErrorOr[Unit]] = Async[F].delay {
    val validatedCreatePollSnapshot = lastSnapshotOrdinal match {
      case Some(value) => validateSnapshotCreateSolSession(value, update)
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

  def CreateSolSessionValidationsWithSignature[F[_] : Async](update: CreateSolSession, proofs: NonEmptySet[SignatureProof], state: DataState[SessionStateOnChain, SessionCalculatedState])(implicit sp: SecurityProvider[F]): F[DataApplicationValidationErrorOr[Unit]] = {
    for {
      addresses <- extractAddresses(proofs)
      validatedAddress = validateProvidedAddress(addresses, update.accessProvider)
      validatedPoll <- CreateSolSessionValidations(update, state.some, None)
      validatedSig = validateCreateSolSessionSignature(update)
    } yield validatedAddress.productR(validatedPoll)productR(validatedSig)
  }

  def ExtendSessionValidations[F[_] : Async](
    update: ExtendSession, 
    maybeState: Option[DataState[SessionStateOnChain, SessionCalculatedState]], 
    lastSnapshotOrdinal: Option[SnapshotOrdinal]
  ): F[DataApplicationValidationErrorOr[Unit]] = Async[F].delay {
    val validatedExtendSnapshot = lastSnapshotOrdinal match {
      case Some(value) => validateSnapshotExtendSession(value, update)
      case None => valid
    }

    maybeState match {
      case Some(state) => 
        val validatedSession = validateExtendSession(state, update)
        validatedExtendSnapshot.productR(validatedSession)
      case None => validatedExtendSnapshot
    }
  }

  def ExtendSessionValidationsWithSignature[F[_] : Async](
    update: ExtendSession, 
    proofs: NonEmptySet[SignatureProof], 
    state: DataState[SessionStateOnChain, SessionCalculatedState]
  )(implicit sp: SecurityProvider[F]): F[DataApplicationValidationErrorOr[Unit]] = {
    for {
      addresses <- extractAddresses(proofs)
      validatedAddress = validateProvidedAddress(addresses, update.accessProvider)
      validatedSession <- ExtendSessionValidations(update, state.some, None)
    } yield validatedAddress.productR(validatedSession)
  }

  def CloseSessionValidations[F[_] : Async](
  update: CloseSession, 
  maybeState: Option[DataState[SessionStateOnChain, SessionCalculatedState]], 
  lastSnapshotOrdinal: Option[SnapshotOrdinal]
): F[DataApplicationValidationErrorOr[Unit]] = Async[F].delay {
  maybeState match {
    case Some(state) => validateCloseSession(state, update)
    case None => valid
  }
}

  def CloseSessionValidationsWithSignature[F[_] : Async](
    update: CloseSession, 
    proofs: NonEmptySet[SignatureProof], 
    state: DataState[SessionStateOnChain, SessionCalculatedState]
  )(implicit sp: SecurityProvider[F]): F[DataApplicationValidationErrorOr[Unit]] = {
    for {
      addresses <- extractAddresses(proofs)
      validatedAddress = validateProvidedAddress(addresses, update.accessProvider)
      validatedSession <- CloseSessionValidations(update, state.some, None)
    } yield validatedAddress.productR(validatedSession)
  }
}

