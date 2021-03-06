package net.opentsdb.proxy.netty

import org.jboss.netty.channel._
import org.slf4j.LoggerFactory
import org.jboss.netty.channel.group.DefaultChannelGroup
import java.nio.channels.ClosedChannelException
import java.io.IOException

class ConnectionManager extends SimpleChannelHandler {
  private final val logger = LoggerFactory.getLogger(classOf[ConnectionManager])
  private final val channels = new DefaultChannelGroup("all-channels")

  override def channelOpen(ctx: ChannelHandlerContext, e: ChannelStateEvent) = channels.add(e.getChannel)

  override def handleUpstream(ctx: ChannelHandlerContext, e: ChannelEvent) = {
    e match {
      case e: ChannelStateEvent => logger.info(e.toString)
      case _ =>
    }

    super.handleUpstream(ctx, e)
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) = {
    val cause = e.getCause
    val chan = ctx.getChannel

    cause match {
      case causeMatch: ClosedChannelException => logger.warn("Attempting to write to a closed channel " + chan)
      case causeMatch: IOException => {
        val message = cause.getMessage
        if ("Connection reset by peer".equals(message) || "Connection timed out".equals(message)) {
          // Do nothing. A client disconnecting isn't really our problem. Oh,
          // and I'm not kidding you, there's no better way to detect ECONNRESET
          // in Java. Like, people have been bitching about errno for years,
          // and Java managed to do something *far* worse. That's quite a feat.
        }
      }
      case _ => {
        logger.error("Unexpected exception from downstream for " + chan, cause)
        e.getChannel.close()
      }
    }
  }
}
