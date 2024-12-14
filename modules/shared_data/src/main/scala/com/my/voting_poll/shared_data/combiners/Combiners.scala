package com.my.voting_poll.shared_data.combiners

import com.my.voting_poll.shared_data.serializers.Serializers
import com.my.voting_poll.shared_data.types.Types._
import monocle.Monocle.toAppliedFocusOps
import org.tessellation.currency.dataApplication.DataState

import org.tessellation.security.hash.Hash

object Combiners {
  
  def combineCreateSession(createPoll: CreateSession, state: DataState[VoteStateOnChain, VoteCalculatedState]): DataState[VoteStateOnChain, VoteCalculatedState] = {
    val sessionId = Hash.fromBytes(Serializers.serializeUpdate(createPoll)).toString
    val newState = Session(sessionId, createPoll.accessProvider, createPoll.accessId, createPoll.accessObj, createPoll.endSnapshotOrdinal)

    val newOnChain = VoteStateOnChain(state.onChain.updates :+ createPoll)
    val newCalculatedState = state.calculated.focus(_.sessions).modify(_.updated(sessionId, newState))

    DataState(newOnChain, newCalculatedState)
  }
}