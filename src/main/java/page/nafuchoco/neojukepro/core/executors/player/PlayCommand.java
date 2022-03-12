/*
 * Copyright 2020 NAFU_at.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package page.nafuchoco.neojukepro.core.executors.player;

import lombok.val;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jetbrains.annotations.Nullable;
import page.nafuchoco.neojukepro.core.Main;
import page.nafuchoco.neojukepro.core.MessageManager;
import page.nafuchoco.neojukepro.core.command.CommandContext;
import page.nafuchoco.neojukepro.core.command.CommandExecutor;
import page.nafuchoco.neojukepro.core.command.CommandValueOption;
import page.nafuchoco.neojukepro.core.http.youtube.SearchItem;
import page.nafuchoco.neojukepro.core.http.youtube.YouTubeAPIClient;
import page.nafuchoco.neojukepro.core.http.youtube.YouTubeSearchResults;
import page.nafuchoco.neojukepro.core.player.AudioTrackLoader;
import page.nafuchoco.neojukepro.core.player.NeoGuildPlayer;
import page.nafuchoco.neojukepro.core.player.TrackContext;
import page.nafuchoco.neojukepro.core.utils.ChannelPermissionUtil;
import page.nafuchoco.neojukepro.core.utils.ExceptionUtil;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Pattern;

public class PlayCommand extends CommandExecutor {
    private static final Pattern URL_REGEX = Pattern.compile("^(http|https)://([\\w-]+\\.)+[\\w-]+(/[\\w-./?%&=]*)?$");
    private static final Pattern NUMBER_REGEX = Pattern.compile("^[1-5]$");

    private static final YouTubeAPIClient YOUTUBE_CLIENT;

    static {
        YouTubeAPIClient apiClient;
        try {
            apiClient = new YouTubeAPIClient(Main.getLauncher().getConfig().getAdvancedConfig().getGoogleAPIToken());
        } catch (IllegalArgumentException e) {
            apiClient = null;
        }
        YOUTUBE_CLIENT = apiClient;
    }

    public PlayCommand(String name, String... aliases) {
        super(name, aliases);

        getOptions().add(new CommandValueOption(OptionType.STRING,
                "url",
                "URL of track to play",
                false,
                false));
        getOptions().add(new CommandValueOption(OptionType.STRING,
                "search",
                "Search by the specified keyword.",
                false,
                false));
    }

    @Override
    public void onInvoke(CommandContext context) {
        NeoGuildPlayer audioPlayer = context.getNeoGuild().getAudioPlayer();
        if (!context.getNeoGuild().getJDAGuild().getSelfMember().getVoiceState().inAudioChannel()) {
            VoiceChannel targetChannel = null;
            if (context.getInvoker().getJDAMember().getVoiceState().getChannel().getType() == ChannelType.VOICE)
                targetChannel = (VoiceChannel) context.getInvoker().getJDAMember().getVoiceState().getChannel();

            if (targetChannel == null) {
                context.getResponseSender().sendMessage(MessageManager.getMessage(
                        context.getNeoGuild().getSettings().getLang(),
                        "command.join.before")).queue();
            }
            if (!ChannelPermissionUtil.checkAccessVoiceChannel(targetChannel, context.getNeoGuild().getJDAGuild().getSelfMember())) {
                context.getResponseSender().sendMessage(MessageManager.getMessage(
                        context.getNeoGuild().getSettings().getLang(),
                        "command.channel.permission")).queue();
            }
            try {
                audioPlayer.joinChannel(targetChannel);
            } catch (InsufficientPermissionException e) {
                context.getResponseSender().sendMessage(MessageManager.getMessage(
                        context.getNeoGuild().getSettings().getLang(),
                        "command.channel.permission")).queue();
            }
        }
        audioPlayer.play();


        // TODO: 2022/03/12 添付ファイルの再生機能の実装

        val url = (String) context.getOptions().get("url").getValue();
        if (URL_REGEX.matcher(url).find()) { // 指定された引数がURLの場合はURLのトラックを再生
            audioPlayer.play(new AudioTrackLoader(new TrackContext(context.getNeoGuild(), context.getInvoker(), 0, url)));
        } else {
            File file = new File(url);
            if (file.exists()) { // 入力された引数がファイルパスかを確認しファイルが存在する場合再生
                // TODO: 2022/03/12 ファイルがあるかどうかが分かってしまうのでこのあたりの判定をどうにかする
                audioPlayer.play(new AudioTrackLoader(new TrackContext(context.getNeoGuild(), context.getInvoker(), 0, file.getPath())));
            } else if (YOUTUBE_CLIENT == null) {
                context.getResponseSender().sendMessage(MessageManager.getMessage(
                        context.getNeoGuild().getSettings().getLang(),
                        "command.play.search.disabled")).queue();
            } else {
                try {
                    YouTubeSearchResults result =
                            new YouTubeAPIClient(context.getNeoJukePro().getConfig().getAdvancedConfig().getGoogleAPIToken()).searchVideos(url);
                    if (result == null || result.getItems().length == 0) {
                        context.getResponseSender().sendMessage(MessageManager.getMessage(
                                context.getNeoGuild().getSettings().getLang(),
                                "command.play.search.notfound")).queue();
                    }

                    StringBuilder message = new StringBuilder();
                    message.append(MessageManager.getMessage(
                            context.getNeoGuild().getSettings().getLang(),
                            "command.play.search.found"));
                    int count = 1;
                    for (SearchItem item : result.getItems()) {
                        message.append("\n`[" + count + "]` " + item.getSnippet().getTitle() + "");
                        count++;
                    }
                    message.append("\n\n" + MessageManager.getMessage(
                            context.getNeoGuild().getSettings().getLang(),
                            "command.play.search.select"));

                    context.getNeoGuild().getGuildTempRegistry().registerTemp(
                            "searchResults", Arrays.asList(result, url));
                    context.getResponseSender().sendMessage(message.toString()).queue();
                } catch (IOException e) {
                    ExceptionUtil.sendStackTrace(
                            context.getNeoGuild(),
                            e,
                            MessageManager.getMessage(
                                    context.getNeoGuild().getSettings().getLang(),
                                    "command.play.search.failed"));
                }
            }
        }
    }

    @Override
    public String getDescription() {
        return "Play for tracks.";
    }

    @Override
    public int getRequiredPerm() {
        return 0;
    }
}
