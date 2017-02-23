package releasewatcher

import scalatags.JsDom.all._
import scalatags.stylesheet._

object Styles extends StyleSheet {
  def body = cls(
    margin := 0.px,
    display := "flex",
    backgroundColor := "white",
    height := 100.pct,
    width := 100.pct,
    fontFamily := "'Helvetica Neue', 'Calibri Light', Roboto, sans-serif"
  )

  def table = cls(width := 100.pct)

  def thead = cls(fontWeight := "bold")

  initStyleSheet()
}
