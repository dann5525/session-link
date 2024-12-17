package com.my.session_link.shared_data.combiners

import com.my.session_link.shared_data.serializers.Serializers
import com.my.session_link.shared_data.types.Types._
import monocle.Monocle.toAppliedFocusOps
import org.tessellation.currency.dataApplication.DataState
import org.tessellation.security.hash.Hash
import org.slf4j.LoggerFactory

object Combiners {

  private val logger = LoggerFactory.getLogger("Combiners")

  def combineNotarizeSession(createPoll: NotarizeSession, state: DataState[VoteStateOnChain, VoteCalculatedState]): DataState[VoteStateOnChain, VoteCalculatedState] = {
    logger.info(s"combineNotarizeSession called with createPoll: $createPoll and state: $state")
    val sessionId = Hash.fromBytes(Serializers.serializeUpdate(createPoll)).toString
    logger.info(s"Generated sessionId: $sessionId")
    val newSession = Session(sessionId, createPoll.accessProvider, createPoll.accessId, createPoll.accessObj, createPoll.endSnapshotOrdinal, "")

    val newOnChain = VoteStateOnChain(state.onChain.updates :+ createPoll)
    val newCalculatedState = state.calculated.focus(_.sessions).modify(_.updated(sessionId, newSession))

    val result = DataState(newOnChain, newCalculatedState)
    logger.info(s"Returning new state after combineNotarizeSession: $result")
    result
  }
}
