package ro.stancalau.test.framework.docker;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import ro.stancalau.test.framework.util.PathUtils;

import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class FFmpegContainer extends GenericContainer<FFmpegContainer> {

    private static final String DEFAULT_IMAGE = "jrottenberg/ffmpeg:5-ubuntu";
    private static final String DEFAULT_RESOLUTION = "1280x720";
    private static final int DEFAULT_FRAMERATE = 30;
    private static final int DEFAULT_DURATION_SECONDS = 30;

    @Getter
    private final Network network;

    private FFmpegContainer(String imageName, Network network) {
        super(imageName);
        this.network = network;
    }

    public static FFmpegContainer createRtmpStream(
            String alias,
            Network network,
            String rtmpUrl,
            String streamKey,
            int durationSeconds,
            @Nullable String logDestinationPath) {
        return createRtmpStream(
            alias,
            network,
            rtmpUrl,
            streamKey,
            durationSeconds,
            DEFAULT_RESOLUTION,
            DEFAULT_FRAMERATE,
            logDestinationPath
        );
    }

    public static FFmpegContainer createRtmpStream(
            String alias,
            Network network,
            String rtmpUrl,
            String streamKey,
            int durationSeconds,
            String resolution,
            int framerate,
            @Nullable String logDestinationPath) {

        String logDirPath = (logDestinationPath != null)
            ? logDestinationPath
            : PathUtils.containerLogPath(PathUtils.currentScenarioPath(), "docker", alias);

        File logDirRoot = new File(logDirPath);
        logDirRoot.mkdirs();

        String fullRtmpUrl = rtmpUrl + "/" + streamKey;

        FFmpegContainer container = new FFmpegContainer(DEFAULT_IMAGE, network);

        container = ContainerLogUtils.withLogCapture(container, logDirRoot, "ffmpeg.log");

        List<String> command = buildRtmpCommand(fullRtmpUrl, resolution, framerate, durationSeconds);

        container = container
            .withNetwork(network)
            .withNetworkAliases(alias)
            .withCommand(command.toArray(new String[0]));

        log.info("Created FFmpeg container {} streaming to {} for {} seconds at {} {}fps",
            alias, fullRtmpUrl, durationSeconds, resolution, framerate);

        return container;
    }

    public static FFmpegContainer createRtmpStreamWithWrongKey(
            String alias,
            Network network,
            String rtmpUrl,
            int durationSeconds,
            @Nullable String logDestinationPath) {
        return createRtmpStream(
            alias,
            network,
            rtmpUrl,
            "invalid-stream-key",
            durationSeconds,
            logDestinationPath
        );
    }

    private static List<String> buildRtmpCommand(
            String rtmpUrl,
            String resolution,
            int framerate,
            int durationSeconds) {
        List<String> command = new ArrayList<>();
        command.add("-re");
        command.add("-f");
        command.add("lavfi");
        command.add("-i");
        command.add(String.format("testsrc=size=%s:rate=%d", resolution, framerate));
        command.add("-f");
        command.add("lavfi");
        command.add("-i");
        command.add("sine=frequency=1000:sample_rate=48000");
        command.add("-c:v");
        command.add("libx264");
        command.add("-preset");
        command.add("ultrafast");
        command.add("-tune");
        command.add("zerolatency");
        command.add("-b:v");
        command.add("2500k");
        command.add("-maxrate");
        command.add("2500k");
        command.add("-bufsize");
        command.add("5000k");
        command.add("-g");
        command.add(String.valueOf(framerate * 2));
        command.add("-c:a");
        command.add("aac");
        command.add("-b:a");
        command.add("128k");
        command.add("-ar");
        command.add("48000");
        command.add("-f");
        command.add("flv");
        command.add("-t");
        command.add(String.valueOf(durationSeconds));
        command.add(rtmpUrl);

        return command;
    }
}
