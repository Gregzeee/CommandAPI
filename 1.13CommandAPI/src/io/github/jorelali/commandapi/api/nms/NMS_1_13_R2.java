package io.github.jorelali.commandapi.api.nms;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Iterator;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_13_R2.CraftParticle;
import org.bukkit.craftbukkit.v1_13_R2.command.ProxiedNativeCommandSender;
import org.bukkit.craftbukkit.v1_13_R2.enchantments.CraftEnchantment;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_13_R2.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_13_R2.potion.CraftPotionEffectType;
import org.bukkit.craftbukkit.v1_13_R2.util.CraftChatMessage;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;

import io.github.jorelali.commandapi.api.FunctionWrapper;
import io.github.jorelali.commandapi.api.arguments.CustomProvidedArgument.SuggestionProviders;
import io.github.jorelali.commandapi.api.arguments.LocationArgument.LocationType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import net.minecraft.server.v1_13_R2.ArgumentChatComponent;
import net.minecraft.server.v1_13_R2.ArgumentChatFormat;
import net.minecraft.server.v1_13_R2.ArgumentEnchantment;
import net.minecraft.server.v1_13_R2.ArgumentItemStack;
import net.minecraft.server.v1_13_R2.ArgumentMobEffect;
import net.minecraft.server.v1_13_R2.ArgumentParticle;
import net.minecraft.server.v1_13_R2.ArgumentPosition;
import net.minecraft.server.v1_13_R2.ArgumentProfile;
import net.minecraft.server.v1_13_R2.ArgumentTag;
import net.minecraft.server.v1_13_R2.ArgumentVec3;
import net.minecraft.server.v1_13_R2.BlockPosition;
import net.minecraft.server.v1_13_R2.CommandListenerWrapper;
import net.minecraft.server.v1_13_R2.CompletionProviders;
import net.minecraft.server.v1_13_R2.CustomFunction;
import net.minecraft.server.v1_13_R2.CustomFunctionData;
import net.minecraft.server.v1_13_R2.Entity;
import net.minecraft.server.v1_13_R2.IChatBaseComponent.ChatSerializer;
import net.minecraft.server.v1_13_R2.ICompletionProvider;
import net.minecraft.server.v1_13_R2.MinecraftServer;
import net.minecraft.server.v1_13_R2.Vec3D;

@SuppressWarnings({"unchecked", "rawtypes"})
public class NMS_1_13_R2 implements NMS {
	
	private CommandListenerWrapper getCLW(CommandContext cmdCtx) {
		return (CommandListenerWrapper) cmdCtx.getSource();
	}

	@Override
	public ChatColor getChatColor(CommandContext cmdCtx, String str) {
		return CraftChatMessage.getColor(ArgumentChatFormat.a(cmdCtx, str));
	}

	@Override
	public BaseComponent[] getChatComponent(CommandContext cmdCtx, String str) {
		String resultantString = ChatSerializer.a(ArgumentChatComponent.a(cmdCtx, str));
		return ComponentSerializer.parse((String) resultantString);
	}

	@Override
	public Enchantment getEnchantment(CommandContext cmdCtx, String str) {
		return new CraftEnchantment(ArgumentEnchantment.a(cmdCtx, str));
	}

	@Override
	public ItemStack getItemStack(CommandContext cmdCtx, String str) throws CommandSyntaxException {
		return CraftItemStack.asBukkitCopy(ArgumentItemStack.a(cmdCtx, str).a(1, false));
	}

	@Override
	public Location getLocation(CommandContext cmdCtx, String str, LocationType locationType, CommandSender sender) throws CommandSyntaxException {
		switch(locationType) {
			case BLOCK_POSITION:
				BlockPosition blockPos = ArgumentPosition.a(cmdCtx, str);
				return new Location(getCommandSenderWorld(sender), blockPos.getX(), blockPos.getY(), blockPos.getZ());
			case PRECISE_POSITION:
				Vec3D vecPos = ArgumentVec3.a(cmdCtx, str);
				return new Location(getCommandSenderWorld(sender), vecPos.x, vecPos.y, vecPos.z);
		}
		return null;
	}

