package releasewatcher

import org.scalajs.dom
import releasewatcher.firebase.auth.GithubAuthProvider
import releasewatcher.firebase.{Firebase, User}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.JSApp
import scala.util.{Failure, Success}

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
    val repo = json.url.toString.split("/")(5)
    val cs = json.published_at.toString.split("-")
    val time = s"${cs(2) take 2}.${cs(1)}.${cs(0)}"

    js.Dynamic.literal(repo = repo, tag = json.tag_name, rawTime = json.published_at, time = time, description = json.body).asInstanceOf[Release]
  }
}

object App extends JSApp {
  val organization = "vaadin"

  val repos = Seq("vaadin-combo-box", "framework", "designer", "vaadin-grid",
    "vaadin-context-menu", "vaadin-combo-box", "vaadin-grid", "vaadin-context-menu",
    "vaadin-split-layout", "vaadin-date-picker", "vaadin-upload", "vaadin-input",
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

        showReleases(result.credential.accessToken.toString)
      } else {
        // No user, authorize
        val provider = new GithubAuthProvider()
        provider.addScope("repo")

        Firebase.auth().signInWithRedirect(provider)
      }
    } `catch` { error =>
      println(s"Error: ${error.getMessage}")
    }
  }

  private lazy val releaseTable = dom.document.body.querySelector("#grid").asInstanceOf[js.Dynamic]

  private def showReleases(token: String): Unit = {
    val urls = repos map { repo =>
      dom.ext.Ajax.get(s"https://api.github.com/repos/$organization/$repo/releases?access_token=$token")
    }

    // Lift potentially failed XHRs into Failure's
    val lifted = urls.map(_.map(Success(_)).recover { case ex => Failure(ex) })

    // Convert the sequence of futures into a future of sequences.
    // Collect will only accept succeeded XHRs.
    val releases: Future[Seq[Seq[Release]]] = Future.sequence(lifted) map { _ collect {
        case Success(xhr) => // Parse the successful responses
          js.JSON.parse(xhr.responseText).asInstanceOf[js.Array[js.Dynamic]].toSeq map Release.apply
      }
    }

    // Trigger the XHRs and set up the callback to build the result table based on response
    releases onComplete {
      case Success(rrs: Seq[Seq[Release]]) =>
        releaseTable.items = (rrs.flatten sortBy { _.rawTime }).reverse.to[js.Array]

        dom.document.body.querySelector("#loading").classList.add("hidden") // Hide loading message

      case Failure(_) => // Just to make the compiler happy.
    }
  }
}