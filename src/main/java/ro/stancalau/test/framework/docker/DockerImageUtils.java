package ro.stancalau.test.framework.docker;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@UtilityClass
public class DockerImageUtils {

    public static void ensureDockerImageExists(String fullImageName, String baseDirectory) {
        if (!dockerImageExists(fullImageName)) {
            log.info("Docker image {} not found locally. Building from source...", fullImageName);
            String version = extractVersionFromImageName(fullImageName);
            buildDockerImage(fullImageName, version, baseDirectory);
        } else {
            log.info("Docker image {} already exists locally", fullImageName);
        }
    }

    private static String extractVersionFromImageName(String fullImageName) {
        if (fullImageName == null || !fullImageName.contains(":")) {
            throw new IllegalArgumentException("Invalid image name format. Expected format: name:tag");
        }
        
        int colonIndex = fullImageName.lastIndexOf(':');
        return fullImageName.substring(colonIndex + 1);
    }

    public static boolean dockerImageExists(String imageName) {
        try {
            ProcessBuilder pb = new ProcessBuilder("docker", "images", "--format", "{{.Repository}}:{{.Tag}}", imageName);
            Process process = pb.start();
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                int exitCode = process.waitFor();
                return exitCode == 0 && line != null && !line.trim().isEmpty();
            }
        } catch (IOException | InterruptedException e) {
            log.error("Failed to check if Docker image exists: {}", imageName, e);
            return false;
        }
    }

    public static void buildDockerImage(String imageName, String version, String baseDirectory) {
        try {
            String projectRoot = System.getProperty("user.dir");
            Path dockerContextPath = Paths.get(projectRoot, baseDirectory, version);
            
            log.info("Building Docker image {} from context: {}", imageName, dockerContextPath);
            
            ProcessBuilder pb = new ProcessBuilder("docker", "build", "-t", imageName, ".");
            pb.directory(dockerContextPath.toFile());
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("Docker build: {}", line);
                }
            }
            
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("Docker build failed with exit code: " + exitCode);
            }
            
            log.info("Successfully built Docker image: {}", imageName);
            
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to build Docker image: " + imageName, e);
        }
    }
}