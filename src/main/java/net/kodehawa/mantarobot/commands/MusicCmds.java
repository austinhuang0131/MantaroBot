package net.kodehawa.mantarobot.commands;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.managers.AudioManager;
import net.kodehawa.mantarobot.commands.music.AudioCmdUtils;
import net.kodehawa.mantarobot.commands.music.MantaroAudioManager;
import net.kodehawa.mantarobot.commands.music.MusicManager;
import net.kodehawa.mantarobot.commands.currency.InventoryResolver;
import net.kodehawa.mantarobot.modules.Category;
import net.kodehawa.mantarobot.modules.CommandPermission;
import net.kodehawa.mantarobot.modules.Module;
import net.kodehawa.mantarobot.modules.SimpleCommand;
import net.kodehawa.mantarobot.utils.Utils;

import java.net.URL;

import static net.kodehawa.mantarobot.commands.music.AudioCmdUtils.embedForQueue;
import static net.kodehawa.mantarobot.commands.music.MantaroAudioManager.*;

public class MusicCmds extends Module {
	public MusicCmds() {
		super(Category.MUSIC);
		//Audio intensifies.
		np();
		pause();
		play();
		queue();
		removetrack();
		shuffle();
		skip();
		stop();
		volume();
		repeat();
		move();
	}

