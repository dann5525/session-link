package com.my.session_link.shared_data.validations

import com.my.session_link.shared_data.errors.Errors._
import com.my.session_link.shared_data.types.Types.{NotarizeSession, SessionCalculatedState, SessionStateOnChain, CreateSession}
import org.tessellation.currency.dataApplication.DataState
import org.tessellation.currency.schema.currency.CurrencySnapshotInfo
import org.tessellation.schema.SnapshotOrdinal
import org.tessellation.schema.address.Address
import org.bouncycastle.crypto.digests.KeccakDigest
import org.web3j.crypto.{Keys, Sign}
import org.web3j.utils.Numeric
import org.web3j.crypto.Sign.SignatureData
import org.slf4j.LoggerFactory

import java.nio.charset.StandardCharsets
import scala.util.{Try, Success, Failure}

object TypeValidators {
  private val logger = LoggerFactory.getLogger("TypeValidators")

  private def hexToBytes(hex: String): Array[Byte] = {
    val cleanHex = hex.stripPrefix("0x").toLowerCase
    require(cleanHex.matches("[0-9a-f]*"), "Invalid hex string")
    require(cleanHex.length % 2 == 0, "Hex string must have even length")
    cleanHex.sliding(2, 2).toArray.map(Integer.parseInt(_, 16).toByte)
  }

  private def serializeCreateSession(session: CreateSession): Array[Byte] = {
    logger.debug("Serializing CreateSession fields for Ethereum signature")
    val serialized = s"${session.accessId}${session.accessProvider}${session.accessObj}${session.endSnapshotOrdinal}"
    serialized.getBytes(StandardCharsets.UTF_8)
  }

  private def keccak256(input: Array[Byte]): Array[Byte] = {
    val digest = new KeccakDigest(256)
    val output = new Array[Byte](32)
    digest.update(input, 0, input.length)
    digest.doFinal(output, 0)
    output
  }

  private def getEthereumMessage(message: Array[Byte]): Array[Byte] = {
    val prefix = "\u0019Ethereum Signed Message:\n"
    val length = message.length.toString
    val prefixBytes = (prefix + length).getBytes(StandardCharsets.UTF_8)
    
    val fullMessage = new Array[Byte](prefixBytes.length + message.length)
    System.arraycopy(prefixBytes, 0, fullMessage, 0, prefixBytes.length)
    System.arraycopy(message, 0, fullMessage, prefixBytes.length, message.length)
    
    fullMessage
  }


