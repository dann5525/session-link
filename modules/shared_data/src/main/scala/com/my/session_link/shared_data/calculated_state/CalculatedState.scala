package com.my.session_link.shared_data.calculated_state

import com.my.session_link.shared_data.types.Types.SessionCalculatedState
import eu.timepit.refined.types.all.NonNegLong
import org.tessellation.schema.SnapshotOrdinal

case class CalculatedState(ordinal: SnapshotOrdinal, state: SessionCalculatedState)

object CalculatedState {
  def empty: CalculatedState =
    CalculatedState(SnapshotOrdinal(NonNegLong.MinValue), SessionCalculatedState(Map.empty))
}