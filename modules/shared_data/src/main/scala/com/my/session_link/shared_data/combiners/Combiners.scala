package com.my.session_link.shared_data.combiners

import com.my.session_link.shared_data.serializers.Serializers
import com.my.session_link.shared_data.types.Types._
import monocle.Monocle.toAppliedFocusOps
import org.tessellation.currency.dataApplication.DataState
import org.tessellation.security.hash.Hash
import org.slf4j.LoggerFactory

object Combiners {

  private val logger = LoggerFactory.getLogger("Combiners")

  def combineNotarizeSession(createPoll: NotarizeSession, state: DataState[SessionStateOnChain, SessionCalculatedState]): DataState[SessionStateOnChain, SessionCalculatedState] = {
    logger.info(s"combineNotarizeSession called with createPoll: $createPoll and state: $state")
    val sessionId = Hash.fromBytes(Serializers.serializeUpdate(createPoll)).toString
    logger.info(s"Generated sessionId: $sessionId")
    val newSession = Session(sessionId, createPoll.accessProvider, createPoll.accessId, createPoll.accessObj, createPoll.endSnapshotOrdinal, createPoll.metadata)

    val newOnChain = SessionStateOnChain(state.onChain.updates :+ createPoll)
    val newCalculatedState = state.calculated.focus(_.sessions).modify(_.updated(sessionId, newSession))

    val result = DataState(newOnChain, newCalculatedState)
    logger.info(s"Returning new state after combineNotarizeSession: $result")
    result
  }

  def combineCreateSession(createPoll: CreateSession, state: DataState[SessionStateOnChain, SessionCalculatedState]): DataState[SessionStateOnChain, SessionCalculatedState] = {
    logger.info(s"combineNotarizeSession called with createPoll: $createPoll and state: $state")
    val sessionId = Hash.fromBytes(Serializers.serializeUpdate(createPoll)).toString
    logger.info(s"Generated sessionId: $sessionId")
    val newSession = Session(sessionId, createPoll.accessProvider, createPoll.accessId, createPoll.accessObj, createPoll.endSnapshotOrdinal)

    val newOnChain = SessionStateOnChain(state.onChain.updates :+ createPoll)
    val newCalculatedState = state.calculated.focus(_.sessions).modify(_.updated(sessionId, newSession))

    val result = DataState(newOnChain, newCalculatedState)
    logger.info(s"Returning new state after combineNotarizeSession: $result")
    result
  }

  def combineCreateSolSession(createPoll: CreateSolSession, state: DataState[SessionStateOnChain, SessionCalculatedState]): DataState[SessionStateOnChain, SessionCalculatedState] = {
    logger.info(s"combineNotarizeSession called with createPoll: $createPoll and state: $state")
    val sessionId = Hash.fromBytes(Serializers.serializeUpdate(createPoll)).toString
    logger.info(s"Generated sessionId: $sessionId")
    val newSession = Session(sessionId, createPoll.accessProvider, createPoll.solanaAddress, createPoll.accessObj, createPoll.endSnapshotOrdinal)

    val newOnChain = SessionStateOnChain(state.onChain.updates :+ createPoll)
    val newCalculatedState = state.calculated.focus(_.sessions).modify(_.updated(sessionId, newSession))

    val result = DataState(newOnChain, newCalculatedState)
    logger.info(s"Returning new state after combineNotarizeSession: $result")
    result
  }

  def combineExtendSession(extendSession: ExtendSession, state: DataState[SessionStateOnChain, SessionCalculatedState]): DataState[SessionStateOnChain, SessionCalculatedState] = {
    logger.info(s"combineExtendSession called with extendSession: $extendSession and state: $state")
    
    val existingSession = state.calculated.sessions(extendSession.id)
    val updatedSession = existingSession.copy(
      endSnapshotOrdinal = extendSession.endSnapshotOrdinal
    )

    val newOnChain = SessionStateOnChain(state.onChain.updates :+ extendSession)
    val newCalculatedState = state.calculated.focus(_.sessions).modify(_.updated(extendSession.id, updatedSession))

    val result = DataState(newOnChain, newCalculatedState)
    logger.info(s"Returning new state after combineExtendSession: $result")
    result
  }

  def combineCloseSession(closeSession: CloseSession, state: DataState[SessionStateOnChain, SessionCalculatedState], currentOrdinal: Long): DataState[SessionStateOnChain, SessionCalculatedState] = {
    logger.info(s"combineCloseSession called with closeSession: $closeSession and state: $state")
    
    val existingSession = state.calculated.sessions(closeSession.id)
    val updatedSession = existingSession.copy(
      endSnapshotOrdinal = currentOrdinal
    )

    val newOnChain = SessionStateOnChain(state.onChain.updates :+ closeSession)
    val newCalculatedState = state.calculated.focus(_.sessions).modify(_.updated(closeSession.id, updatedSession))

    val result = DataState(newOnChain, newCalculatedState)
    logger.info(s"Returning new state after combineCloseSession: $result")
    result
  }
}
