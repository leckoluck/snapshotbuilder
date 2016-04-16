package lecko.snapshots

import java.io.{File, PrintWriter}

import akka.actor.{ActorSystem, Terminated, Props, Actor}
import akka.routing.{Router, RoundRobinRoutingLogic, ActorRefRoutee}
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.HtmlPage

import scala.xml.XML


object SnapshotBuilder extends App {

  val sitemap = System.getProperty("sitemap.path", "/tmp/sitemap.xml")

  val file = new File (sitemap)

  val xml = XML.loadFile(file)

  val system = ActorSystem.create("PiSystem")

  val master = system.actorOf(Props[Master])

  val s = (xml \\ "loc").toList.foreach {
    l =>
      val url: String = l.text

      master ! CreatePage(url)

  }

}

class Master extends Actor {
  var router = {
    val routees = Vector.fill(20) {
      val r = context.actorOf(Props[Worker])
      context watch r
      ActorRefRoutee(r)
    }
    Router(RoundRobinRoutingLogic(), routees)
  }

  def receive = {
    case w: CreatePage =>
      router.route(w, sender())
    case Terminated(a) =>
      router = router.removeRoutee(a)
      val r = context.actorOf(Props[Worker])
      context watch r
      router = router.addRoutee(r)
  }
}



case class Worker() extends Actor {
  val startPath = "http://www.mitresarroyos.com/#!"
  val scapedPath = System.getProperty("scaped.path", "/tmp/snapshots")

  override def receive = {
    case CreatePage(url) =>
      val newFile = new File(url.replace(startPath, scapedPath) + ".html")
      if (!newFile.exists()) {
        val  webClient = new WebClient()
        val page:HtmlPage = webClient.getPage(url)
        println(s"getting $url")
        webClient.waitForBackgroundJavaScriptStartingBefore(3000)
        newFile.getParentFile.mkdirs()
        println(s"writing ${newFile.getAbsolutePath}")
        Some(new PrintWriter(newFile)).foreach{p =>

          def replaceAll(list:List[String], content:String):String = {
            list match {
              case x::xs => replaceAll(xs, content.replaceAllLiterally(x, "<script>"))
              case Nil => content
            }
          }
          val xml: String = replaceAll(scriptToReplace, page.asXml()) //.replaceAllLiterally("<script", "<st").replaceAllLiterally("</script>","</st>")
          p.write(xml)
          p.close()
        }
      }

  }

  val scriptToReplace = List("<script id=\"twitter-wjs\" src=\"http://platform.twitter.com/widgets.js\">",
  "<script src=\"./bower_components/angular/angular.js\">",
  "<script src=\"./bower_components/angular-animate/angular-animate.js\">",
  "<script src=\"./bower_components/angular-ui-router/release/angular-ui-router.min.js\">",
  "<script src=\"app2.js\">", "<script src=\"app.js\">","<script src=\"search.js\">","<script src=\"login/login.js\">",
  "<script src=\"admin/admin.js\">", "<script src=\"new/new.js\">", "<script src=\"hashBang/hashBang.js\">"
  )
}

case class CreatePage(url:String)


