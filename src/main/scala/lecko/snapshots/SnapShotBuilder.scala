package lecko.snapshots

import java.io.{File, PrintWriter}

import akka.actor.{ActorSystem, Terminated, Props, Actor}
import akka.routing.{Router, RoundRobinRoutingLogic, ActorRefRoutee}
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.HtmlPage

import scala.xml.XML


object SnapShotBuilder extends App {

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
          val xml: String = page.asXml().replaceAllLiterally("<script", "<st").replaceAllLiterally("</script>","</st>")
          p.write(xml)
          p.close()
        }
      }

  }
}

case class CreatePage(url:String)