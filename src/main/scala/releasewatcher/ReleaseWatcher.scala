package releasewatcher

import org.scalajs.dom
import org.scalajs.dom.html.Label
import releasewatcher.firebase.auth.GithubAuthProvider
import releasewatcher.firebase.{Firebase, User}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.JSApp
import scala.util.{Failure, Success, Try}
import scala.scalajs.js.timers._

@js.native
trait Release extends js.Object {
  val title: String = js.native
  val url: String = js.native
  val rawTime: String = js.native
  val time: String = js.native
  val description: String = js.native
}

object Release {
  def apply(json: js.Dynamic): Release = {
    val repo = Option(json.url).map(_.toString.split("/")(5)).getOrElse("-")

    val cs = Option(json.published_at).map(_.toString.split("-")) getOrElse {
      Array("0000", "00", "00") // Not yet released
    }

    val time = if(cs.length < 3) "" else s"${cs(2) take 2}.${cs(1)}.${cs(0)}"

    // TODO: parse feature / fix counts from body
    val description = Option(json.name) map { _.toString } getOrElse "(no name)"

    js.Dynamic.literal(url = json.html_url, title = s"$repo ${json.tag_name}", rawTime = json.published_at, time = time, description = description).asInstanceOf[Release]
  }

  def duplicate(r: Release)(other: Release): Boolean = r.url == other.url

  def empty(): Release = js.Dynamic.literal(url = "", title = "-", rawTime = "-", time = "-", description = "-").asInstanceOf[Release]

  def nonEmpty(r: Release): Boolean = r.title != "-"
}

@js.native
trait DataProviderParams extends js.Object {
  val page: Int = js.native
  val pageSize: Int = js.native
}

object App extends JSApp {
  val organization = "vaadin"

  val repos = Seq("vaadin-combo-box", "framework", "designer", "vaadin-grid",
    "vaadin-context-menu", "vaadin-combo-box", "vaadin-grid", "vaadin-context-menu",
    "vaadin-split-layout", "vaadin-date-picker", "vaadin-upload",
    "vaadin-form-layout", "vaadin-button", "framework", "spring", "testbench", "charts",
    "spreadsheet", "board", "vaadin-icons", "vaadin-button", "vaadin-text-field",
    "vaadin-charts", "vaadin-form-layout", "vaadin-button", "maven-plugin", "cdi",
    "eclipse-plugin", "vaadin-board", "flow")

  def main(): Unit = {
    Option(dom.window.localStorage.getItem("github-token")) match {
      case Some(token) =>
        println("Using stored token")
        showReleases(token)
      case None => signIn()
    }
  }

  private def signIn(): Unit = Firebase.auth().getRedirectResult() `then` { r =>
    val result = r.asInstanceOf[js.Dynamic]
    val user = result.user.asInstanceOf[User]

    if(user != null) {
      println(s"${user.email} signed in")

      dom.window.localStorage.setItem("github-token", result.credential.accessToken.toString)

      showReleases(result.credential.accessToken.toString)
    } else {
      // No user, authenticate
      val provider = new GithubAuthProvider()
      provider.addScope("repo")

      Firebase.auth().signInWithRedirect(provider)
    }
  } `catch` { error =>
    if(error == null) println(s"Redirect error: NULL")
    else println(s"Redirect error: $error")
  }

  private lazy val releaseGrid = dom.document.body.querySelector("#grid").asInstanceOf[js.Dynamic]

  private lazy val loadingLabel = dom.document.body.querySelector("#loading").asInstanceOf[Label]

  private var releases: js.Array[Release] = js.Array()

  private def showReleases(token: String): Unit = {
    loadingLabel.innerHTML = s"Fetching releases for ${repos.size} repositories..."

    var forwarded = false

    val urls = repos map { repo =>
      dom.ext.Ajax.get(s"https://api.github.com/repos/$organization/$repo/releases?access_token=$token")
    }

    // Lift potentially failed XHRs into Failure's
    val lifted = urls.map(_.map(Success(_)).recover {
      case ex if ex == null =>
        println(s"XHR error: NULL")
        Failure(ex)

      case ex =>
        println(s"XHR error: $ex")
        if(!forwarded) {
          signIn()
          forwarded = true
        }
        Failure(ex)
    })

    // Convert the sequence of futures into a future of sequences.
    // Collect will only accept succeeded XHRs.
    val releasesFuture: Future[Seq[Seq[Release]]] = Future.sequence(lifted) map { _ map {
        case Success(xhr) => Try(js.JSON.parse(xhr.responseText)) match {
          case Success(result) => result.asInstanceOf[js.Array[js.Dynamic]].toSeq map Release.apply

          case Failure(ex) =>
            println(s"Failed to parse XHR result: ${ex.getMessage}\n${xhr.responseText}")
            Seq(Release.empty())
        }

        case Failure(ex) =>
          println(s"XHR fail: ${ex.getMessage}")
          Seq(Release.empty())
      }
    }

    // Trigger the XHRs and set up the callback to build the result table based on response
    releasesFuture onComplete {
      case Success(rrs: Seq[Seq[Release]]) =>
        loadingLabel.innerHTML = s"Updating..."

        // Org admins can see unreleased tags
        val released: Seq[Release] = rrs.flatten filter Release.nonEmpty // { r => r.rawTime != null && r.tag != "-" }

        val deduped: Seq[Release] = released.foldLeft[Seq[Release]] (Seq[Release]()) {
          case (accu, r) => if(accu exists Release.duplicate(r)) accu else accu :+ r
        }

        releases = deduped.sortBy(_.rawTime).reverse.to[js.Array]

        setTimeout(1) { // Make sure the loading label updates before starting lazy load
          releaseGrid.size = releases.size

          releaseGrid.dataProvider = { (params: DataProviderParams, callback: js.Dynamic) =>
            callback(releases.jsSlice(params.pageSize * params.page), params.pageSize)
          }

          dom.document.body.querySelector("#loading").classList.add("hidden") // Hide loading message
        }

      case Failure(ex) if ex == null => println(s"XHR failure: NULL")

      case Failure(ex) => println(s"XHR failure: ${ex.getMessage}")
    }
  }
}
