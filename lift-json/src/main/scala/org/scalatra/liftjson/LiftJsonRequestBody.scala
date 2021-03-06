package org.scalatra
package liftjson

import net.liftweb.json._
import annotation.tailrec
import net.liftweb.json.Xml._
import util.RicherString._
import java.nio.CharBuffer


object LiftJsonRequestBody {

  val ParsedBodyKey = "org.scalatra.liftjson.ParsedBody".intern
}
/**
 * Parses request bodies with lift json if the appropriate content type is set.
 * Be aware that it also parses XML and returns a JValue of the parsed XML.
 */
trait LiftJsonRequestBody extends ScalatraKernel with ApiFormats {

  protected implicit def jsonFormats = DefaultFormats

  import LiftJsonRequestBody._
  private def requestBody: String = {
    val sb = new StringBuilder
    val reader = request.getReader
    val buf = new Array[Char](1000)
//    val cb = CharBuffer.allocate(1000)

    def fromBuffer = reader.read(buf, 0, 1000)
    @tailrec
    def readBody(read: Int): String = {
      if (read <= 0) {
        sb.toString()
      } else {
        sb appendAll buf.slice(0, read)
        readBody(fromBuffer)
      }
    }
    readBody(fromBuffer)
  }

  protected def parseRequestBody(format: String, content: String) = try {
    if (format == "json") {
      transformRequestBody(JsonParser.parse(content))
    } else if (format == "xml") {
      transformRequestBody(toJson(scala.xml.XML.loadString(content)))
    } else JString(content)
  } catch { case _ ⇒ JNothing }

  protected def transformRequestBody(body: JValue) = body

  override protected def invoke(matchedRoute: MatchedRoute) = {
    withRouteMultiParams(Some(matchedRoute)) {
      val mt = request.getContentType.toOption map { _.split(";").head } getOrElse "application/x-www-form-urlencoded"
      val fmt = mimeTypes get mt getOrElse "html"
      if (fmt == "json" || fmt == "xml") {
        request(ParsedBodyKey) = parseRequestBody(fmt, requestBody)
      }
      super.invoke(matchedRoute)
    }
  }

  def parsedBody = request.get(ParsedBodyKey) getOrElse JNothing

}
