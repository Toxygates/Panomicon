/*
 * Copyright (c) 2012-2015 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition 
 * (NIBIOHN), Japan.
 *
 * This file is part of Toxygates.
 *
 * Toxygates is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Toxygates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Toxygates. If not, see <http://www.gnu.org/licenses/>.
 */

package t.viewer.server

import java.util.Properties
import javax.mail.Session
import javax.mail.internet.MimeMessage
import javax.mail.internet.InternetAddress
import javax.mail.Message
import javax.mail.Address
import javax.mail.Transport
import scala.Array.canBuildFrom

object Feedback {
  
  implicit def asAddressAr(xs: Array[InternetAddress]): Array[Address] = {
    xs.map(_.asInstanceOf[Address]).toArray
  }

  /**
   *  Send a feedback message, as well as some application state,
   *  by e-mail.
   */
  def send(user: String, email: String, message: String, userState: String,
      receiverList: String): Unit = {
    val p = new Properties()
    p.setProperty("mail.smtp.host", "localhost")

    try {
      val s = Session.getInstance(p)
      val m = new MimeMessage(s)
      m.setFrom(new InternetAddress("root@nibio.go.jp"))

      m.setRecipients(Message.RecipientType.TO,
        InternetAddress.parse(receiverList))

      m.setSubject(s"[System message] Toxygates user feedback from $user")
      m.setText(s"Feedback from: $user <$email>\n\nMessage: $message\n\nUser state: $userState")
      Transport.send(m)
    } catch {
      case e: Exception =>
        println("Exception while trying to send email.")
        println(s"User: $user email: $email")
        println("Feedback:")
        println(message)
        throw e //re-throw to the GUI
    }
  }
}