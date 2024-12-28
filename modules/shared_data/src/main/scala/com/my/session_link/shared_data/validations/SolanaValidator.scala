package com.my.session_link.shared_data.validations

import com.my.session_link.shared_data.types.Types.CreateSolSession
import org.slf4j.LoggerFactory
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer

import java.nio.charset.StandardCharsets
import java.math.BigInteger
import scala.util.{Try, Success, Failure}

object SolanaValidator {
  private val logger = LoggerFactory.getLogger("SolanaValidator")

  private val ALPHABET: String = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
  private val BASE: BigInteger = BigInteger.valueOf(58L)

  private def decodeBase58(input: String): Array[Byte] = {
    Try {
      var bi = BigInteger.ZERO
      for (c <- input.toCharArray) {
        // Explicitly convert char to String first
        val charStr: String = String.valueOf(c)
        val charIndex: Int = ALPHABET.indexOf(charStr)
        if (charIndex == -1) throw new IllegalArgumentException(s"Invalid character found: $c")
        bi = bi.multiply(BASE).add(BigInteger.valueOf(charIndex.toLong))
      }
      
      // Convert to byte array
      val bytes = bi.toByteArray
      
      // Remove leading zero bytes if present (due to BigInteger's sign bit)
      val leadingZeros = if (bytes(0) == 0 && bytes.length > 1) 1 else 0
      val result = new Array[Byte](bytes.length - leadingZeros)
      System.arraycopy(bytes, leadingZeros, result, 0, result.length)
      
      // Add back any leading zeros from the original Base58 string
      val numLeadingZeros = input.takeWhile(_ == '1').length
      if (numLeadingZeros > 0) {
        val withLeadingZeros = new Array[Byte](numLeadingZeros + result.length)
        System.arraycopy(result, 0, withLeadingZeros, numLeadingZeros, result.length)
        withLeadingZeros
      } else {
        result
      }
    }.getOrElse(Array.empty[Byte])
  }

  private def serializeSolSession(session: CreateSolSession): Array[Byte] = {
    logger.debug("Serializing CreateSolSession fields for Solana signature")
    val serialized = s"${session.solanaAddress}${session.accessProvider}${session.accessObj}${session.endSnapshotOrdinal}"
    serialized.getBytes(StandardCharsets.UTF_8)
  }

  def verifySolanaSignature(session: CreateSolSession): Boolean = {
    logger.info(s"Starting Solana signature verification for CreateSolSession with solanaAddress=${session.solanaAddress}")
    
    val verificationResult = Try {
      logger.info("Step 1: Serializing the CreateSolSession message")
      val messageBytes = serializeSolSession(session)
      logger.debug(s"Serialized message: ${new String(messageBytes, StandardCharsets.UTF_8)}")

      logger.info("Step 2: Decoding Solana public key from solanaAddress")
      val publicKeyBytes = decodeBase58(session.solanaAddress)
      if (publicKeyBytes.length != 32) {
        logger.error(s"Invalid Solana public key length: ${publicKeyBytes.length}, expected 32")
        return false
      }

      logger.info("Step 3: Parsing the Solana signature")
      val signatureBytes = decodeBase58(session.solanaSignature)
      if (signatureBytes.length != 64) {
        logger.error(s"Invalid Solana signature length: ${signatureBytes.length}, expected 64")
        return false
      }

      logger.info("Step 4: Setting up Ed25519 verification")
      val publicKey = new Ed25519PublicKeyParameters(publicKeyBytes, 0)
      val verifier = new Ed25519Signer()
      verifier.init(false, publicKey)
      verifier.update(messageBytes, 0, messageBytes.length)

      logger.info("Step 5: Verifying Solana signature")
      val isValid = verifier.verifySignature(signatureBytes)
      
      if (!isValid) {
        logger.warn("Solana signature verification failed")
      } else {
        logger.info("Solana signature verification succeeded")
      }

      isValid
    }

    verificationResult match {
      case Success(result) => result
      case Failure(e) =>
        logger.error(s"Error during Solana signature verification: ${e.getMessage}", e)
        false
    }
  }
}