  def verifySignature(session: CreateSession): Boolean = {
  logger.info(s"Starting signature verification for CreateSession with accessId=${session.accessId}")
  logger.info("Step 1: Serializing the CreateSession message")
  
  val messageBytes = serializeCreateSession(session)
  logger.debug(s"Serialized message as UTF-8 string: ${new String(messageBytes, StandardCharsets.UTF_8)}")
  logger.debug(s"Serialized message (hex): ${Numeric.toHexString(messageBytes)}")

  logger.info("Step 2: Prefixing message with Ethereum signed message header")
  val ethMessage = getEthereumMessage(messageBytes)
  logger.debug(s"Ethereum message (raw bytes): ${ethMessage.map("%02x".format(_)).mkString}")
  logger.debug(s"Ethereum message (hex): ${Numeric.toHexString(ethMessage)}")

  logger.info("Step 3: Computing keccak256 hash of Ethereum message")
  val messageHash = keccak256(ethMessage)
  logger.debug(s"Message keccak256 hash (hex): ${Numeric.toHexString(messageHash)}")

  logger.info("Step 4: Parsing the signature from the session.hash field")
  logger.debug(s"Raw signature (from CreateSession.hash): ${session.hash}")

  val verificationResult = Try {
    parseSignature(session.hash) match {
      case Success(sigData) =>
        logger.info("Step 5: Successfully parsed signature data")
        logger.debug(s"SignatureData v: ${sigData.getV}, r: ${Numeric.toHexString(sigData.getR)}, s: ${Numeric.toHexString(sigData.getS)}")

        logger.info("Step 6: Attempting to recover the public key from the signature and message hash")
        try {
          val publicKey = Sign.signedMessageHashToKey(messageHash, sigData)
          val recoveredAddress = "0x" + Keys.getAddress(publicKey)

          logger.debug(s"Recovered public key (hex): ${publicKey.toString(16)}")
          logger.debug(s"Recovered address: $recoveredAddress")
          logger.debug(s"Expected address: ${session.accessId}")

          val addressesMatch = recoveredAddress.equalsIgnoreCase(session.accessId)
          if (!addressesMatch) {
            logger.warn(s"Recovered address does not match expected address: $recoveredAddress vs ${session.accessId}")
          } else {
            logger.info("Signature verification succeeded: recovered address matches expected address")
          }

          addressesMatch
        } catch {
          case e: Exception =>
            logger.error(s"Error during signature verification (public key recovery step): ${e.getMessage}", e)
            false
        }

      case Failure(e) =>
        logger.error(s"Failed to parse signature: ${e.getMessage}", e)
        false
    }
  }

  verificationResult match {
    case Success(result) =>
      if (!result) {
        logger.warn("Signature verification failed at some point after parsing signature.")
      }
      result
    case Failure(e) =>
      logger.error(s"Unexpected error during signature verification: ${e.getMessage}", e)
      false
  }
}

private def parseSignature(signature: String): Try[SignatureData] = {
  Try {
    logger.info("Parsing raw signature string into r, s, v components")
    logger.debug(s"Raw signature input: $signature")

    val sigBytes = hexToBytes(signature)
    logger.debug(s"Signature bytes length: ${sigBytes.length}")
    require(sigBytes.length == 65, s"Invalid signature length: ${sigBytes.length}, expected 65")

    logger.debug(s"Raw signature bytes (hex): ${sigBytes.map("%02x".format(_)).mkString}")

    val r = java.util.Arrays.copyOfRange(sigBytes, 0, 32)
    val s = java.util.Arrays.copyOfRange(sigBytes, 32, 64)
    val rawV: Int = sigBytes(64) & 0xff

    logger.debug(s"Extracted r (hex): ${Numeric.toHexString(r)}")
    logger.debug(s"Extracted s (hex): ${Numeric.toHexString(s)}")
    logger.debug(s"Extracted v (raw): $rawV")

    // Adjust v if needed. If v is not 27 or 28, try adding 27 if less than 27.
    val v: Byte = {
      if (rawV < 27) (rawV + 27).toByte else rawV.toByte
    }

    logger.debug(s"Final v (adjusted if needed): $v")

    new SignatureData(v, r, s)
  }
}


  // Existing validators remain unchanged
  def validateIfSessionAlreadyExists(state: DataState[SessionStateOnChain, SessionCalculatedState], id: String): DataApplicationValidationType = {
    logger.debug(s"Validating if session already exists with id=$id")
    PollAlreadyExists.whenA(state.calculated.sessions.contains(id))
  }

  def validateProvidedAddress(proofAddresses: List[Address], address: Address): DataApplicationValidationType = {
    logger.debug(s"Validating provided address: $address against proofAddresses: $proofAddresses")
    InvalidAddress.unlessA(proofAddresses.contains(address))
  }

  def validateWalletBalance(snapshotInfo: CurrencySnapshotInfo, walletAddress: Address): DataApplicationValidationType = {
    val balance = snapshotInfo.balances.get(walletAddress).map(_.value.value).getOrElse(0L)
    logger.debug(s"Validating wallet balance for address=$walletAddress: balance=$balance")
    NotEnoughWalletBalance.unlessA(balance > 0L)
  }

  def validateSnapshotNotarizeSession(snapshotOrdinal: SnapshotOrdinal, update: NotarizeSession): DataApplicationValidationType = {
    logger.debug(s"Validating snapshot notarize session: update.endSnapshotOrdinal=${update.endSnapshotOrdinal}, snapshotOrdinal=${snapshotOrdinal.value.value}")
    InvalidEndSnapshot.whenA(update.endSnapshotOrdinal < snapshotOrdinal.value.value)
  }

  def validateSnapshotCreateSession(snapshotOrdinal: SnapshotOrdinal, update: CreateSession): DataApplicationValidationType = {
    logger.debug(s"Validating snapshot create session: update.endSnapshotOrdinal=${update.endSnapshotOrdinal}, snapshotOrdinal=${snapshotOrdinal.value.value}")
    InvalidEndSnapshot.whenA(update.endSnapshotOrdinal < snapshotOrdinal.value.value)
  }

  def validateCreateSessionSignature(session: CreateSession): DataApplicationValidationType = {
    logger.debug(s"Validating Ethereum signature for CreateSession with accessId=${session.accessId}")
    InvalidSig.unlessA(verifySignature(session))
  }
}