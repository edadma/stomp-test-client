package xyz.hyperreal.stomp_test_client

import typings.node.{nodeStrings, process}
import typings.sockjsDashClient.sockjsDashClientMod
import typings.stompjs.stompjsMod
import typings.stompjs.stompjsMod.Message

import scala.scalajs.js


object Main extends App {

  val serverHostname = "192.168.0.117"
  val serverPort = 15674
  val serverPath = "/stomp"

  val client = stompjsMod.over( new sockjsDashClientMod.^(s"http://$serverHostname:$serverPort$serverPath") )

  val sendRegex = """([a-z0-9-]*):\s*(.*)"""r
  val subscribeRegex = """\+(.+)"""r
  val unsubscribeRegex = """\-(.+)"""r

  var current: String = null

  client.connect(js.Dynamic.literal(), frame =>
    frame.asInstanceOf[js.Dictionary[String]]("command") match {
      case "CONNECTED" =>
        println( s"connected to ${frame.asInstanceOf[js.Dictionary[Any]]("headers").asInstanceOf[js.Dictionary[String]]("server")}\n" )
        println(
          """
            |type a command
            |  <queue>:<message>    if <queue> is empty, the previous queue that was messaged will be used as destination
            |  +<queue>             subscribe to <queue>
            |  -<queue>             unsubscribe from <queue>
            |""".stripMargin
        )
      case _ => println( js.JSON.stringify(frame) )
    } )

  process.stdin.setEncoding( "utf-8" )

  process.stdin.on_data( nodeStrings.data, line =>
    line.asInstanceOf[String].trim match {
      case sendRegex( queue, message ) =>
        val dest = if (queue isEmpty) current else queue

        if (dest eq null)
          println( "specify queue" )
        else {
          client.send( dest, js.Dynamic.literal(), message )
          current = dest
        }
      case subscribeRegex( queue ) =>
        println( s"subscribing to '$queue'" )
        current = queue
        client.subscribe( queue, (message: Message) =>
          message.command match {
            case "MESSAGE" =>
              println( s"($queue) ${message.body}" )
            case _ =>
              println( message )
          } )
      case command => println( s"unrecognized command '$command'" )
    } )

}