package com.devos.core.service.impl;

import com.devos.core.service.CommandSandboxService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class CommandSandboxServiceImpl implements CommandSandboxService {

    @Value("${devos.execution.timeout:60000}")
    private long executionTimeout;

    private static final List<String> DANGEROUS_COMMANDS = Arrays.asList(
            "rm -rf /", "mkfs", "dd if=", "shutdown", "reboot", ":(){ :|:& };:", "chmod -R 777 /"
    );

    @Override
    public CommandResult executeCommand(String command, String workingDirectory, Map<String, String> environment) {
        if (!isCommandSafe(command)) {
            return CommandResult.builder()
                    .error("Command blocked for security reasons: " + command)
                    .exitCode(-1)
                    .success(false)
                    .build();
        }

        log.info("Executing sandboxed command: {} in {}", command, workingDirectory);
        long startTime = System.currentTimeMillis();
        
        try {
            ProcessBuilder pb = new ProcessBuilder();
            
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                pb.command("cmd.exe", "/c", command);
            } else {
                pb.command("sh", "-c", command);
            }

            pb.directory(new File(workingDirectory));
            if (environment != null) {
                pb.environment().putAll(environment);
            }
            pb.redirectErrorStream(true);

            Process process = pb.start();
            
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    if (output.length() > 100000) { // 100KB limit
                        output.append("\n... [Output truncated due to size] ...");
                        break;
                    }
                }
            }

            boolean finished = process.waitFor(executionTimeout, TimeUnit.MILLISECONDS);
            
            if (!finished) {
                process.destroyForcibly();
                return CommandResult.builder()
                        .output(output.toString())
                        .error("Command timed out after " + executionTimeout + "ms")
                        .exitCode(-2)
                        .success(false)
                        .executionTimeMs(System.currentTimeMillis() - startTime)
                        .build();
            }

            int exitCode = process.exitValue();
            return CommandResult.builder()
                    .output(output.toString())
                    .exitCode(exitCode)
                    .success(exitCode == 0)
                    .executionTimeMs(System.currentTimeMillis() - startTime)
                    .build();

        } catch (Exception e) {
            log.error("Command execution error", e);
            return CommandResult.builder()
                    .error("Command execution error: " + e.getMessage())
                    .exitCode(-3)
                    .success(false)
                    .executionTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    @Override
    public boolean isCommandSafe(String command) {
        if (command == null || command.trim().isEmpty()) return false;
        
        String lowerCmd = command.toLowerCase();
        for (String dangerous : DANGEROUS_COMMANDS) {
            if (lowerCmd.contains(dangerous.toLowerCase())) {
                return false;
            }
        }
        
        // Basic check for redirection to system files or sensitive areas
        if (lowerCmd.contains("> /etc/") || lowerCmd.contains("> /var/") || lowerCmd.contains("> /boot/")) {
            return false;
        }

        return true;
    }
}