	@Override
	public Particle getParticle(CommandContext cmdCtx, String str) {
		return CraftParticle.toBukkit(ArgumentParticle.a(cmdCtx, str));
	}

	@Override
	public PotionEffectType getPotionEffect(CommandContext cmdCtx, String str) throws CommandSyntaxException {
		return new CraftPotionEffectType(ArgumentMobEffect.a(cmdCtx, str));
	}

	@Override
	public void createDispatcherFile(File file) {
		//TODO
		//getMethod(getNMSClass("CommandDispatcher"), "a", File.class).invoke(this.nmsCommandDispatcher, file);
	}

	@Override
	public SuggestionProvider getSuggestionProvider(SuggestionProviders provider) {
		switch(provider) {
			case FUNCTION:
				return (context, builder) -> {
					CustomFunctionData functionData = getCLW(context).getServer().getFunctionData();
					ICompletionProvider.a(functionData.g().a(), builder, "#");
					return ICompletionProvider.a(functionData.c().keySet(), builder);
				};
			case RECIPES:
				return CompletionProviders.b;
			case SOUNDS:
				return CompletionProviders.c;
			case ADVANCEMENTS:
				//TODO;
				//return CommandAdvancement.a;
				return null;
			case LOOT_TABLES:
				//TODO;
				return (context, builder) -> {
					getCLW(context).getServer().getLootTableRegistry();//.e
					return ICompletionProvider.a((Iterable) null, builder);
				};		
			default:
				return (context, builder) -> Suggestions.empty();
		}
	}

	@Override
	public FunctionWrapper[] getFunction(CommandContext cmdCtx, String str) throws CommandSyntaxException {
		Collection<CustomFunction> customFuncList = ArgumentTag.a(cmdCtx, str);
		
		FunctionWrapper[] result = new FunctionWrapper[customFuncList.size()];
		
		CustomFunctionData customFunctionData = getCLW(cmdCtx).getServer().getFunctionData();
		CommandListenerWrapper commandListenerWrapper = getCLW(cmdCtx).a().b(2);
		
		int count = 0;
		Iterator<CustomFunction> it = customFuncList.iterator();
		while(it.hasNext()) {
			CustomFunction customFunction = it.next();
			
			FunctionWrapper wrapper = new FunctionWrapper(customFunction.a().toString(), customFunctionData::a, customFunctionData, customFunction, commandListenerWrapper, e -> {
				return getCLW(cmdCtx).a(((CraftEntity) e).getHandle());
			});
			
			result[count] = wrapper;
			count++;
		}
		
		return result;
	}

	@Override
	public CommandSender getSenderForCommand(CommandContext cmdCtx) {
		CommandSender sender = getCLW(cmdCtx).getBukkitSender();
		
		Entity proxyEntity = getCLW(cmdCtx).f();
		if(proxyEntity != null) {
			CommandSender proxy = ((Entity) proxyEntity).getBukkitEntity();
			
			if(!proxy.equals(sender)) {
				sender = new ProxiedNativeCommandSender(getCLW(cmdCtx), sender, proxy);
			}
		}
		
		return sender;
	}

	@Override
	public Object getNMSCommandDispatcher(Object server) {
		return ((MinecraftServer) server).commandDispatcher;
	}

	@Override
	public CommandDispatcher getDispatcher(Object server) {
		return ((MinecraftServer) server).commandDispatcher.a();
	}

	@Override
	public CommandSender getCommandSenderForCLW(Object clw) {
		return ((CommandListenerWrapper) clw).getBukkitSender();
	}

	@Override
	public Player getPlayer(CommandContext cmdCtx, String str) throws CommandSyntaxException {
		Player target = Bukkit.getPlayer(((GameProfile) ArgumentProfile.a(cmdCtx, str).iterator().next()).getId());
		if(target == null) {
			throw ArgumentProfile.a.create();
		} else {
			return target;
		}
	}

}
