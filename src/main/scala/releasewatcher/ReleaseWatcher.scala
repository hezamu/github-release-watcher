package releasewatcher

import org.scalajs.dom
import releasewatcher.firebase.{Firebase, User}

import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.JSApp
import scala.util.{Failure, Success}
import scalatags.JsDom.all._
import scala.concurrent.ExecutionContext.Implicits.global

case class Release(repo: String, tag: String, time: String) {
  def formattedTime: String = {
    val cs = time.split("-")
    s"${cs(2) take 2}.${cs(1)}.${cs(0)}"
  }
}

object Release {
  def apply(json: js.Dynamic): Release = {
    val repo = json.url.toString.split("/")(5)
    Release(repo, json.tag_name.toString, json.published_at.toString)
  }
}

case class Pair(a: Int, b: Int) {
  def next = Pair(b, a + b)
}

object App extends JSApp {
  def main(): Unit = {
    println("Initing app")

    Firebase.auth().onAuthStateChanged{
      _ map { user: User =>
        println(s"${user.uid} logged in: anon: ${user.isAnonymous}")
        doTheThing()
        user
      } getOrElse println("No user :(")
    }

    Firebase.auth().signInAnonymously() `catch` { error =>
      println(s"Sign in error: ${error.getMessage}")
    }
  }

  private def doTheThing(): Unit = {
    println("Doing the thing")

    //      dom.document.head.appendChild(style(Styles.styleSheetText).render)
    //      dom.document.body.applyTags(Styles.body)

    val repos = Seq("vaadin-combo-box", "framework")
//      "vaadin-grid", "vaadin-context-menu",
//      "vaadin-split-layout", "vaadin-date-picker", "vaadin-upload", "vaadin-input",
//      "vaadin-form-layout", "framework", "charts", "board", "cdi", "vaadin-charts",
//      "spring", "testbench", "spreadsheet", "designer"
//    )

    val fs = repos map { repo =>
      dom.ext.Ajax.get(s"https://api.github.com/repos/vaadin/$repo/releases")
    }

    // Lift potentially failed XHRs into Failure's
    val lifted = fs.map(_.map(Success(_)).recover{ case ex => Failure(ex) })

    // Convert the sequence of futures into a future of sequences.
    // Collect will only accept succeeded XHRs.
    val releases: Future[Seq[Seq[Release]]] = Future.sequence(lifted) map {
      _ collect {
        case Success(xhr) => // Parse the successful responses
          js.JSON.parse(xhr.responseText).asInstanceOf[js.Array[js.Dynamic]].toSeq map Release.apply
      }
    }

    // Trigger the XHRs and set up the callback to build the page based on responses
    releases onComplete {
      case Success(rrs: Seq[Seq[Release]]) =>
        val sorted: Seq[Release] = rrs.flatten sortBy {
          _.time
        } reverse

        dom.document.body.appendChild(table(Styles.table,
          thead(Styles.thead, td("Product"), td("Release"), td("Published")),

          for(r <- sorted) yield tr(td(r.repo), td(r.tag), td(r.formattedTime))
        ).render)

      case Failure(_) => // Just to make the compiler happy.
    }
  }
}
/*
    <iron-ajax url="https://api.github.com/repos/vaadin/[[repo]]/releases" last-response="{{releases}}" auto></iron-ajax>
    <vaadin-grid items=[[data]]>
      <vaadin-grid-column flex-grow="0" width="150px">
        <template class="header">Element</template>
        <template>[[_elementName(item.url)]]</template>
      </vaadin-grid-column>
      <vaadin-grid-column>
        <template class="header">Release</template>
        <template><a href="[[item.html_url]]" target="_blank">[[item.name]]</a></template>
      </vaadin-grid-column>
      <vaadin-grid-column>
        <template class="header">Published</template>
        <template>[[item.published_at]]</template>
      </vaadin-grid-column>
    </vaadin-grid>
  </template>
  <script>
    addEventListener('WebComponentsReady', function() {
      Polymer({
        is: 'vaadin-releases',

        observers: ['_releasesChanged(releases)'],

        ready: function() {
          this.repos = ['vaadin-combo-box', 'vaadin-grid', 'vaadin-context-menu', 'vaadin-split-layout', 'vaadin-date-picker', 'vaadin-upload', 'vaadin-input', 'vaadin-form-layout', 'vaadin-button', 'framework', 'spring', 'testbench', 'charts', 'spreadsheet', 'board', 'vaadin-icons', 'vaadin-button', 'vaadin-text-field', 'vaadin-charts', 'vaadin-form-layout', 'vaadin-button', 'maven-plugin', 'cdi', 'eclipse-plugin'];

          this.repo = this.repos.pop();
        },

        _releasesChanged: function(releases) {
          this._data = this._data || [];
          this._data = this._data.concat(releases);

          this.debounce('fetching', function() {
            this._data.sort(function(a,b) {
              return a.published_at < b.published_at ? 1 : -1;
            });

						this.data = this._data;
          }, 500);

          if (this.repos.length > 0) {
	          this.repo = this.repos.pop();
          }
        },

        _elementName: function(url) {
					return url ? url.split('/')[5] : '';
        }
      });
    });
  </script>
</dom-module>
 */