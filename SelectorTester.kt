

/**
 * Test Connection with Java's SocketChannel, ServerSocketChannel, Selector, ByteBuffer, ExecutorService, and Thread (Runnable).
 *
 * @author	BingLi224
 * @version	2018.10.14
 */

import java.nio.channels.Selector
import java.nio.channels.SocketChannel
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SelectionKey
import java.nio.ByteBuffer

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

import java.util.Random

import java.net.InetSocketAddress

class Server
{
	val selector : Selector
	val server : ServerSocketChannel

	init {
		println ( "Server :: init" )

		selector = Selector.open ( )
		server = ServerSocketChannel.open ( )
		server.bind ( InetSocketAddress ( 4567 ) )
		server.configureBlocking ( false )
		server.register ( selector, SelectionKey.OP_ACCEPT )

		val es = Executors.newFixedThreadPool ( 1 )
		es.submit (
			Runnable {
				while ( true )
				{
					println ( "Server :: Select.select ()..." )
					selector.select ( )
					val selected = selector.selectedKeys ( )
					selected
						.iterator ( )
						.forEachRemaining {
							if ( it.isAcceptable ( ) )
							{
								println ( "Server :: new client" )

								val newClient = ( it.channel ( ) as ServerSocketChannel ).accept ( )

								println ( "Server :: wakeup" )
								selector.wakeup ( )

								newClient.configureBlocking ( false )

								println ( "Server :: new client :: will register" )
								newClient.register ( selector, SelectionKey.OP_READ )
								println ( "Server :: new client :: registered" )
							}
							else if ( it.isReadable ( ) )
							{
								val client = it.channel ( )
								if ( client is SocketChannel )
								{
									while ( true )
									{
										val input = ByteBuffer.allocate ( 0xff )
										val len = client.read ( input )
										if ( len <= 0 )
											break

										input.position ( 0 )
										val buffer = ByteArray ( len )
										input.get ( buffer )

										println ( "Server :: in [${String ( buffer )}]" )
									}
								}
							}
						}
					selected.clear ( )
				} // while: true
			} // runnable
		) // executorService.submit
	}
}

class Client
{
	val socketChannel : SocketChannel

	init {
		println ( "Client :: init" )
		socketChannel = SocketChannel.open (
			InetSocketAddress ( "localhost", 4567 )
		)

		var msg = "Hello!"
		println ( "Client :: sending msg :: $msg" )
		socketChannel.write (
			ByteBuffer.allocate ( msg.length )
				.put ( msg.toByteArray ( ) )
				.position ( 0 ) as ByteBuffer
		)

		val rnd = Random ( )

		Thread {
			while ( true )
			{
				Thread.sleep ( 1000 )

				msg = "echo-" + rnd.nextInt ( )
				println ( "Client :: sending msg :: $msg" )
				socketChannel.write (
					ByteBuffer.allocate ( msg.length )
						.put ( msg.toByteArray ( ) )
						.position ( 0 ) as ByteBuffer
				)
			}
		}.start ( )
	}
}

fun main ( argv : Array <String> )
{
	Server ( )
	Client ( )
}
