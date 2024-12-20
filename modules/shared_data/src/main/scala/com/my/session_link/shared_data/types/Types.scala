package com.my.session_link.shared_data.types

import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive
import org.tessellation.currency.dataApplication.{DataCalculatedState, DataOnChainState, DataUpdate}
import org.tessellation.schema.address.Address

object Types {
  @derive(decoder, encoder)
  sealed trait SessionUpdate extends DataUpdate

 
  @derive(decoder, encoder)
  case class NotarizeSession(accessProvider: Address, accessId:String, accessObj: String, endSnapshotOrdinal: Long) extends SessionUpdate

    @derive(decoder, encoder)
  case class CreateSession(accessProvider: Address, accessId:String, accessObj: String, endSnapshotOrdinal: Long, hash: String) extends SessionUpdate
  
  @derive(decoder, encoder)
  case class Session(id: String, accessProvider: Address, accessId:String, accessObj: String, endSnapshotOrdinal: Long)  

 //What does each session need from an abstract point? 
 //sessionId for the state maping,
 //acessProvider entetie that is giving acess
 //acessId:: entetie that has acces
 //acessObj Object that the entie has acess to   -> if the metagraph has acess to the Objects metadata we can validate the acessProvider has allowance to give acess in the first place
 //acessDuration how long will the entetie have acess/ or some derivation.. how many time can the entite acess.. 

  @derive(decoder, encoder)
  case class SessionStateOnChain(updates: List[SessionUpdate]) extends DataOnChainState

  @derive(decoder, encoder)
  case class SessionCalculatedState(sessions: Map[String, Session]) extends DataCalculatedState
}
