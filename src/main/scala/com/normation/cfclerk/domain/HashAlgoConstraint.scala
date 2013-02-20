/*
*************************************************************************************
* Copyright 2011 Normation SAS
*************************************************************************************
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Affero General Public License as
* published by the Free Software Foundation, either version 3 of the
* License, or (at your option) any later version.
*
* In accordance with the terms of section 7 (7. Additional Terms.) of
* the GNU Affero GPL v3, the copyright holders add the following
* Additional permissions:
* Notwithstanding to the terms of section 5 (5. Conveying Modified Source
* Versions) and 6 (6. Conveying Non-Source Forms.) of the GNU Affero GPL v3
* licence, when you create a Related Module, this Related Module is
* not considered as a part of the work and may be distributed under the
* license agreement of your choice.
* A "Related Module" means a set of sources files including their
* documentation that, without modification of the Source Code, enables
* supplementary functions or services in addition to those offered by
* the Software.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Affero General Public License for more details.
*
* You should have received a copy of the GNU Affero General Public License
* along with this program. If not, see <http://www.gnu.org/licenses/agpl.html>.
*
*************************************************************************************
*/

package com.normation.cfclerk.domain

import com.normation.utils.HashcodeCaching
import net.liftweb.common.{ Box, Failure, Full }
import java.security.MessageDigest
import net.liftweb.common.EmptyBox
import org.apache.commons.codec.binary.Base64
import org.apache.commons.codec.binary.Hex

object HashAlgoConstraint {

  def algorithmes = PLAIN :: MD5 :: SHA1 :: SHA256 :: Nil
  def algoNames =  algorithmes.map( _.prefix ).mkString(", ")

  def fromString(algo:String) : Option[HashAlgoConstraint] = {
    algorithmes.find( a => a.prefix == algo.toLowerCase)
  }

  /*
   * Hash will be store with the format: algo:hash
   * So there is the regex to read them back.
   */

  private[this] val format = """([\w]+):(.*)""".r
  def unserialize(value:String): Box[(HashAlgoConstraint, String)] = value match {
    case format(algo,h) => HashAlgoConstraint.fromString(algo) match {
      case None => Failure(s"Unknow algorithm ${algo}. List of know algorithme: ${HashAlgoConstraint.algoNames}")
      case Some(a) => Full((a,h))
    }
    case _ => Failure(s"Bad format of serialized hashed value, expexted format is: 'algorithme:hash', with algorithm among: ${HashAlgoConstraint.algoNames}")
  }
}

sealed trait HashAlgoConstraint {
  def prefix: String
  def hash(input:Array[Byte]): String

  /**
   * Serialize an input to the storage format:
   * algotype:hash
   */
  def serialize(input:Array[Byte]): String = s"${prefix}:${hash(input)}"
  def unserialize(value:String) : Box[String] = HashAlgoConstraint.unserialize(value) match {
    case Full((algo, v)) if algo == this => Full(v)
    case Full((algo,_)) => Failure(s"Bad algorithm prefix: found ${algo.prefix}, was expecting ${this.prefix}")
    case eb: EmptyBox => eb
  }
}


/**
 * Actually do not hash the result
 */
object PLAIN  extends HashAlgoConstraint {
  override def hash(input:Array[Byte]) : String = new String(input, "UTF-8")
  override val prefix = "plain"
}

object MD5  extends HashAlgoConstraint {
  private[this] val md = MessageDigest.getInstance("MD5")
  override def hash(input:Array[Byte]) : String = Hex.encodeHexString(md.digest(input))
  override val prefix = "md5"
}

object SHA1 extends HashAlgoConstraint {
  private[this] val md = MessageDigest.getInstance("SHA-1")
  override def hash(input:Array[Byte]) : String = Hex.encodeHexString(md.digest(input))
  override val prefix = "sha1"
}

object SHA256 extends HashAlgoConstraint {
  private[this] val md = MessageDigest.getInstance("SHA-256")
  override def hash(input:Array[Byte]) : String = Hex.encodeHexString(md.digest(input))
  override val prefix = "sha256"
}

