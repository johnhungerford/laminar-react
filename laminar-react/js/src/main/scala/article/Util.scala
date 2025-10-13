package article

import scala.util.Random

/** Utilities for generating signals to demo the examples
  */
object Util:
    def randomString(size: Int): String =
        Random.alphanumeric.filterNot(_.isDigit).take(size).mkString

    def randomWord(minLen: Int, maxLen: Int): String =
        randomString(minLen + Random.nextInt(maxLen - minLen))

    def randomWords(length: Int, minWordLength: Int, maxWordLength: Int): List[String] =
        (1 to length).map(_ => randomWord(minWordLength, maxWordLength)).toList

    def randomPhrase(length: Int, minWordLength: Int, maxWordLength: Int): String =
        randomWords(length, minWordLength, maxWordLength).mkString(" ")

    def randomWords(minLength: Int, maxLength: Int, minWordLength: Int, maxWordLength: Int): List[String] =
        randomWords(minLength + Random.nextInt(maxLength - minLength), minWordLength, maxWordLength)

    def randomPhrase(minLength: Int, maxLength: Int, minWordLength: Int, maxWordLength: Int): String =
        randomPhrase(minLength + Random.nextInt(maxLength - minLength), minWordLength, maxWordLength)