	private void move() {
		super.register("move", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				if(content.isEmpty()){
					event.getChannel().sendMessage("Cannot move to no channel, remember to write the name.").queue();
					onHelp(event);
					return;
				}

				try {
					VoiceChannel vc = event.getGuild().getVoiceChannelsByName(content, true).get(0);
					AudioManager am = event.getGuild().getAudioManager();

					AudioCmdUtils.closeAudioConnection(event, am);
					AudioCmdUtils.openAudioConnection(event, am, vc);
					event.getChannel().sendMessage(":ok_hand: Moved bot to VC: ``" + vc.getName() + "``").queue();
				} catch (IndexOutOfBoundsException e) {
					event.getChannel().sendMessage("Voice Channel not found or you didn't specify any voice channel.").queue();
				}
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "Move command")
					.setDescription("Moves the bot from one VC to another")
					.addField("Usage", "~>move <vc>", false)
					.addField("Parameters", "vc: voice channel to move the bot to (exact name, caps doesn't matter).", false)
					.build();
			}
		});
	}

	private void np() {
		super.register("np", new SimpleCommand() {
			@Override
			public void call(String[] args, String content, GuildMessageReceivedEvent event) {
				MusicManager musicManager = getGuildAudioPlayer(event);
				if (musicManager.getScheduler().getPlayer().getPlayingTrack() == null) {
					event.getChannel().sendMessage("There is no track playing or we cannot seem to find it, maybe try playing a song?").queue();
					return;
				}

				event.getChannel().sendMessage(String.format("\uD83D\uDCE3 Now playing ->``%s (%s)``", musicManager.getScheduler().getPlayer().getPlayingTrack().getInfo().title, Utils.getDurationMinutes(musicManager.getScheduler().getPlayer().getPlayingTrack().getInfo().length))).queue();
				InventoryResolver.dropWithChance(event.getChannel(), 0, 40);
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "NowPlaying (np) Command")
					.addField("Description", "Returns what track is playing now.", false).build();
			}

			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.USER;
			}
		});
	}

	private void pause() {
		super.register("pause", new SimpleCommand() {
			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "Pause Command")
					.addField("Description", "Pauses or unpauses the current track.", false)
					.addField("Usage:", "~>pause (if paused will unpause and viseversa)", false).build();
			}

			@Override
			public void call(String[] args, String content, GuildMessageReceivedEvent event) {
				MusicManager musicManager = getGuildAudioPlayer(event);
				boolean paused = !musicManager.getScheduler().getPlayer().isPaused();
				String toSend = paused ? ":mega: Player paused." : ":mega: Player unpaused.";
				musicManager.getScheduler().getPlayer().setPaused(paused);
				event.getChannel().sendMessage(toSend).queue();
				InventoryResolver.dropWithChance(event.getChannel(), 0, 40);
			}

			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.USER;
			}
		});
	}

	private void play() {
		super.register("play", new SimpleCommand() {
			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.USER;
			}

			@Override
			public void call(String[] args, String content, GuildMessageReceivedEvent event) {
				if (content.trim().isEmpty()) {
					onHelp(event);
					return;
				}

				try {
					new URL(content);
				} catch (Exception e) {
					content = "ytsearch: " + content;
				}

				MantaroAudioManager.loadAndPlay(event, content);
				InventoryResolver.dropWithChance(event.getChannel(), 0, 40);
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "Play Command")
					.addField("Description", "Plays a song in the music voice channel.", false)
					.addField("Usage:", "~>play <song url> (Can be a YouTube song, a playlist or a search)", false).build();
			}

		});
	}

	public void queue() {
		super.register("queue", new SimpleCommand() {
			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "Queue Command")
					.addField("Description", "Returns the current queue playing on the server or clears it.", false)
					.addField("Usage:", "~>queue\n~>queue clear", false).build();
			}

			@Override
			public void call(String[] args, String content, GuildMessageReceivedEvent event) {
				MusicManager musicManager = getGuildAudioPlayer(event);
				if (content.isEmpty()) {
					event.getChannel().sendMessage(embedForQueue(event.getGuild(), musicManager)).queue();
				} else if (content.startsWith("clear")) {
					MantaroAudioManager.clearQueue(musicManager, event, true);
				}
				InventoryResolver.dropWithChance(event.getChannel(), 0, 50);
			}

			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.USER;
			}
		});
	}

	private void removetrack() {
		super.register("removetrack", new SimpleCommand() {
			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "RemoveTrack Command")
					.addField("Description", "Removes the specified track from the queue.", false)
					.addField("Usage:", "~>removetrack <tracknumber/first/next/last> (as specified on the ~>queue command)", false)
					.addField("Parameters:", "tracknumber: the number of the track to remove\n" +
							"first: remove first track\n"
							+ "next: remove next track\n"
							+ "last: remove last track", false)
					.build();
			}

			@Override
			public void call(String[] args, String content, GuildMessageReceivedEvent event) {
				getGuildAudioPlayer(event).getScheduler().getQueueAsList(list -> {
					int i;
					try {
						switch (content) {
							case "first":
							case "next":
								i = 0;
								break;
							case "last":
								i = list.size() - 1;
								break;
							default:
								i = Integer.parseInt(content) - 1;
								break;
						}
					} catch (NumberFormatException ex) {
						event.getChannel().sendMessage(":heavy_multiplication_x: That's not a number.").queue();
						return;
					}

					if (i >= list.size()) {
						event.getChannel().sendMessage(":heavy_multiplication_x: I don't have a music that corresponds to the number.").queue();
						return;
					}

					event.getChannel().sendMessage(":ok_hand: Removed music **" + list.remove(i).getInfo().title + "** from the queue.").queue();
					InventoryResolver.dropWithChance(event.getChannel(), 0, 40);
				});
			}

			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.USER;
			}
		});
	}

	private void repeat() {
		super.register("repeat", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				MusicManager musicManager = getGuildAudioPlayer(event);
				if (musicManager.getScheduler().getPlayer().getPlayingTrack() != null) {
					boolean repeat = !musicManager.getScheduler().isRepeat();
					String toSend = repeat ? ":mega: Repeating current song." : ":mega: Continuing with normal queue.";
					musicManager.getScheduler().setRepeat(repeat);
					event.getChannel().sendMessage(toSend).queue();
					InventoryResolver.dropWithChance(event.getChannel(), 0, 70);
					return;
				}

				event.getChannel().sendMessage(":heavy_multiplication_x: Cannot repeat a non-existant track.").queue();
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "Repeat command")
					.setDescription("Repeats a song.")
					.addField("Usage", "~>repeat (if it's not repeating, start repeating and viseversa)", false)
					.addField("Warning", "Might not work correctly, if the bot leaves the voice channel after disabling repeat, just add a song to the queue", true)
					.build();
			}
		});
	}

	private void shuffle() {
		super.register("shuffle", new SimpleCommand() {
			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "Shuffle Command")
					.addField("Description", "Shuffles the current queue.", false).build();
			}

			@Override
			public void call(String[] args, String content, GuildMessageReceivedEvent event) {
				getGuildAudioPlayer(event).shuffle();
				event.getChannel().sendMessage("\uD83D\uDCE3 Randomized current queue order.").queue();
				InventoryResolver.dropWithChance(event.getChannel(), 0, 70);
			}

			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.USER;
			}
		});
	}

	private void skip() {
		super.register("skip", new SimpleCommand() {
			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "Skip Command")
					.addField("Description", "Stops the track and continues to the next one, if there is one.", false).build();
			}

			@Override
			public void call(String[] args, String content, GuildMessageReceivedEvent event) {
				getGuildAudioPlayer(event).skipTrack(event);
				InventoryResolver.dropWithChance(event.getChannel(), 0, 50);
			}

			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.USER;
			}
		});
	}

	private void stop() {
		super.register("stop", new SimpleCommand() {
			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "Stop Command")
					.addField("Description", "Clears the queue and leaves the voice channel.", false).build();
			}

			@Override
			public void call(String[] args, String content, GuildMessageReceivedEvent event) {
				MusicManager musicManager = getGuildAudioPlayer(event);
				if(musicManager.getScheduler().getPlayer().getPlayingTrack() != null && !musicManager.getScheduler().getPlayer().isPaused())
					musicManager.getScheduler().getPlayer().getPlayingTrack().stop();
				clearQueue(musicManager, event, false);
				closeConnection(musicManager, event.getGuild().getAudioManager(), event.getChannel());
				InventoryResolver.dropWithChance(event.getChannel(), 0, 30);
			}

			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.USER;
			}
		});
	}

	private void volume() {
		super.register("volume", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				AudioPlayer player = getGuildAudioPlayer(event).getScheduler().getPlayer();

				if (args[0].equals("check")) {
					event.getChannel().sendMessage("The current volume in this session is: " + player.getVolume()).queue();
					return;
				}

				int volume;
				try {
					volume = Math.max(0, Math.min(100, Integer.parseInt(args[0])));
				} catch (Exception e) {
					event.getChannel().sendMessage(":heavy_multiplication_x: Not a valid integer.").queue();
					return;
				}
				player.setVolume(volume);
				event.getChannel().sendMessage(String.format(":ok_hand: Volume set to %d", volume)).queue();
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "Volume command")
					.addField("Usage", "~>volume <number>", false)
					.addField("Parameters", "number: Integer number from 1 to 99", false)
					.addField("Notice", "To check the current volume do ~>volume check", false)
					.build();
			}
		});
	}
}