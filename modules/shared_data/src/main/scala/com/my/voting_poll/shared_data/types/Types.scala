package com.my.voting_poll.shared_data.types

import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive
import org.tessellation.currency.dataApplication.{DataCalculatedState, DataOnChainState, DataUpdate}
import org.tessellation.schema.address.Address

object Types {
  @derive(decoder, encoder)
  sealed trait PollUpdate extends DataUpdate

  @derive(decoder, encoder)
  case class CreatePoll(name: String, owner: Address, pollOptions: List[String], startSnapshotOrdinal: Long, endSnapshotOrdinal: Long) extends PollUpdate

  @derive(decoder, encoder)
  case class VoteInPoll(pollId: String, address: Address, option: String) extends PollUpdate

  @derive(decoder, encoder)
  case class Poll(id: String, name: String, owner: Address, pollOptions: Map[String, Long], usersVotes: Map[Address, Map[String, Long]], startSnapshotOrdinal: Long, endSnapshotOrdinal: Long)
  
  @derive(decoder, encoder)
  case class CreateSession(creator: Address, acessId:String, dataId: String, endSnapshotOrdinal: Long) extends PollUpdate
  
  @derive(decoder, encoder)
  case class Session(id: String, creator: Address, acessId:String, dataId: String, endSnapshotOrdinal: Long)  

 //What does each session need from an abstract point? 
 //sessionId for the state maping,
 //acessProvider entetie that is giving acess
 //acessId:: entetie that has acces
 //acessObj Object that the entie has acess to   -> if the metagraph has acess to the Objects metadata we can validate the acessProvider has allowance to give acess in the first place
 //acessDuration how long will the entetie have acess/ or some derivation.. how many time can the entite acess.. 

  @derive(decoder, encoder)
  case class VoteStateOnChain(updates: List[PollUpdate]) extends DataOnChainState

  @derive(decoder, encoder)
  case class VoteCalculatedState(polls: Map[String, Poll], sessions: Map[String, Session]) extends DataCalculatedState
}
