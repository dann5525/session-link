package com.my.session_link.shared_data.validations

import com.my.session_link.shared_data.errors.Errors._
import com.my.session_link.shared_data.types.Types.{NotarizeSession, VoteCalculatedState, VoteStateOnChain}
import org.tessellation.currency.dataApplication.DataState
import org.tessellation.currency.schema.currency.CurrencySnapshotInfo
import org.tessellation.schema.SnapshotOrdinal
import org.tessellation.schema.address.Address

object TypeValidators {
  
  def validateIfSessionAlreadyExists(state: DataState[VoteStateOnChain, VoteCalculatedState],id: String): DataApplicationValidationType =
    PollAlreadyExists.whenA(state.calculated.sessions.contains(id))  


  def validateProvidedAddress(proofAddresses: List[Address], address: Address): DataApplicationValidationType =
    InvalidAddress.unlessA(proofAddresses.contains(address))


  def validateWalletBalance(snapshotInfo: CurrencySnapshotInfo, walletAddress: Address): DataApplicationValidationType =
    NotEnoughWalletBalance.unlessA(snapshotInfo.balances.get(walletAddress).exists(_.value.value > 0L))


  def validateSnapshotNotarizeSession(snapshotOrdinal: SnapshotOrdinal, update: NotarizeSession): DataApplicationValidationType=
    InvalidEndSnapshot.whenA(update.endSnapshotOrdinal < snapshotOrdinal.value.value)

}

