package com.my.session_link.shared_data.types

import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive
import org.tessellation.currency.dataApplication.{DataCalculatedState, DataOnChainState, DataUpdate}
import org.tessellation.schema.address.Address
import java.time.Instant

object Types {
  @derive(decoder, encoder)
  sealed trait SessionUpdate extends DataUpdate

  @derive(decoder, encoder)
  sealed trait MetadataValue
  
  object MetadataValue {
    case class StringValue(value: String) extends MetadataValue
    case class NumberValue(value: Long) extends MetadataValue
    case class BooleanValue(value: Boolean) extends MetadataValue
    case class InstantValue(value: Instant) extends MetadataValue
    case class MapValue(value: Map[String, MetadataValue]) extends MetadataValue
    case class ListValue(value: List[MetadataValue]) extends MetadataValue
  }

  @derive(decoder, encoder)
  case class SessionMetadata(
    startTime: Instant,
    data: Map[String, MetadataValue]
  )

  @derive(decoder, encoder)
  case class NotarizeSession(
    accessProvider: Address, 
    accessId: String, 
    accessObj: String, 
    endSnapshotOrdinal: Long,
    metadata: Option[SessionMetadata] = None
  ) extends SessionUpdate

  @derive(decoder, encoder)
  case class CreateSession(
    accessProvider: Address, 
    accessId: String, 
    accessObj: String, 
    endSnapshotOrdinal: Long, 
    hash: String,
    metadata: Option[SessionMetadata] = None
  ) extends SessionUpdate

  @derive(decoder, encoder)
  case class CreateSolSession(
    accessProvider: Address,  
    solanaAddress: String, 
    accessObj: String, 
    endSnapshotOrdinal: Long, 
    solanaSignature: String,
    metadata: Option[SessionMetadata] = None
  ) extends SessionUpdate
  
  @derive(decoder, encoder)
  case class Session(
    id: String, 
    accessProvider: Address, 
    accessId: String, 
    accessObj: String, 
    endSnapshotOrdinal: Long,
    metadata: Option[SessionMetadata] = None
  )  

  @derive(decoder, encoder)
  case class SessionStateOnChain(updates: List[SessionUpdate]) extends DataOnChainState

  @derive(decoder, encoder)
  case class SessionCalculatedState(sessions: Map[String, Session]) extends DataCalculatedState

  // Helper methods and syntax extensions
  object SessionMetadata {
    import MetadataValue._

    def empty: SessionMetadata = SessionMetadata(Instant.now(), Map.empty)

    def create(data: Map[String, MetadataValue]): SessionMetadata = {
      SessionMetadata(Instant.now(), data)
    }

    implicit class MetadataOps(val metadata: SessionMetadata) extends AnyVal {
      def withValue(key: String, value: String): SessionMetadata = 
        metadata.copy(data = metadata.data + (key -> StringValue(value)))
        
      def withValue(key: String, value: Long): SessionMetadata = 
        metadata.copy(data = metadata.data + (key -> NumberValue(value)))
        
      def withValue(key: String, value: Boolean): SessionMetadata = 
        metadata.copy(data = metadata.data + (key -> BooleanValue(value)))
        
      def withValue(key: String, value: Instant): SessionMetadata = 
        metadata.copy(data = metadata.data + (key -> InstantValue(value)))

      def withValues(values: Map[String, MetadataValue]): SessionMetadata =
        metadata.copy(data = metadata.data ++ values)
    }
  }
}