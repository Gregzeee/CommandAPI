package dev.jorel.commandapi.nms;

import dev.jorel.commandapi.preprocessor.RequireField;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.chat.ChatPreviewCache;
import net.minecraft.network.chat.ChatPreviewThrottler;
import net.minecraft.network.protocol.game.ClientboundChatPreviewPacket;
import net.minecraft.network.protocol.game.ServerboundChatPreviewPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.util.concurrent.CompletableFuture;

@RequireField(in = ServerGamePacketListenerImpl.class, name = "chatPreviewCache", ofType = ChatPreviewCache.class)
@RequireField(in = ServerGamePacketListenerImpl.class, name = "chatPreviewThrottler", ofType = ChatPreviewThrottler.class)
public class NMS_1_19_1_R1_ChatPreviewHandler extends NMS_1_19_Common_ChatPreviewHandler {
	ChatPreviewThrottler throttler;

	public NMS_1_19_1_R1_ChatPreviewHandler(NMS<CommandSourceStack> nms, Plugin plugin, Player player) {
		super(nms, plugin, player);

		try {
			Field f = ServerGamePacketListenerImpl.class.getDeclaredField("M");
			f.setAccessible(true);
			throttler = (ChatPreviewThrottler) f.get(((CraftPlayer) player).getHandle().connection);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void handleChatPreviewPacket(ServerboundChatPreviewPacket chatPreview) {
		// We want to run this synchronously, just in case there's some funky async stuff going on here
		throttler.schedule(() -> {
			int i = chatPreview.queryId();
			CompletableFuture<net.minecraft.network.chat.Component> result = new CompletableFuture<>();

			// get preview
			Bukkit.getScheduler().runTask(this.plugin, () -> result.complete(parseChatPreviewQuery(chatPreview.query())));

			// update player's ChatPreviewCache
			result.thenAcceptAsync(component -> {
				try {
					Field f = ServerGamePacketListenerImpl.class.getDeclaredField("L");
					f.setAccessible(true);
					ChatPreviewCache c = (ChatPreviewCache) f.get(((CraftPlayer) player).getHandle().connection);
					c.set(chatPreview.query().substring(1), component);
				} catch (Exception e) {
					e.printStackTrace();
				}
			});

			// send ChatPreviewPacket
			return result.thenAccept(
				component -> connection.send(
					new ClientboundChatPreviewPacket(i, component),
					PacketSendListener.exceptionallySend(() -> new ClientboundChatPreviewPacket(i, null))
				)
			);
		});
	}
}