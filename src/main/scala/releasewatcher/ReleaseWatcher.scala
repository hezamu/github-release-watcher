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
  val repo: String = js.native
  val tag: String = js.native
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

    js.Dynamic.literal(repo = repo, tag = json.tag_name, rawTime = json.published_at, time = time, description = json.body).asInstanceOf[Release]
  }

  def str(r: Release): String = s"${r.rawTime}, ${r.repo}, ${r.tag}"
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
    Firebase.auth().getRedirectResult() `then` { r =>
      val result = r.asInstanceOf[js.Dynamic]
      val user = result.user.asInstanceOf[User]

      if(user != null) {
        println(s"${user.email} signed in")

        loadingLabel.innerHTML = s"Fetching releases for ${repos.size} repositories..."

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
  }

  private lazy val releaseGrid = dom.document.body.querySelector("#grid").asInstanceOf[js.Dynamic]

  private lazy val loadingLabel = dom.document.body.querySelector("#loading").asInstanceOf[Label]

  private var releases: js.Array[Release] = js.Array()

  private def showReleases(token: String): Unit = {
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
        Failure(ex)
    })

    // Convert the sequence of futures into a future of sequences.
    // Collect will only accept succeeded XHRs.
    val releasesFuture: Future[Seq[Seq[Release]]] = Future.sequence(lifted) map { _ map {
        case Success(xhr) => Try(js.JSON.parse(xhr.responseText)) match {
          case Success(result) => result.asInstanceOf[js.Array[js.Dynamic]].toSeq map Release.apply

          case Failure(ex) =>
            println(s"Failed to parse XHR result: ${ex.getMessage}\n${xhr.responseText}")
            Seq(js.Dynamic.literal(repo = "-", tag = "-", rawTime = "-", time = "-", description = "-").asInstanceOf[Release])
        }

        case Failure(ex) =>
          println(s"XHR fail: ${ex.getMessage}")
          Seq(js.Dynamic.literal(repo = "-", tag = "-", rawTime = "-", time = "-", description = "-").asInstanceOf[Release])
      }
    }

    // Trigger the XHRs and set up the callback to build the result table based on response
    releasesFuture onComplete {
      case Success(rrs: Seq[Seq[Release]]) =>
        loadingLabel.innerHTML = s"Updating..."

        releases = (rrs.flatten filter { _.rawTime != null } sortBy { _.rawTime }).reverse.to[js.Array]

        setTimeout(1) {
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
