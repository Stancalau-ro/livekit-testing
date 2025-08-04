package ro.stancalau.test.framework.docker;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.OutputFrame;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.function.Consumer;

@Slf4j
@UtilityClass
public class ContainerLogUtils {

    /**
     * Creates a log consumer that writes container output to a specified log file.
     * This method provides a unified approach for capturing stdout/stderr from containers
     * and persisting them to the test artifact directory structure.
     *
     * @param logDirectory The directory where the log file should be created
     * @param logFileName The name of the log file (e.g., "livekit.log", "egress.log")
     * @return A consumer that can be used with GenericContainer.withLogConsumer()
     */
    public static Consumer<OutputFrame> createLogConsumer(File logDirectory, String logFileName) {
        return outputFrame -> {
            try {
                File logFile = new File(logDirectory, logFileName);
                Files.write(logFile.toPath(), 
                    outputFrame.getUtf8String().getBytes(), 
                    StandardOpenOption.CREATE, 
                    StandardOpenOption.APPEND);
            } catch (Exception e) {
                log.warn("Failed to write {} log: {}", logFileName, e.getMessage());
            }
        };
    }

    /**
     * Adds log capturing to any GenericContainer using the standard approach.
     * This ensures consistent log preservation across all container types.
     *
     * @param container The container to add log capturing to
     * @param logDirectory The directory where logs should be saved
     * @param logFileName The name of the log file
     * @param <T> The container type
     * @return The container with log capturing enabled
     */
    public static <T extends GenericContainer<T>> T withLogCapture(T container, File logDirectory, String logFileName) {
        return container.withLogConsumer(createLogConsumer(logDirectory, logFileName));
    }
}