package org.scalatra

import scala.util.matching.Regex
import scala.util.parsing.combinator._
import java.net.URLEncoder.encode
import util.MultiMap

trait RouteMatcher {

  def apply(): Option[ScalatraKernel.MultiParams]
}

trait ReversibleRouteMatcher {

  def reverse(params: Map[String, String], splats: List[String]): String
}

final class SinatraRouteMatcher(pattern: String, requestPath: => String)
  extends RouteMatcher with ReversibleRouteMatcher {

  lazy val generator: (Builder => Builder) = BuilderGeneratorParser(pattern)

  def apply() = SinatraPathPatternParser(pattern)(requestPath)

  def reverse(params: Map[String, String], splats: List[String]): String =
    generator(Builder("", params, splats)).get

  case class Builder(path: String, params: Map[String, String], splats: List[String]) {

    def addLiteral(text: String): Builder = copy(path = path + text)

    def addSplat: Builder = copy(path = path + splats.head, splats = splats.tail)

    def addNamed(name: String): Builder =
      if (params contains name) copy(path = path + params(name), params = params - name)
      else throw new Exception("Builder \"%s\" requires param \"%s\"" format (pattern, name))

    def addOptional(name: String): Builder =
      if (params contains name) copy(path = path + params(name), params = params - name)
      else this

    def addPrefixedOptional(name: String, prefix: String): Builder =
      if (params contains name) copy(path = path + prefix + params(name), params = params - name)
      else this

    // checks all splats are used, appends additional params as a query string
    def get: String = {
      if (!splats.isEmpty) throw new Exception("Too many splats for builder \"%s\"" format pattern)
      val pairs = params map { case(key, value) => encode(key, "utf-8") + "=" +encode(value, "utf-8") }
      val queryString = if (pairs.isEmpty) "" else pairs.mkString("?", "&", "")
      path + queryString
    }
  }

  object BuilderGeneratorParser extends RegexParsers {

    def apply(pattern: String): (Builder => Builder) = parseAll(tokens, pattern) get

    private def tokens: Parser[Builder => Builder] = rep(token) ^^ {
      tokens => tokens reduceLeft ((acc, fun) => builder => fun(acc(builder)))
    }

    private def token: Parser[Builder => Builder] = splat | prefixedOptional | optional | named | literal

    private def splat: Parser[Builder => Builder] = "*" ^^^ { builder => builder addSplat }

    private def prefixedOptional: Parser[Builder => Builder] =
      ("." | "/") ~ "?:" ~ """\w+""".r ~ "?" ^^ {
        case p ~ "?:" ~ o ~ "?" => builder => builder addPrefixedOptional (o, p)
      }

    private def optional: Parser[Builder => Builder] =
      "?:" ~> """\w+""".r <~ "?" ^^ { str => builder => builder addOptional str }

    private def named: Parser[Builder => Builder] =
      ":" ~> """\w+""".r ^^ { str => builder => builder addNamed str }

    private def literal: Parser[Builder => Builder] =
      ("""[\.\+\(\)\$]""".r | ".".r) ^^ { str => builder => builder addLiteral str }
  }

  override def toString = pattern
}

final class RailsRouteMatcher(pattern: String, requestPath: => String)
  extends RouteMatcher with ReversibleRouteMatcher {

  lazy val generator: (Builder => Builder) = BuilderGeneratorParser(pattern)

  def apply() = RailsPathPatternParser(pattern)(requestPath)

  def reverse(params: Map[String, String], splats: List[String]): String =
    generator(Builder("", params)).get

  case class Builder(path: String, params: Map[String, String]) {

    def addStatic(text: String): Builder = copy(path = path + text)

    def addParam(name: String): Builder =
      if (params contains name) copy(path = path + params(name), params = params - name)
      else throw new Exception("Builder \"%s\" requires param \"%s\"" format (pattern, name))

    def optional(builder: Builder => Builder): Builder =
      try builder(this)
      catch { case e: Exception => this }

    // appends additional params as a query string
    def get: String = {
      val pairs = params map { case(key, value) => encode(key, "utf-8") + "=" +encode(value, "utf-8") }
      val queryString = if (pairs.isEmpty) "" else pairs.mkString("?", "&", "")
      path + queryString
    }
  }

  object BuilderGeneratorParser extends RegexParsers {

    def apply(pattern: String): (Builder => Builder) = parseAll(tokens, pattern) get

    private def tokens: Parser[Builder => Builder] = rep(token) ^^ {
      tokens => tokens reduceLeft ((acc, fun) => builder => fun(acc(builder)))
    }

    //private def token = param | glob | optional | static
    private def token: Parser[Builder => Builder] = param | glob | optional | static

    private def param: Parser[Builder => Builder] =
      ":" ~> identifier ^^ { str => builder => builder addParam str }

    private def glob: Parser[Builder => Builder] =
      "*" ~> identifier ^^ { str => builder => builder addParam str }

    private def optional: Parser[Builder => Builder] =
      "(" ~> tokens <~ ")" ^^ { subBuilder => builder => builder optional subBuilder }

    private def static: Parser[Builder => Builder] =
      (escaped | char) ^^ { str => builder => builder addStatic str }

    private def identifier = """[a-zA-Z_]\w*""".r

    private def escaped = literal("\\") ~> (char | paren)

    private def char = metachar | stdchar

    private def metachar = """[.^$|?+*{}\\\[\]-]""".r

    private def stdchar = """[^()]""".r

    private def paren = ("(" | ")")
  }
}

final class PathPatternRouteMatcher(pattern: PathPattern, requestPath: => String)
  extends RouteMatcher {

  def apply() = pattern(requestPath)

  override def toString = pattern.regex.toString
}

final class RegexRouteMatcher(regex: Regex, requestPath: => String)
  extends RouteMatcher {

  def apply() = regex.findFirstMatchIn(requestPath) map { _.subgroups match {
    case Nil => MultiMap()
    case xs => Map("captures" -> xs)
  }}

  override def toString = regex.toString
}

final class BooleanBlockRouteMatcher(block: => Boolean) extends RouteMatcher {

  def apply() = if (block) Some(MultiMap()) else None

  override def toString = "[Boolean Guard]"
}
