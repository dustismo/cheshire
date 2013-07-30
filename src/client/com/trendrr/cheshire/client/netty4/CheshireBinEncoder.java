/**
 * 
 */
package com.trendrr.cheshire.client.netty4;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.trendrr.oss.strest.models.ShardRequest;
import com.trendrr.oss.strest.models.StrestHeader;
import com.trendrr.oss.strest.models.StrestRequest;


/**
 * @author Dustin Norlander
 * @created Jul 29, 2013
 * 
 */
public class CheshireBinEncoder extends MessageToByteEncoder<StrestRequest> {

	protected static Log log = LogFactory.getLog(CheshireBinEncoder.class);

	/* (non-Javadoc)
	 * @see io.netty.handler.codec.MessageToByteEncoder#encode(io.netty.channel.ChannelHandlerContext, java.lang.Object, io.netty.buffer.ByteBuf)
	 */
	@Override 
	public void encode(ChannelHandlerContext ctx, StrestRequest request, ByteBuf out) throws Exception {
//		[partition (int16)] //set to -1 as default
//				[length][shard key (string)] 
//				[router table revision (int64)]
//
//				[length][txn_id (string)]
//				[txn_accept(int8)]
//				[method(int8)]
//				[length][uri (string)]
//				[param_encoding (int8)]
//				[params (array)]
//				[content_encoding (int8)]
//				[content_length (int32)][content (array)]
		ShardRequest shard = request.getShardRequest();
		if (shard == null) {
			out.writeShort((short)-1);
			out.writeShort((short)0);
			out.writeLong(0l);
		} else {
			out.writeShort((short)shard.getPartition());
			BinaryHelper.writeString(out, shard.getKey());
			out.writeLong(shard.getRevision());
		}
		
		BinaryHelper.writeString(out, request.getTxnId());
		out.writeByte(request.getTxnAccept().getBinary());
		out.writeByte(request.getMethod().getBinary());
		BinaryHelper.writeString(out, request.getUri());
		out.writeByte(StrestHeader.ParamEncoding.JSON.getBinary());
		if (request.getParams() != null) {
			BinaryHelper.writeString(out, request.getParams().toJSONString());
		} else {
			BinaryHelper.writeString(out, "");
		}
		
		out.writeByte(request.getContentEncoding().getBinary());
		out.writeInt(request.getContentLength());
		out.writeBytes(request.getContent(), request.getContentLength());
	}
}
